package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.imd.ImageException;
import au.wildie.m68k.cromixfs.fs.CromixTime;
import au.wildie.m68k.cromixfs.fs.DumpMode;
import au.wildie.m68k.cromixfs.fs.FileSystem;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static au.wildie.m68k.cromixfs.fs.DumpMode.EXTRACT;
import static au.wildie.m68k.cromixfs.fs.DumpMode.LIST;
import static au.wildie.m68k.cromixfs.fs.cromix.Inode.*;
import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.*;
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
        SuperBlock superBlock = SuperBlock.initialise(disk.getFormatLabel());

        // Create the free block list
        FreeBlockList freeBlockList = FreeBlockList.create(superBlock);
        freeBlockList.flush(disk);

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
    public void list(PrintStream out) throws IOException {
        out.printf("Version: %s\n", getVersion());
        readDirectory("", inodeManager.getInode( 1), LIST, null, out);
    }

    @Override
    public void extract(String path, PrintStream out) throws IOException {
        readDirectory("", inodeManager.getInode( 1), EXTRACT, path, out);
    }

    protected CromixFileSystemStats check(PrintStream out) {
        Check dcheck = new Check(inodeManager, disk);
        dcheck.passOne();
        dcheck.passTwo();

        InodeStats inodeStats = new InodeStats(superBlock);

        inodeManager.getAllInodes().forEach(inodeStats::countUsage);

        inodeManager.getAllInodes().stream()
                .filter(inode -> inode.getType() != UNUSED && inode.getType() != UNKNOWN)
                .forEach(out::print);

        for (int i = 0; i < FREE_INODE_LIST_SIZE; i++) {
            int inodeNumber = superBlock.getFreeInodeList()[i];
            if (inodeNumber != 0) {
                inodeStats.countFreeList(inodeManager.getInode(inodeNumber));
            }
        }

        BlockStats blockStats = checkBlocks();

        inodeStats.print(out);
        blockStats.print(out);

        return new CromixFileSystemStats(blockStats, inodeStats);
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

        blockUsage.getOrphanedBlocks().forEach(item -> System.out.printf("Orphaned block number %d\n", item.getNumber()));

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

    public void addDirectory(File directory) {
        if (directory.isDirectory()) {
            addDirectory(directory, directory, inodeManager.getInode(1));
        }
    }

    public void addDirectory(File rootDirectory, File directory, Inode parentInode) {
        if (directory.isDirectory()) {
            // Process directories first
            Arrays.stream(Objects.requireNonNull(directory.listFiles(File::isDirectory)))
                    .sorted(Comparator.comparing(File::getName))
                    .forEach(dir -> {
                        if (dir.getName().length() > DirectoryEntry.NAME_LENGTH) {
                            System.out.printf("Skipping directory : %s, name is too long\n", rootDirectory.toPath().relativize(dir.toPath()));
                        } else {
                            DirectoryEntry childEntry = findOrCreateDirectoryEntry(rootDirectory, parentInode, dir);
                            addDirectory(rootDirectory, dir, inodeManager.getInode(childEntry.getInodeNumber()));
                        }
                    });

            // Process files next
            Arrays.stream(Objects.requireNonNull(directory.listFiles(File::isFile)))
                    .sorted(Comparator.comparing(File::getName))
                    .forEach(file -> {
                        if (file.getName().length() > DirectoryEntry.NAME_LENGTH) {
                            System.out.printf("Skipping file      : %s, name is too long\n", rootDirectory.toPath().relativize(file.toPath()));
                        } else {
                            addFile(rootDirectory, parentInode, file);
                        }
                    });
        }
    }

    protected DirectoryEntry addFile(File rootDirectory, Inode parentInode, File file) {
        System.out.printf("Adding file        : %s\n", rootDirectory.toPath().relativize(file.toPath()));
        // Find the first unused directory entry
        DirectoryEntry entry = getNextAvailableDirectoryEntry(parentInode);

        Inode entryInode = inodeManager.getAvailableInode();
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

        entry.setStatus(DirectoryEntryStatus.ALLOCATED);
        entry.setName(file.getName().toLowerCase());
        entry.setInodeNumber(entryInode.getNumber());
        entry.flush(disk);

        parentInode.incrementDirectoryEntryCount();
        parentInode.setFileSize(getDirectoryExtent(parentInode));
        inodeManager.flush(parentInode);

        superBlock.flush(disk);

        return entry;
    }

    protected DirectoryEntry findOrCreateDirectoryEntry(File rootDirectory, Inode parentInode, File dir) {
        // Look for a directory entry with the same name
        for (int i = 0; i < INDIRECT_1_BLOCK; i++) {
            int blockNumber = parentInode.getBlockNumber(i);
            if (blockNumber != 0) {
                DirectoryBlock directoryBlock = DirectoryBlock.from(disk, blockNumber);
                DirectoryEntry entry = directoryBlock.findEntry(dir.getName());
                if (entry != null) {
                    return entry;
                }
            }
        }

        System.out.printf("Adding directory   : %s\n", rootDirectory.toPath().relativize(dir.toPath()));

        // Not found so find the first unused directory entry
        DirectoryEntry entry = getNextAvailableDirectoryEntry(parentInode);

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

    private void readDirectory(String srcPath, Inode inode, DumpMode mode, String trgPath, PrintStream out) throws IOException {
        for (int i = 0; i < INDIRECT_1_BLOCK; i++) {
            int blockNumber = inode.getBlockNumber(i);
            if (blockNumber != 0) {
                DirectoryBlock directoryBlock = DirectoryBlock.from(disk, blockNumber);
                for (DirectoryEntry entry : directoryBlock.getEntries()) {
                    if (entry.getStatus() == DirectoryEntryStatus.ALLOCATED) {
                        Inode entryInode = inodeManager.getInode(entry.getInodeNumber());

                        if (entryInode.getType() == CHARACTER_DEVICE || entryInode.getType() == BLOCK_DEVICE) {
                            out.printf("  %3d,%-3d", entryInode.getMajor(), entryInode.getMinor());
                        } else {
                            out.printf("%9d", entryInode.getType() == DIRECTORY ? entryInode.getDirectoryEntryCount() : entryInode.getFileSize());
                        }
                        out.printf(" %s %2d %s %s %s %5d %5d %s %s%s%s%n",
                                entryInode.getTypeChar(),
                                entryInode.getLinks(),
                                getPermission(entryInode.getOwnerPermission()),
                                getPermission(entryInode.getGroupPermission()),
                                getPermission(entryInode.getOtherPermission()),
                                entryInode.getOwner(),
                                entryInode.getGroup(),
                                entryInode.getModified().toString(),
                                srcPath,
                                FILE_SEP,
                                entry.getName());

                        if (entryInode.getType() == DIRECTORY) {
                            File dir = null;
                            if (mode == EXTRACT) {
                                dir = new File(trgPath + FILE_SEP + entry.getName());
                                dir.mkdirs();
                            }
                            readDirectory(srcPath + FILE_SEP + entry.getName(), entryInode, mode, mode == EXTRACT ? (trgPath + FILE_SEP + entry.getName()) : (srcPath + FILE_SEP + entry.getName()), out);
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
        out.print("");
    }

    private void extractFile(Inode inode, int size, CromixTime modified, String path) throws IOException {
        int remainingBytes = size;
        File file = new File(path);

        try (OutputStream out = new FileOutputStream(file)) {
            // Read first 16 data blocks
            for (int i = 0; i < 0x10 && remainingBytes > 0; i++) {
                int blockNumber = inode.getBlockNumber(i);
                if (blockNumber != 0) {
                    byte[] data = getBlock(blockNumber);
                    int bytes = Math.min(remainingBytes, data.length);
                    out.write(data, 0, bytes);
                    remainingBytes -= bytes;
                }
            }

            if (remainingBytes == 0) {
                out.flush();
                return;
            }

            // 17th pointer
            int blockNumber = inode.getBlockNumber(INDIRECT_1_BLOCK);
            if (blockNumber != 0) {
                remainingBytes = extractPointerBlock(blockNumber, out, remainingBytes);
            }

            if (remainingBytes == 0) {
                out.flush();
                return;
            }

            // 18th pointer
            blockNumber = inode.getBlockNumber(INDIRECT_2_BLOCK);
            if (blockNumber != 0) {
                remainingBytes = extractPointerPointerBlock(blockNumber, out, remainingBytes);
            }

            if (remainingBytes == 0) {
                out.flush();
                return;
            }

            // 19th pointer
            blockNumber = inode.getBlockNumber(INDIRECT_3_BLOCK);
            if (blockNumber != 0) {
                remainingBytes = extractPointerPointerPointerBlock(blockNumber, out, remainingBytes);
            }

            if (remainingBytes != 0) {
                String error = String.format("Did not read all bytes for %s, %d remaining", path, remainingBytes);
                System.out.println(error);
                //throw new RuntimeException(error);
            }

            out.flush();
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
