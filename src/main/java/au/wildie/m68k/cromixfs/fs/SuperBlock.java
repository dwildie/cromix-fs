package au.wildie.m68k.cromixfs.fs;

import static au.wildie.m68k.cromixfs.fs.CromixTime.TIME_SIZE;
import static au.wildie.m68k.cromixfs.fs.Inode.INODES_PER_BLOCK;
import static au.wildie.m68k.cromixfs.utils.BinUtils.readDWord;
import static au.wildie.m68k.cromixfs.utils.BinUtils.readString;
import static au.wildie.m68k.cromixfs.utils.BinUtils.readWord;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SuperBlock {
    private static final int SUPER_VERSION_OFFSET           = 0x000;
    private static final int SUPER_CROMIX_OFFSET            = 0x002;
    private static final int SUPER_INODE_FIRST_OFFSET       = 0x008;
    private static final int SUPER_INODE_COUNT_OFFSET       = 0x00a;
    private static final int SUPER_BLOCK_COUNT_OFFSET       = 0x00c;
    private static final int SUPER_LAST_MODIFIED_OFFSET     = 0x010;
    private static final int SUPER_BLOCK_SIZE_OFFSET        = 0x016;
    private static final int SUPER_FREE_BLOCK_COUNT_OFFSET  = 0x01c;
    private static final int SUPER_FREE_BLOCK_LIST_OFFSET   = 0x01e;
    private static final int SUPER_FREE_INODE_COUNT_OFFSET  = 0x15e;
    private static final int SUPER_FREE_INODE_LIST_OFFSET   = 0x160;

    public static final int FREE_BLOCK_LIST_SIZE = 80;
    public static final int FREE_INODE_LIST_SIZE = 80;

    private int versionMinor;
    private int versionMajor;
    private String cromix;
    private int firstInode;
    private int inodeCount;
    private CromixTime lastModified;
    private int blockCount;
    private int blockSize;
    private int freeBlockCount;
    private int[] freeBlockList;
    private int freeInodeCount;
    private int[] freeInodeList;

    private int firstDataBlock;
    private int dataBlockCount;

    public static SuperBlock from(byte[] data) {
        SuperBlock superBlock = new SuperBlock();

        superBlock.cromix = readString(data, SUPER_CROMIX_OFFSET);
        superBlock.versionMajor = data[SUPER_VERSION_OFFSET];
        superBlock.versionMinor = data[SUPER_VERSION_OFFSET + 1];
        superBlock.firstInode = readWord(data, SUPER_INODE_FIRST_OFFSET);
        superBlock.inodeCount = readWord(data, SUPER_INODE_COUNT_OFFSET);
        superBlock.blockCount = readDWord(data, SUPER_BLOCK_COUNT_OFFSET);
        superBlock.lastModified = CromixTime.from(Arrays.copyOfRange(data, SUPER_LAST_MODIFIED_OFFSET, SUPER_LAST_MODIFIED_OFFSET + TIME_SIZE));
        superBlock.blockSize = readDWord(data, SUPER_BLOCK_SIZE_OFFSET) == 0 ? data.length : readDWord(data, SUPER_BLOCK_SIZE_OFFSET);
        superBlock.freeBlockCount = readWord(data, SUPER_FREE_BLOCK_COUNT_OFFSET);
        superBlock.freeBlockList = new int[FREE_BLOCK_LIST_SIZE];
        for (int i = 0; i < FREE_BLOCK_LIST_SIZE; i++) {
            superBlock.freeBlockList[i] = readDWord(data, SUPER_FREE_BLOCK_LIST_OFFSET + i * 4);
        }
        superBlock.freeInodeCount = readWord(data, SUPER_FREE_INODE_COUNT_OFFSET);
        superBlock.freeInodeList = new int[FREE_INODE_LIST_SIZE];
        for (int i = 0; i < FREE_INODE_LIST_SIZE; i++) {
            superBlock.freeInodeList[i] = 0xFFFF & readWord(data, SUPER_FREE_INODE_LIST_OFFSET + i * 2);
        }

        superBlock.firstDataBlock = (superBlock.inodeCount + INODES_PER_BLOCK - 1) / INODES_PER_BLOCK + superBlock.firstInode;
        superBlock.dataBlockCount = superBlock.blockCount - superBlock.firstDataBlock;

        return superBlock;
    }

    public String getVersion() {
        return String.format("%02x%02x", versionMajor, versionMinor);
    }
}
