package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.imd.ImageException;
import au.wildie.m68k.cromixfs.fs.CromixTime;
import au.wildie.m68k.cromixfs.fs.DumpMode;
import au.wildie.m68k.cromixfs.fs.FileSystem;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static au.wildie.m68k.cromixfs.fs.DumpMode.EXTRACT;
import static au.wildie.m68k.cromixfs.fs.DumpMode.LIST;
import static au.wildie.m68k.cromixfs.fs.cromix.Inode.ACCESS_APPEND;
import static au.wildie.m68k.cromixfs.fs.cromix.Inode.ACCESS_READ;
import static au.wildie.m68k.cromixfs.fs.cromix.Inode.ACCESS_WRITE;
import static au.wildie.m68k.cromixfs.fs.cromix.Inode.INDIRECT_1_BLOCK;
import static au.wildie.m68k.cromixfs.fs.cromix.Inode.INDIRECT_2_BLOCK;
import static au.wildie.m68k.cromixfs.fs.cromix.Inode.INDIRECT_3_BLOCK;
import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.BLOCK_DEVICE;
import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.CHARACTER_DEVICE;
import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.DIRECTORY;
import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.FILE;
import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.SHARED_TEXT;
import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.UNKNOWN;
import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.UNUSED;
import static au.wildie.m68k.cromixfs.fs.cromix.PointerBlock.BLOCK_POINTER_COUNT;
import static au.wildie.m68k.cromixfs.fs.cromix.SuperBlock.FREE_INODE_LIST_SIZE;
import static au.wildie.m68k.cromixfs.utils.BinUtils.readDWord;
import static java.lang.Integer.max;

public class CromixFileSystem implements FileSystem {


    // Always use forward slash as per cromix
    private static final String FILE_SEP = "/";

    @Getter
    private final DiskInterface disk;
    private final SuperBlock superBlock;
    private final FreeBlockList freeBlockList;
    private final InodeManager inodeManager;

    public static boolean isValid(DiskInterface disk) {
        try {
            SuperBlock superBlock = SuperBlock.from(disk.getSuperBlock());
            return StringUtils.equals(superBlock.getCromix(), "cromix");
        } catch (Exception ignored) {
        }
        return false;
    }

    public static CromixFileSystem initialise(DiskInterface disk) throws IOException {
        SuperBlock superBlock = disk.getFormatLabel().startsWith("CL") ?
                SuperBlock.initialiseLarge(disk.getFormatLabel())
                :
                SuperBlock.initialiseSmall(disk.getFormatLabel());

        // Create the free block list
        FreeBlockList freeBlockList = FreeBlockList.create(superBlock, disk);
        freeBlockList.flush();

        // Create the inodes
        InodeManager inodeManager = InodeManager.create(superBlock, disk, freeBlockList);
        inodeManager.flushAll();

        Inode one = inodeManager.getInode(1);
        int blockNumber = freeBlockList.getAvailableBlock();
        inodeManager.addBlock(one, blockNumber);
        DirectoryBlock directoryBlock = DirectoryBlock.from(disk, blockNumber);
        directoryBlock.flush(disk);
        inodeManager.flushAll();
        disk.flushSuperBlock(superBlock.toBytes());

        return new CromixFileSystem(disk);
    }

    public CromixFileSystem(DiskInterface disk) throws IOException {
        this.disk = disk;
        superBlock = SuperBlock.from(disk.getSuperBlock());
        if (!StringUtils.equals(superBlock.getCromix(), "cromix")) {
            throw new CromixFileSystemException("Not a valid cromix filesystem");
        }
        freeBlockList = FreeBlockList.readFreeBlockList(superBlock, disk);
        inodeManager = InodeManager.read(superBlock, disk, freeBlockList);
    }

    public String getVersion() {
        return superBlock.getVersion();
    }

    @Override
    public String getName() {
        return "Cromix filesystem";
    }

    @Override
    public CromixFileSystemDirectoryNode tree() {
        return readDirectoryTree("", inodeManager.getInode( 1));
    }

    @Override
    public void list(PrintStream out) throws IOException {
        out.printf("File system version: %s\n", getVersion());
        readDirectory("", inodeManager.getInode( 1), LIST, null, out);
    }

    @Override
    public void extract(String path, PrintStream out) throws IOException {
        readDirectory("", inodeManager.getInode( 1), EXTRACT, path, out);
    }

    public void dumpInodes(PrintStream out) {
        inodeManager.getAllInodes().stream()
                .filter(inode -> inode.getType() != UNUSED && inode.getType() != UNKNOWN)
                .forEach(out::print);
    }

    public CromixFileSystemStats check(PrintStream out) {
        Check check = new Check(superBlock, inodeManager, disk);
        out.println("Executing dcheck");
        check.passOne();
        int dcheckErrors = check.passTwo(out);
        if (dcheckErrors == 0) {
            out.println("No errors\n");
        } else {
            out.println("Completed dcheck\n");
        }

        Integer iblockErrors = null;
        if (dcheckErrors == 0) {
            out.println("Executing inode block check");
            iblockErrors = check.fileCheck(out);
            if (iblockErrors == 0) {
                out.println("No inode block errors\n");
            } else {
                out.println("Completed inode block check\n");
            }
        }

        InodeStats inodeStats = new InodeStats(superBlock);
        inodeManager.getAllInodes().forEach(inodeStats::countUsage);

        for (int i = 0; i < FREE_INODE_LIST_SIZE; i++) {
            int inodeNumber = superBlock.getFreeInodeList()[i];
            if (inodeNumber != 0) {
                inodeStats.countFreeList(inodeManager.getInode(inodeNumber));
            }
        }

        BlockStats blockStats = checkBlocks();

        inodeStats.print(out);
        blockStats.print(out);

        return new CromixFileSystemStats(dcheckErrors, iblockErrors, blockStats, inodeStats);
    }

    protected BlockStats checkBlocks() {
        BlockUsage blockUsage = new BlockUsage(superBlock);
        AtomicInteger fileCount = new AtomicInteger();
        AtomicInteger directoryCount = new AtomicInteger();
        AtomicInteger deviceCount = new AtomicInteger();

        inodeManager.getAllInodes().forEach(inode -> {
            if (inode.getType() == CHARACTER_DEVICE || inode.getType() == BLOCK_DEVICE) {
                deviceCount.getAndIncrement();
            }

            if (inode.getType() == DIRECTORY) {
                directoryCount.getAndIncrement();
                for (int i = 0; i < 0x10; i++) {
                    int blockNumber = inode.getBlockNumber(i);
                    if (blockNumber != 0) {
                        blockUsage.setDirectory(blockNumber);
                    }
                }
            }

            if (inode.getType() == FILE || inode.getType() == SHARED_TEXT) {
                fileCount.getAndIncrement();
                for (int i = 0; i < INDIRECT_1_BLOCK; i++) {
                    if (inode.getBlockNumber(i) != 0) {
                        blockUsage.setFile(inode.getBlockNumber(i));
                    }
                }
                if (inode.getBlockNumber(INDIRECT_1_BLOCK) != 0) {
                    // Is a pointer block
                    blockUsage.setFile(inode.getBlockNumber(INDIRECT_1_BLOCK));
                    PointerBlock pointerBlock = getPointerBlock(inode.getBlockNumber(INDIRECT_1_BLOCK));
                    pointerBlock.getPointerList().stream()
                            .filter(blockNumber -> blockNumber != 0)
                            .forEach(blockUsage::setFile);
                }

                if (inode.getBlockNumber(INDIRECT_2_BLOCK) != 0) {
                    // Is a pointer block of pointer blocks
                    blockUsage.setFile(inode.getBlockNumber(INDIRECT_2_BLOCK));
                    PointerBlock pointerBlock = getPointerBlock(inode.getBlockNumber(INDIRECT_2_BLOCK));
                    pointerBlock.getPointerList().stream()
                            .filter(blockNumber -> blockNumber != 0)
                            .peek(blockUsage::setFile)
                            .flatMap(blockNumber -> getPointerBlock(blockNumber).getPointerList().stream())
                            .filter(blockNumber -> blockNumber != 0)
                            .forEach(blockUsage::setFile);
                }

                if (inode.getBlockNumber(INDIRECT_3_BLOCK) != 0) {
                    // Is a pointer block of pointer blocks of pointer blocks
                    blockUsage.setFile(inode.getBlockNumber(INDIRECT_3_BLOCK));
                    PointerBlock pointerBlock = getPointerBlock(inode.getBlockNumber(INDIRECT_3_BLOCK));
                    pointerBlock.getPointerList().stream()
                            .filter(blockNumber -> blockNumber != 0)
                            .peek(blockUsage::setFile)
                            .flatMap(blockNumber -> getPointerBlock(blockNumber).getPointerList().stream())
                            .filter(blockNumber -> blockNumber != 0)
                            .peek(blockUsage::setFile)
                            .flatMap(blockNumber -> getPointerBlock(blockNumber).getPointerList().stream())
                            .filter(blockNumber -> blockNumber != 0)
                            .forEach(blockUsage::setFile);
                }
            }
        });

        freeBlockList.visit(blockUsage::setOnFreeList);

        blockUsage.getOrphanedBlocks().forEach(item -> System.out.printf("Orphaned block number 0x%06x\n", item.getNumber()));

        BlockStats stats = new BlockStats(superBlock);
        stats.setFileBlocks(blockUsage.getFileBlockCount());
        stats.setDirectoryBlocks(blockUsage.getDirectoryBlockCount());
        stats.setOnFreeList(blockUsage.getOnFreeListBlockCount());
        stats.setOrphanedBlocks(blockUsage.getOrphanedBlockCount());
        stats.setDuplicateBlocks(blockUsage.getDuplicateBlockCount());
        stats.setFreeListBlocks(freeBlockList.getFreeBlockCount());
        stats.setFiles(fileCount.get());
        stats.setDirectories(directoryCount.get());
        stats.setDevices(deviceCount.get());

        return stats;
    }

    public void append(File path, PrintStream out){
        Inode rootInode = inodeManager.getInode(1);
        if (path.isDirectory()) {
            addDirectory(path, path, rootInode, out);
        } else if (path.isFile()) {
            addFile(path.getParentFile() != null ? path.getParentFile() : path.getAbsoluteFile().getParentFile(), rootInode, path, out);
        }
    }

    public void addDirectory(File directory, PrintStream out) {
        if (directory.isDirectory()) {
            addDirectory(directory, directory, inodeManager.getInode(1), out);
        }
    }

    protected void addDirectory(File rootDirectory, File directory, Inode parentInode, PrintStream out) {
        if (directory.isDirectory()) {
            // Process directories first
            Arrays.stream(Objects.requireNonNull(directory.listFiles(File::isDirectory)))
                    .sorted(Comparator.comparing(File::getName))
                    .forEach(dir -> {
                        if (dir.getName().length() > DirectoryEntry.NAME_LENGTH) {
                            out.printf("Skipping directory : %s, name is too long\n", rootDirectory.toPath().relativize(dir.toPath()));
                        } else {
                            DirectoryEntry childEntry = findOrCreateDirectoryEntry(rootDirectory, parentInode, dir, out);
                            addDirectory(rootDirectory, dir, inodeManager.getInode(childEntry.getInodeNumber()), out);
                        }
                    });

            // Process files next
            Arrays.stream(Objects.requireNonNull(directory.listFiles(File::isFile)))
                    .sorted(Comparator.comparing(File::getName))
                    .forEach(file -> {
                        if (file.getName().length() > DirectoryEntry.NAME_LENGTH) {
                            out.printf("Skipping file      : %s, name is too long\n", rootDirectory.toPath().relativize(file.toPath()));
                        } else {
                            addFile(rootDirectory, parentInode, file, out);
                        }
                    });
        }
    }

    protected DirectoryEntry addFile(File rootDirectory, Inode parentInode, File file, PrintStream out) {
        DirectoryEntry entry = findDirectoryEntry(parentInode, file.getName());
        Inode entryInode;
        if (entry != null) {
            out.printf("Replacing file     : %s\n", rootDirectory.toPath().relativize(file.toPath()));
            // Existing file
            entryInode = inodeManager.getInode(entry.getInodeNumber());
            entryInode.deleteFileBlocks(disk, freeBlockList);
        } else {
            out.printf("Adding file        : %s\n", rootDirectory.toPath().relativize(file.toPath()));

            entryInode = inodeManager.getAvailableInode();
            entryInode.setType(FILE);
            entryInode.setLinks(1);
            entryInode.setOwner(32767);
            entryInode.setGroup(32767);
            entryInode.setOwnerPermission(ACCESS_READ | ACCESS_WRITE | ACCESS_APPEND);
            entryInode.setGroupPermission(ACCESS_READ);
            entryInode.setOtherPermission(ACCESS_READ);
            entryInode.setCreated(CromixTime.now());
            entryInode.setModified(CromixTime.now());
            entryInode.setAccessed(CromixTime.now());

            // Find the first unused directory entry
            entry = getNextAvailableDirectoryEntry(parentInode);
            entry.setStatus(DirectoryEntryStatus.ALLOCATED);
            entry.setName(file.getName().toLowerCase());
            entry.setInodeNumber(entryInode.getNumber());
            entry.flush(disk);

            parentInode.incrementDirectoryEntryCount();
            parentInode.setFileSize(getDirectoryExtent(parentInode));
            inodeManager.flush(parentInode);
        }

        try (FileInputStream in = new FileInputStream(file)) {
            int freeBlocks = freeBlockList.getFreeBlockCount();
            int fileSize = 0;
            byte[] data = new byte[superBlock.getBlockSize()];
            while(true) {
                int bytes = in.read(data);
                if (bytes > 0) {
                    int blockNumber = freeBlockList.getAvailableBlock();
                    byte[] block = disk.getBlock(blockNumber);
                    System.arraycopy(data, 0, block, 0, bytes);
                    fileSize += bytes;
                    inodeManager.addBlock(entryInode, blockNumber);
                }
                if (bytes < superBlock.getBlockSize()) {
                    break;
                }
            }
            entryInode.setUsedBlockCount(freeBlocks - freeBlockList.getFreeBlockCount());
            entryInode.setFileSize(fileSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        inodeManager.flush(entryInode);

        entry.flush(disk);

        superBlock.flush(disk);

        return entry;
    }

    protected DirectoryEntry findOrCreateDirectoryEntry(File rootDirectory, Inode parentInode, File dir, PrintStream out) {
        // Look for a directory entry with the same name
        DirectoryEntry entry = findDirectoryEntry(parentInode, dir.getName());
        if (entry != null) {
            return entry;
        }

        out.printf("Adding directory   : %s\n", rootDirectory.toPath().relativize(dir.toPath()));

        // Not found so find the first unused directory entry
        entry = getNextAvailableDirectoryEntry(parentInode);

        Inode entryInode = inodeManager.getAvailableInode();
        entryInode.setType(DIRECTORY);
        entryInode.setLinks(1);
        entryInode.setFileSize(0);
        entryInode.setParent(parentInode.getNumber());
        entryInode.setOwnerPermission(0x0f);
        entryInode.setGroupPermission(0x03);
        entryInode.setOtherPermission(0x03);
        entryInode.setCreated(CromixTime.now());
        entryInode.setModified(CromixTime.now());
        entryInode.setAccessed(CromixTime.now());
        inodeManager.flush(entryInode);

        entry.setStatus(DirectoryEntryStatus.ALLOCATED);
        entry.setName(dir.getName().toLowerCase());
        entry.setInodeNumber(entryInode.getNumber());
        entry.flush(disk);

        parentInode.incrementDirectoryEntryCount();
        parentInode.setFileSize(getDirectoryExtent(parentInode));
        inodeManager.flush(parentInode);

        superBlock.flush(disk);
        return entry;
    }

    protected DirectoryEntry findDirectoryEntry(Inode parentInode, String name) {
        // Look for a directory entry with the same name
        for (int i = 0; i < INDIRECT_1_BLOCK; i++) {
            int blockNumber = parentInode.getBlockNumber(i);
            if (blockNumber != 0) {
                DirectoryBlock directoryBlock = DirectoryBlock.from(disk, blockNumber);
                DirectoryEntry entry = directoryBlock.findEntry(name.toLowerCase());
                if (entry != null) {
                    return entry;
                }
            }
        }
        return null;
    }

    private DirectoryEntry getNextAvailableDirectoryEntry(Inode inode) {
        // Find the first unused directory entry
        for (int i = 0; i < INDIRECT_1_BLOCK; i++) {
            int blockNumber = inode.getBlockNumber(i);
            if (blockNumber != 0) {
                DirectoryBlock directoryBlock = DirectoryBlock.from(disk, blockNumber);
                DirectoryEntry unused = directoryBlock.getFirstUnusedEntry();
                if (unused != null) {
                    return unused;
                }
            }
        }

        // Need to add a new directory block
        int blockNumber = freeBlockList.getAvailableBlock();
        DirectoryBlock directoryBlock = new DirectoryBlock(blockNumber, true);
        directoryBlock.flush(disk);
        inodeManager.addBlock(inode, blockNumber);
        inodeManager.flush(inode);
        return directoryBlock.getFirstUnusedEntry();
    }

    private Integer getDirectoryExtent(Inode inode) {
        int extent = 0;
        for (int i = 0; i < INDIRECT_1_BLOCK; i++) {
            if (inode.getBlockNumber(i) != 0) {
                DirectoryBlock directoryBlock = DirectoryBlock.from(disk, inode.getBlockNumber(i));
                if (directoryBlock.getUnusedEntries() > 0) {
                    extent = max(extent, i * superBlock.getBlockSize() + directoryBlock.getExtent());
                }
            }
        }
        return extent;
    }

    private CromixFileSystemDirectoryNode readDirectoryTree(String name, Inode inode) {
        CromixFileSystemDirectoryNode treeNode = new CromixFileSystemDirectoryNode(name, inode);

        for (int i = 0; i < INDIRECT_1_BLOCK; i++) {
            int blockNumber = inode.getBlockNumber(i);
            if (blockNumber != 0) {
                DirectoryBlock directoryBlock = DirectoryBlock.from(disk, blockNumber);
                for (DirectoryEntry entry : directoryBlock.getEntries()) {
                    if (entry.getStatus() == DirectoryEntryStatus.ALLOCATED) {
                        Inode entryInode = inodeManager.getInode(entry.getInodeNumber());
                        if (entryInode.getType() == DIRECTORY) {
                            treeNode.add(readDirectoryTree(entry.getName(), entryInode));
                        } else {
                            treeNode.add(new CromixFileSystemFileNode(entry.getName(), entryInode));
                        }
                    }
                }
            }
        }
        return treeNode;
    }

    private void readDirectory(String srcPath, Inode inode, DumpMode mode, String trgPath, PrintStream listingOut) throws IOException {
        for (int i = 0; i < INDIRECT_1_BLOCK; i++) {
            int blockNumber = inode.getBlockNumber(i);
            if (blockNumber != 0) {
                DirectoryBlock directoryBlock = DirectoryBlock.from(disk, blockNumber);
                for (DirectoryEntry entry : directoryBlock.getEntries()) {
                    if (entry.getStatus() == DirectoryEntryStatus.ALLOCATED) {
                        Inode entryInode = inodeManager.getInode(entry.getInodeNumber());

                        if (entryInode.getType() == CHARACTER_DEVICE || entryInode.getType() == BLOCK_DEVICE) {
                            listingOut.printf("  %3d,%-3d", entryInode.getMajor(), entryInode.getMinor());
                        } else {
                            listingOut.printf("%9d", entryInode.getType() == DIRECTORY ? entryInode.getDirectoryEntryCount() : entryInode.getFileSize());
                        }
                        listingOut.printf(" %s %2d %s %s %s %5d %5d %s %4d %s%s%s%n",
                                entryInode.getTypeChar(),
                                entryInode.getLinks(),
                                getPermission(entryInode.getOwnerPermission()),
                                getPermission(entryInode.getGroupPermission()),
                                getPermission(entryInode.getOtherPermission()),
                                entryInode.getOwner(),
                                entryInode.getGroup(),
                                entryInode.getModified().toString(),
                                entryInode.getNumber(),
                                srcPath,
                                FILE_SEP,
                                entry.getName());

                        if (entryInode.getType() == DIRECTORY) {
                            File dir = null;
                            if (mode == EXTRACT) {
                                dir = new File(trgPath + FILE_SEP + entry.getName());
                                dir.mkdirs();
                            }
                            readDirectory(srcPath + FILE_SEP + entry.getName(), entryInode, mode, mode == EXTRACT ? (trgPath + FILE_SEP + entry.getName()) : (srcPath + FILE_SEP + entry.getName()), listingOut);
                            if (mode == EXTRACT) {
                                try {
                                    dir.setLastModified(entryInode.getModified().toDate().getTime());
                                } catch (ParseException e) {
                                    //
                                }
                            }
                        }
                        if (entryInode.getType() == FILE && mode == EXTRACT) {
                            extractFile(entryInode, entryInode.getFileSize(), entryInode.getModified(), trgPath + FILE_SEP + entry.getName());
                        }
                    }
                }
            }
        }
        listingOut.print("");
    }

    @Override
    public byte[] readFile(String name, Inode inode) throws IOException {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        int remainingBytes = inode.getFileSize();

        try {
            // Read first 16 data blocks
            for (int i = 0; i < 0x10 && remainingBytes > 0; i++) {
                int blockNumber = inode.getBlockNumber(i);
                if (blockNumber != 0) {
                    byte[] data = getBlock(blockNumber);
                    int bytes = Math.min(remainingBytes, data.length);
                    content.write(data, 0, bytes);
                    remainingBytes -= bytes;
                }
            }

            if (remainingBytes == 0) {
                return content.toByteArray();
            }

            // 17th pointer
            int blockNumber = inode.getBlockNumber(INDIRECT_1_BLOCK);
            if (blockNumber != 0) {
                remainingBytes = extractPointerBlock(blockNumber, content, remainingBytes);
            }

            if (remainingBytes == 0) {
                return content.toByteArray();
            }

            // 18th pointer
            blockNumber = inode.getBlockNumber(INDIRECT_2_BLOCK);
            if (blockNumber != 0) {
                remainingBytes = extractPointerPointerBlock(blockNumber, content, remainingBytes);
            }

            if (remainingBytes == 0) {
                return content.toByteArray();
            }

            // 19th pointer
            blockNumber = inode.getBlockNumber(INDIRECT_3_BLOCK);
            if (blockNumber != 0) {
                remainingBytes = extractPointerPointerPointerBlock(blockNumber, content, remainingBytes);
            }

            if (remainingBytes != 0) {
                String error = String.format("Did not read all bytes, %d remaining", name, remainingBytes);
                System.out.println(error);
                //throw new RuntimeException(error);
            }
        } catch (ImageException e) {
            System.out.printf("Error extracting file %s, only %d bytes of %d bytes extracted. %s\n", name, inode.getFileSize() - remainingBytes, inode.getFileSize(), e.getMessage());
        }
        return content.toByteArray();
    }

    private void extractFile(Inode inode, int size, CromixTime modified, String path) throws IOException {
        int remainingBytes = size;
        File file = new File(path);

        try (OutputStream fileOutputStream = new FileOutputStream(file)) {
            // Read first 16 data blocks
            for (int i = 0; i < 0x10 && remainingBytes > 0; i++) {
                int blockNumber = inode.getBlockNumber(i);
                if (blockNumber != 0) {
                    byte[] data = getBlock(blockNumber);
                    int bytes = Math.min(remainingBytes, data.length);
                    fileOutputStream.write(data, 0, bytes);
                    remainingBytes -= bytes;
                }
            }

            if (remainingBytes == 0) {
                fileOutputStream.flush();
                return;
            }

            // 17th pointer
            int blockNumber = inode.getBlockNumber(INDIRECT_1_BLOCK);
            if (blockNumber != 0) {
                remainingBytes = extractPointerBlock(blockNumber, fileOutputStream, remainingBytes);
            }

            if (remainingBytes == 0) {
                fileOutputStream.flush();
                return;
            }

            // 18th pointer
            blockNumber = inode.getBlockNumber(INDIRECT_2_BLOCK);
            if (blockNumber != 0) {
                remainingBytes = extractPointerPointerBlock(blockNumber, fileOutputStream, remainingBytes);
            }

            if (remainingBytes == 0) {
                fileOutputStream.flush();
                return;
            }

            // 19th pointer
            blockNumber = inode.getBlockNumber(INDIRECT_3_BLOCK);
            if (blockNumber != 0) {
                remainingBytes = extractPointerPointerPointerBlock(blockNumber, fileOutputStream, remainingBytes);
            }

            if (remainingBytes != 0) {
                String error = String.format("Did not read all bytes for %s, %d remaining", path, remainingBytes);
                System.out.println(error);
                //throw new RuntimeException(error);
            }
            fileOutputStream.flush();
        } catch (ImageException e) {
            System.out.printf("Error extracting file %s, only %d bytes of %d bytes extracted. %s\n", path, size - remainingBytes, size, e.getMessage());
        } finally {
            try {
                if (modified.toDate().before(new Date())) {
                    try {
                        file.setLastModified(modified.toDate().getTime());
                    } catch (IllegalArgumentException e) {
                        //
                    }
                }
            } catch (ParseException e) {
                //
            }
        }
    }

    protected PointerBlock getPointerBlock(int blockNumber) {
        return PointerBlock.from(blockNumber, disk);
    }

    protected byte[] getBlock(int blockNumber) {
        try {
            return disk.getBlock(blockNumber);
        } catch (IOException e) {
            throw new BlockUnavailableException(blockNumber, e);
        }
    }

    private int extractPointerPointerPointerBlock(int ptrPtrPtrBlockNumber, OutputStream out, int remainingBytes) throws IOException {
        byte[] block = getBlock(ptrPtrPtrBlockNumber);
        for (int i = 0; i < BLOCK_POINTER_COUNT && remainingBytes > 0; i++) {
            int ptrPtrBlockNumber = readDWord(block, i * 4);
            if (ptrPtrBlockNumber != 0) {
                remainingBytes = extractPointerPointerBlock(ptrPtrBlockNumber, out, remainingBytes);
            }
        }
        return remainingBytes;
    }

    private int extractPointerPointerBlock(int ptrPtrBlockNumber, OutputStream out, int remainingBytes) throws IOException {
        byte[] block = getBlock(ptrPtrBlockNumber);
        for (int i = 0; i < BLOCK_POINTER_COUNT && remainingBytes > 0; i++) {
            int ptrBlockNumber = readDWord(block, i * 4);
            if (ptrBlockNumber != 0) {
                remainingBytes = extractPointerBlock(ptrBlockNumber, out, remainingBytes);
            }
        }
        return remainingBytes;
    }

    private int extractPointerBlock(int ptrBlockNumber, OutputStream out, int remainingBytes) throws IOException {
        byte[] block = getBlock(ptrBlockNumber);
        for (int i = 0; i < BLOCK_POINTER_COUNT && remainingBytes > 0; i++) {
            int blockNumber = readDWord(block, i * 4);
            byte[] data = blockNumber == 0 ? new byte[512] : getBlock(blockNumber);
            int bytes = Math.min(remainingBytes, data.length);
            out.write(data, 0, bytes);
            remainingBytes -= bytes;
        }
        return remainingBytes;
    }

    private String getPermission(int value) {
        return ((value & 0x1) != 0 ? "r" : "-")
             + ((value & 0x2) != 0 ? "e" : "-")
             + ((value & 0x4) != 0 ? "w" : "-")
             + ((value & 0x8) != 0 ? "a" : "-");
    }

    public void persist(OutputStream archive) throws IOException {
        disk.persist(archive);
    }
}
