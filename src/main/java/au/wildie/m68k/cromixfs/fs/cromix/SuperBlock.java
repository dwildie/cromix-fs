package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.fs.CromixTime;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

import static au.wildie.m68k.cromixfs.fs.CromixTime.TIME_SIZE;
import static au.wildie.m68k.cromixfs.fs.cromix.Inode.INODES_PER_BLOCK;
import static au.wildie.m68k.cromixfs.utils.BinUtils.*;

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

    private boolean dirty;

    public static SuperBlock initialise (String label) {
        SuperBlock superBlock = new SuperBlock();
        superBlock.setVersionMajor(0x31);
        superBlock.setVersionMinor(0x68);
        superBlock.setCromix("cromix");
        superBlock.setFirstInode(20);
        superBlock.setInodeCount(508);
        superBlock.setBlockCount(2454);
        superBlock.setLastModified(CromixTime.now());
        superBlock.setBlockSize(512);
        superBlock.setFreeBlockCount(0);
        superBlock.freeBlockList = new int[FREE_BLOCK_LIST_SIZE];
        superBlock.setFreeInodeCount(0);
        superBlock.setFreeInodeList(new int[FREE_INODE_LIST_SIZE]);

        superBlock.firstDataBlock = (superBlock.inodeCount + INODES_PER_BLOCK - 1) / INODES_PER_BLOCK + superBlock.firstInode;
        superBlock.dataBlockCount = superBlock.blockCount - superBlock.firstDataBlock;

        superBlock.dirty = true;
        return superBlock;
    }

    public static SuperBlock from(byte[] data) {
        SuperBlock superBlock = new SuperBlock();

        superBlock.versionMajor = data[SUPER_VERSION_OFFSET];
        superBlock.versionMinor = data[SUPER_VERSION_OFFSET + 1];
        superBlock.cromix = readString(data, SUPER_CROMIX_OFFSET);
        superBlock.firstInode = readWord(data, SUPER_INODE_FIRST_OFFSET);
        superBlock.inodeCount = readWord(data, SUPER_INODE_COUNT_OFFSET);
        superBlock.blockCount = readDWord(data, SUPER_BLOCK_COUNT_OFFSET);
        superBlock.lastModified = CromixTime.from(Arrays.copyOfRange(data, SUPER_LAST_MODIFIED_OFFSET, SUPER_LAST_MODIFIED_OFFSET + TIME_SIZE));
        superBlock.blockSize = readDWord(data, SUPER_BLOCK_SIZE_OFFSET) == 0 ? data.length : readDWord(data, SUPER_BLOCK_SIZE_OFFSET);
        superBlock.freeBlockCount = readWord(data, SUPER_FREE_BLOCK_COUNT_OFFSET);
        superBlock.freeBlockList = new int[FREE_BLOCK_LIST_SIZE];
        for (int i = 0; i < FREE_BLOCK_LIST_SIZE; i++) {
            superBlock.freeBlockList[i] = readDWord(data, SUPER_FREE_BLOCK_LIST_OFFSET + i * DWORD_SIZE);
        }
        superBlock.freeInodeCount = readWord(data, SUPER_FREE_INODE_COUNT_OFFSET);
        superBlock.freeInodeList = new int[FREE_INODE_LIST_SIZE];
        for (int i = 0; i < FREE_INODE_LIST_SIZE; i++) {
            superBlock.freeInodeList[i] = 0xFFFF & readWord(data, SUPER_FREE_INODE_LIST_OFFSET + i * WORD_SIZE);
        }

        superBlock.firstDataBlock = (superBlock.inodeCount + INODES_PER_BLOCK - 1) / INODES_PER_BLOCK + superBlock.firstInode;
        superBlock.dataBlockCount = superBlock.blockCount - superBlock.firstDataBlock;

        return superBlock;
    }

    public byte[] toBytes() {
        byte[] data = new byte[512];

        data[SUPER_VERSION_OFFSET] = (byte)versionMajor;
        data[SUPER_VERSION_OFFSET + 1] = (byte)versionMinor;
        writeString(cromix, data, SUPER_CROMIX_OFFSET);
        writeWord(firstInode, data, SUPER_INODE_FIRST_OFFSET);
        writeWord(inodeCount, data, SUPER_INODE_COUNT_OFFSET);
        writeDWord(blockCount, data, SUPER_BLOCK_COUNT_OFFSET);
        System.arraycopy(lastModified.toBytes(), 0, data, SUPER_LAST_MODIFIED_OFFSET, TIME_SIZE);
        writeDWord(blockSize, data, SUPER_BLOCK_SIZE_OFFSET);
        writeWord(freeBlockCount, data, SUPER_FREE_BLOCK_COUNT_OFFSET);
        for (int i = 0; i < FREE_BLOCK_LIST_SIZE; i++) {
            writeDWord(freeBlockList[i], data, SUPER_FREE_BLOCK_LIST_OFFSET + i * DWORD_SIZE);
        }
        writeWord(freeInodeCount, data, SUPER_FREE_INODE_COUNT_OFFSET);
        for (int i = 0; i < FREE_INODE_LIST_SIZE; i++) {
            writeWord(freeInodeList[i], data, SUPER_FREE_INODE_LIST_OFFSET + i * WORD_SIZE);
        }

        return data;
    }

    public String getVersion() {
        return String.format("%02x%02x", versionMajor, versionMinor);
    }
}
