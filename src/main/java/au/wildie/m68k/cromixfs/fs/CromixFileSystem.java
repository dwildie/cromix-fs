package au.wildie.m68k.cromixfs.fs;

import static au.wildie.m68k.cromixfs.fs.DumpMode.EXTRACT;
import static au.wildie.m68k.cromixfs.fs.DumpMode.LIST;
import static au.wildie.m68k.cromixfs.fs.Inode.INODES_PER_BLOCK;
import static au.wildie.m68k.cromixfs.fs.Inode.INODE_LENGTH;
import static au.wildie.m68k.cromixfs.fs.InodeType.BLOCK_DEVICE;
import static au.wildie.m68k.cromixfs.fs.InodeType.CHARACTER_DEVICE;
import static au.wildie.m68k.cromixfs.fs.InodeType.DIRECTORY;
import static au.wildie.m68k.cromixfs.fs.InodeType.FILE;
import static au.wildie.m68k.cromixfs.fs.PointerBlock.BLOCK_POINTER_COUNT;
import static au.wildie.m68k.cromixfs.fs.SuperBlock.FREE_INODE_LIST_SIZE;
import static au.wildie.m68k.cromixfs.utils.BinUtils.readDWord;
import static au.wildie.m68k.cromixfs.utils.BinUtils.readWord;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.imd.ImageException;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

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
        if (freeBlock.getFreeBlockList()[0] == 0) {
            return null;
        }
        FreeBlock next = FreeBlock.from(getBlock(freeBlock.getFreeBlockList()[0]));
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

    protected CromixInodeStats check(PrintStream out) {
        CromixInodeStats inodeStats = new CromixInodeStats(superBlock);

        inodes.forEach(inodeStats::countUsage);

        for (int i = 0; i < FREE_INODE_LIST_SIZE; i++) {
            getInode(superBlock.getFreeInodeList()[i]).ifPresent(inodeStats::countFreeList);
        }
        inodeStats.print(out);

        CromixBlockStats blockStats = checkBlocks();
        blockStats.print(out);
        return inodeStats;
    }

    protected CromixBlockStats checkBlocks() {
        BlockUsage[] blockUsage = new BlockUsage[superBlock.getDataBlockCount()];
        for (int i = 0; i < blockUsage.length; i++) {
            blockUsage[i] = new BlockUsage();
        }

        inodes.forEach(inode -> {
            if (inode.getType() == DIRECTORY) {
                for (int i = 0; i < 0x10; i++) {
                    int blockNumber = inode.getBlock(i);
                    if (blockNumber != 0) {
                        blockUsage[blockNumber - superBlock.getFirstDataBlock()].setDirectory();
                    }
                }
            }

            if (inode.getType() == FILE) {
                for (int i = 0; i < 0x10; i++) {
                    if (inode.getBlock(i) != 0) {
                        blockUsage[inode.getBlock(i) - superBlock.getFirstDataBlock()].setFile();
                    }
                }
                if (inode.getBlock(0x11) != 0) {
                    // Is a pointer block
                    PointerBlock pointerBlock = getPointerBlock(inode.getBlock(0x11));
                    pointerBlock.getPointerList().stream()
                            .filter(blockNumber -> blockNumber != 0)
                            .forEach(blockNumber -> blockUsage[blockNumber - superBlock.getFirstDataBlock()].setFile());
                }

                if (inode.getBlock(0x12) != 0) {
                    // Is a pointer block of pointer blocks
                    PointerBlock pointerBlock = getPointerBlock(inode.getBlock(0x11));
                    pointerBlock.getPointerList().stream()
                            .filter(blockNumber -> blockNumber != 0)
                            .flatMap(blockNumber -> getPointerBlock(inode.getBlock(blockNumber)).getPointerList().stream())
                            .filter(blockNumber -> blockNumber != 0)
                            .forEach(blockNumber -> blockUsage[blockNumber - superBlock.getFirstDataBlock()].setFile());
                }


                if (inode.getBlock(0x13) != 0) {
                    // Is a pointer block of pointer blocks of pointer blocks
                    PointerBlock pointerBlock = getPointerBlock(inode.getBlock(0x11));
                    pointerBlock.getPointerList().stream()
                            .filter(blockNumber -> blockNumber != 0)
                            .flatMap(blockNumber -> getPointerBlock(inode.getBlock(blockNumber)).getPointerList().stream())
                            .filter(blockNumber -> blockNumber != 0)
                            .flatMap(blockNumber -> getPointerBlock(inode.getBlock(blockNumber)).getPointerList().stream())
                            .filter(blockNumber -> blockNumber != 0)
                            .forEach(blockNumber -> blockUsage[blockNumber - superBlock.getFirstDataBlock()].setFile());
                }
            }
        });

        freeBlockList.visit(blockNumber -> blockUsage[blockNumber - superBlock.getFirstDataBlock()].setOnFreeList());

        CromixBlockStats stats = new CromixBlockStats(superBlock);
        stats.setFileBlocks((int)Arrays.stream(blockUsage)
                .filter(BlockUsage::isFile)
                .count());

        stats.setDirectoryBlocks((int)Arrays.stream(blockUsage)
                .filter(BlockUsage::isDirectory)
                .count());

        stats.setOnFreeList((int)Arrays.stream(blockUsage)
                .filter(BlockUsage::isOnFreeList)
                .count());

        stats.setUnusedBlocks((int)Arrays.stream(blockUsage)
                .filter(BlockUsage::isUnused)
                .count());

        stats.setDuplicateBlocks((int)Arrays.stream(blockUsage)
                .filter(BlockUsage::isDuplicate)
                .count());

        stats.setFreeListBlocks(freeBlockList.getTotalFreeBlockCount());
        return stats;
    }

    protected Optional<Inode> getInode(int iNodeNumber) {
        return inodes.stream().filter(iNode -> iNode.getNumber() == iNodeNumber).findFirst();
    }

    protected List<Inode> readAllINodes() throws IOException {
        List<Inode> iNodes = new ArrayList<>();
        int blocks = superBlock.getInodeCount() / INODES_PER_BLOCK;
        for (int i = 0; i < blocks; i++) {
            byte[] block = disk.getBlock(superBlock.getFirstInode() + i);
            for (int j = 0; j < INODES_PER_BLOCK; j++) {
                iNodes.add(Inode.from(Arrays.copyOfRange(block, j * INODE_LENGTH, j * INODE_LENGTH + INODE_LENGTH)));
            }
        }
        return iNodes;
    }

    private void readDirectory(String srcPath, Inode inode, DumpMode mode, String trgPath, PrintStream out) throws IOException {
        for (int i = 0; i < 0x10; i++) {
            int blockNumber = inode.getBlock(i);
            if (blockNumber != 0) {
                byte[] data = disk.getBlock(blockNumber);

                for (int j = 0; j < 0x10; j++) {
                    if (data[j * DIR_ENTRY_LENGTH + 0x1c] == (byte)0x80) {
                        String name = "";
                        for (int k = 0; k < 0x18 && data[j * DIR_ENTRY_LENGTH + k] != 0; k++) {
                            name = name + (char) data[j * DIR_ENTRY_LENGTH + k];
                        }

                        int entryInodeNumber = readWord(data, j * DIR_ENTRY_LENGTH + 0x1E);
                        Inode entryINode = getInode(entryInodeNumber).get();

                        if (entryINode.getType() == CHARACTER_DEVICE || entryINode.getType() == BLOCK_DEVICE) {
                            out.printf("  %3d,%-3d", entryINode.getMajor(), entryINode.getMinor());
                        } else {
                            out.printf("%9d", entryINode.getType() == DIRECTORY ? entryINode.getDirCount() : entryINode.getFileSize());
                        }
                        out.printf(" %s %2d %s %s %s %5d %5d %s %s%s%s%n",
                                entryINode.getTypeChar(),
                                entryINode.getLinks(),
                                getPermission(entryINode.getUserPermission()),
                                getPermission(entryINode.getGroupPermission()),
                                getPermission(entryINode.getOtherPermission()),
                                entryINode.getOwner(),
                                entryINode.getGroup(),
                                entryINode.getModified().toString(),
                                srcPath,
                                FILE_SEP,
                                name);

                        if (entryINode.getType() == DIRECTORY) {
                            File dir = null;
                            if (mode == EXTRACT) {
                                dir = new File(trgPath + FILE_SEP + name);
                                dir.mkdirs();
                            }
                            readDirectory(srcPath + FILE_SEP + name, entryINode, mode, mode == EXTRACT ? (trgPath + FILE_SEP + name) : (srcPath + FILE_SEP + name), out);
                            if (mode == EXTRACT) {
                                try {
                                    dir.setLastModified(entryINode.getModified().toDate().getTime());
                                } catch (ParseException e) {
                                    //
                                }
                            }
                        }
                        if (entryINode.getType() == FILE && mode == EXTRACT) {
                            extractFile(entryINode, entryINode.getFileSize(), entryINode.getModified(), trgPath + FILE_SEP + name);
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
                int blockNumber = inode.getBlock(i);
                byte[] data = blockNumber == 0 ? new byte[512] : disk.getBlock(blockNumber);
                int bytes = Math.min(remainingBytes, data.length);
                out.write(data, 0, bytes);
                remainingBytes -= bytes;
            }

            if (remainingBytes == 0) {
                out.flush();
                return;
            }

            // 17th pointer
            int blockNumber = inode.getBlock(0x10);
            if (blockNumber != 0) {
                remainingBytes = extractPointerBlock(blockNumber, out, remainingBytes);
            }

            if (remainingBytes == 0) {
                out.flush();
                return;
            }

            // 18th pointer
            blockNumber = inode.getBlock(0x11);
            if (blockNumber != 0) {
                remainingBytes = extractPointerPointerBlock(blockNumber, out, remainingBytes);
            }

            if (remainingBytes == 0) {
                out.flush();
                return;
            }

            // 19th pointer
            blockNumber = inode.getBlock(0x12);
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
        byte[] block = disk.getBlock(ptrPtrPtrBlockNumber);
        for (int i = 0; i < BLOCK_POINTER_COUNT && remainingBytes > 0; i++) {
            int ptrPtrBlockNumber = readDWord(block, i * 4);
            if (ptrPtrBlockNumber != 0) {
                remainingBytes = extractPointerPointerBlock(ptrPtrBlockNumber, out, remainingBytes);
            }
        }
        return remainingBytes;
    }

    private int extractPointerPointerBlock(int ptrPtrBlockNumber, OutputStream out, int remainingBytes) throws IOException {
        byte[] block = disk.getBlock(ptrPtrBlockNumber);
        for (int i = 0; i < BLOCK_POINTER_COUNT && remainingBytes > 0; i++) {
            int ptrBlockNumber = readDWord(block, i * 4);
            if (ptrBlockNumber != 0) {
                remainingBytes = extractPointerBlock(ptrBlockNumber, out, remainingBytes);
            }
        }
        return remainingBytes;
    }

    private int extractPointerBlock(int ptrBlockNumber, OutputStream out, int remainingBytes) throws IOException {
        byte[] block = disk.getBlock(ptrBlockNumber);
        for (int i = 0; i < BLOCK_POINTER_COUNT && remainingBytes > 0; i++) {
            int blockNumber = readDWord(block, i * 4);
            byte[] data = blockNumber == 0 ? new byte[512] : disk.getBlock(blockNumber);
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
}
