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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static au.wildie.m68k.cromixfs.fs.DumpMode.EXTRACT;
import static au.wildie.m68k.cromixfs.fs.DumpMode.LIST;
import static au.wildie.m68k.cromixfs.fs.cromix.Inode.*;
import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.*;
import static au.wildie.m68k.cromixfs.fs.cromix.PointerBlock.BLOCK_POINTER_COUNT;
import static au.wildie.m68k.cromixfs.fs.cromix.SuperBlock.FREE_INODE_LIST_SIZE;
import static au.wildie.m68k.cromixfs.utils.BinUtils.readDWord;
import static au.wildie.m68k.cromixfs.utils.BinUtils.readWord;

public class CromixFileSystem implements FileSystem {
    private static final int DIR_ENTRY_LENGTH = 0x20;

    // Always use forward slash as per cromix
    private static final String FILE_SEP = "/";

    @Getter
    private final DiskInterface disk;
    private final SuperBlock superBlock;
    private final FreeBlock freeBlockList;
    private final List<Inode> inodes;

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

        // Create the inodes
        for (int i = 0; i < superBlock.getInodeCount() / INODES_PER_BLOCK; i++) {
            byte[] block = disk.getBlock(superBlock.getFirstInode() + i);
            for (int j = 0; j < INODES_PER_BLOCK; j++) {
                new Inode(i * INODES_PER_BLOCK + j + 1).toBytes(block, (superBlock.getBlockSize() / INODES_PER_BLOCK) * j);
            }
        }

        // Create the free block list
        FreeBlock freeBlock = FreeBlock.create(superBlock);
        freeBlock.flush(disk);

        disk.setSuperBlock(superBlock.toBytes());

        return new CromixFileSystem(disk);
    }

    public CromixFileSystem(DiskInterface disk) throws IOException {
        this.disk = disk;
        superBlock = SuperBlock.from(disk.getSuperBlock());
        if (!StringUtils.equals(superBlock.getCromix(), "cromix")) {
            throw new CromixFileSystemException("Not a valid cromix filesystem");
        }
        freeBlockList = readFreeBlockList();
        inodes = readAllINodes();
    }

    protected FreeBlock readFreeBlockList() {
        FreeBlock base = FreeBlock.from(superBlock);
        base.setNext(readNextFreeBlock(base));
        return base;
    }

    protected FreeBlock readNextFreeBlock(FreeBlock freeBlock) {
        if (freeBlock.getList()[0] == 0) {
            return null;
        }
        int blockNumber = freeBlock.getList()[0];
        FreeBlock next = FreeBlock.from(blockNumber, getBlock(blockNumber));
        next.setNext(readNextFreeBlock(next));
        return next;
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
        readDirectory("", getInode( 1).get(), LIST, null, out);
    }

    @Override
    public void extract(String path, PrintStream out) throws IOException {
        readDirectory("", getInode( 1).get(), EXTRACT, path, out);
    }

    protected CromixFileSystemStats check(PrintStream out) throws IOException {
        InodeStats inodeStats = new InodeStats(superBlock);

        inodes.forEach(inodeStats::countUsage);

        for (int i = 0; i < FREE_INODE_LIST_SIZE; i++) {
            getInode(superBlock.getFreeInodeList()[i]).ifPresent(inodeStats::countFreeList);
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

        inodes.forEach(inode -> {
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
                for (int i = 0; i < DIRECT_BLOCKS; i++) {
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
        stats.setOrphanedBlock(blockUsage.getOrphanedBlockCount());
        stats.setDuplicateBlocks(blockUsage.getDuplicateBlockCount());
        stats.setFreeListBlocks(freeBlockList.getTotalFreeBlockCount());
        stats.setFiles(fileCount.get());
        stats.setDirectories(directoryCount.get());
        stats.setDevices(deviceCount.get());

        return stats;
    }

    protected Optional<Inode> getInode(int iNodeNumber) {
        return inodes.stream().filter(iNode -> iNode.getNumber() == iNodeNumber).findFirst();
    }

    protected List<Inode> readAllINodes() {
        List<Inode> iNodes = new ArrayList<>();
        int blocks = superBlock.getInodeCount() / INODES_PER_BLOCK;
        for (int i = 0; i < blocks; i++) {
            byte[] block = getBlock(superBlock.getFirstInode() + i);
            for (int j = 0; j < INODES_PER_BLOCK; j++) {
                iNodes.add(Inode.from(Arrays.copyOfRange(block, j * INODE_LENGTH, j * INODE_LENGTH + INODE_LENGTH)));
            }
        }
        return iNodes;
    }

    private void readDirectory(String srcPath, Inode inode, DumpMode mode, String trgPath, PrintStream out) throws IOException {
        for (int i = 0; i < 0x10; i++) {
            int blockNumber = inode.getBlockNumber(i);
            if (blockNumber != 0) {
                byte[] data = getBlock(blockNumber);

                for (int j = 0; j < DIRECT_BLOCKS; j++) {
                    if (data[j * DIR_ENTRY_LENGTH + 0x1c] == (byte)0x80) {
                        String name = "";
                        for (int k = 0; k < 0x18 && data[j * DIR_ENTRY_LENGTH + k] != 0; k++) {
                            name = name + (char) data[j * DIR_ENTRY_LENGTH + k];
                        }

                        if (name.equals("gtty.bin")) {
                            System.out.println("gtty");
                        }

                        int entryInodeNumber = readWord(data, j * DIR_ENTRY_LENGTH + 0x1E);
                        Inode entryInode = getInode(entryInodeNumber).get();

                        if (entryInode.getType() == CHARACTER_DEVICE || entryInode.getType() == BLOCK_DEVICE) {
                            out.printf("  %3d,%-3d", entryInode.getMajor(), entryInode.getMinor());
                        } else {
                            out.printf("%9d", entryInode.getType() == DIRECTORY ? entryInode.getDirCount() : entryInode.getFileSize());
                        }
                        out.printf(" %s %2d %s %s %s %5d %5d %s %s%s%s%n",
                                entryInode.getTypeChar(),
                                entryInode.getLinks(),
                                getPermission(entryInode.getUserPermission()),
                                getPermission(entryInode.getGroupPermission()),
                                getPermission(entryInode.getOtherPermission()),
                                entryInode.getOwner(),
                                entryInode.getGroup(),
                                entryInode.getModified().toString(),
                                srcPath,
                                FILE_SEP,
                                name);

                        if (entryInode.getType() == DIRECTORY) {
                            File dir = null;
                            if (mode == EXTRACT) {
                                dir = new File(trgPath + FILE_SEP + name);
                                dir.mkdirs();
                            }
                            readDirectory(srcPath + FILE_SEP + name, entryInode, mode, mode == EXTRACT ? (trgPath + FILE_SEP + name) : (srcPath + FILE_SEP + name), out);
                            if (mode == EXTRACT) {
                                try {
                                    dir.setLastModified(entryInode.getModified().toDate().getTime());
                                } catch (ParseException e) {
                                    //
                                }
                            }
                        }
                        if (entryInode.getType() == FILE && mode == EXTRACT) {
                            extractFile(entryInode, entryInode.getFileSize(), entryInode.getModified(), trgPath + FILE_SEP + name);
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
                byte[] data = blockNumber == 0 ? new byte[512] : getBlock(blockNumber);
                int bytes = Math.min(remainingBytes, data.length);
                out.write(data, 0, bytes);
                remainingBytes -= bytes;
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
        return PointerBlock.from(getBlock(blockNumber));
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
