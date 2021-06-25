package au.wildie.m68k.cromixfs.fs.cromix;

import static au.wildie.m68k.cromixfs.fs.CromixTime.TIME_SIZE;
import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.*;
import static au.wildie.m68k.cromixfs.utils.BinUtils.*;

import java.util.Arrays;

import au.wildie.m68k.cromixfs.fs.CromixTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Inode {
    public static final int INODE_LENGTH = 0x80;
    public static final int INODES_PER_BLOCK = 4;

    public static final int INODE_OWNER_OFFSET       = 0x00;
    public static final int INODE_GROUP_OFFSET       = 0x02;
    public static final int INODE_P_OWN_OFFSET       = 0x04;
    public static final int INODE_P_GRP_OFFSET       = 0x05;
    public static final int INODE_P_OTH_OFFSET       = 0x06;
    public static final int INODE_TYPE_OFFSET        = 0x07;
    public static final int INODE_LINKS_OFFSET       = 0x08;
    public static final int INODE_F_SIZE_OFFSET      = 0x0A;
    public static final int INODE_NUMBER_OFFSET      = 0x0E;
    public static final int INODE_PARENT_OFFSET      = 0x10;
    public static final int INODE_DIR_COUNT_OFFSET   = 0x12;
    public static final int INODE_MAJOR_OFFSET       = 0x12;
    public static final int INODE_MINOR_OFFSET       = 0x13;
    public static final int INODE_USED_BLOCKS_OFFSET = 0x14;
    public static final int INODE_CREATED_OFFSET     = 0x18;
    public static final int INODE_MODIFIED_OFFSET    = 0x1E;
    public static final int INODE_ACCESSED_OFFSET    = 0x24;
    public static final int INODE_DUMPED_OFFSET      = 0x2A;
    public static final int INODE_PTRS_OFFSET        = 0x30;

    public static final int INODE_BLOCKS = 20;

    public static final int DIRECT_BLOCKS = 0x10;
    public static final int INDIRECT_1_BLOCK = 0x10;
    public static final int INDIRECT_2_BLOCK = 0x11;
    public static final int INDIRECT_3_BLOCK = 0x12;

    private int number;
    private int parent;
    private InodeType type;
//    private int[] permissions = new int[3];
    private int owner;
    private int group;
    private int dirCount;
    private int fileSize;
    private int usedBlockCount;
    private int major;
    private int minor;
    private int userPermission;
    private int groupPermission;
    private int otherPermission;
    private int links;
    private CromixTime created;
    private CromixTime modified;
    private CromixTime accessed;
    private CromixTime dumped;
    private int[] blocks = new int[INODE_BLOCKS];

    public static Inode from(byte[] raw) {
        Inode inode = new Inode(0xFFFF & readWord(raw, INODE_NUMBER_OFFSET));
        inode.parent = 0xFFFF & readWord(raw, INODE_PARENT_OFFSET);
        inode.type = InodeType.from(0xFF & raw[INODE_TYPE_OFFSET]);
//        inode.permissions[0] =  0xFF & raw[INODE_P_OWN_OFFSET];
//        inode.permissions[1] =  0xFF & raw[INODE_P_GRP_OFFSET];
//        inode.permissions[2] =  0xFF & raw[INODE_P_OTH_OFFSET];
        inode.owner = 0xFFFF & readWord(raw, INODE_OWNER_OFFSET);
        inode.group = 0xFFFF & readWord(raw, INODE_GROUP_OFFSET);
        if (inode.type == CHARACTER_DEVICE || inode.type == BLOCK_DEVICE) {
            inode.major = 0xFF & raw[INODE_MAJOR_OFFSET];
            inode.minor = 0xFF & raw[INODE_MINOR_OFFSET];
        } else {
            inode.dirCount = 0xFFFF & readWord(raw, INODE_DIR_COUNT_OFFSET);
        }
        inode.fileSize = readDWord(raw, INODE_F_SIZE_OFFSET);
        inode.usedBlockCount = readDWord(raw, INODE_USED_BLOCKS_OFFSET);
        inode.userPermission = 0xFF & raw[INODE_P_OWN_OFFSET];
        inode.groupPermission = 0xFF & raw[INODE_P_GRP_OFFSET];
        inode.otherPermission = 0xFF & raw[INODE_P_OTH_OFFSET];
        inode.links = 0xFF & raw[INODE_LINKS_OFFSET];
        inode.created = CromixTime.from(Arrays.copyOfRange(raw, INODE_CREATED_OFFSET, INODE_CREATED_OFFSET + TIME_SIZE));
        inode.modified = CromixTime.from(Arrays.copyOfRange(raw, INODE_MODIFIED_OFFSET, INODE_MODIFIED_OFFSET + TIME_SIZE));
        inode.accessed = CromixTime.from(Arrays.copyOfRange(raw, INODE_ACCESSED_OFFSET, INODE_ACCESSED_OFFSET + TIME_SIZE));
        inode.dumped = CromixTime.from(Arrays.copyOfRange(raw, INODE_DUMPED_OFFSET, INODE_DUMPED_OFFSET + TIME_SIZE));

        for (int i = 0; i < INODE_BLOCKS; i++) {
            inode.blocks[i] = readDWord(raw, INODE_PTRS_OFFSET + i * 4);
        }
        return inode;
    }

    public Inode(int number) {
        this.number = number;
        this.type = UNUSED;
    }

    public void toBytes(byte[] data, int offset) {
        writeWord(number, data, offset + INODE_NUMBER_OFFSET);
        writeWord(parent, data, offset + INODE_PARENT_OFFSET);
        data[offset + INODE_TYPE_OFFSET] = (byte)type.to();
        data[offset + INODE_P_OWN_OFFSET] = (byte)userPermission;
        data[offset + INODE_P_GRP_OFFSET] = (byte)groupPermission;
        data[offset + INODE_P_OTH_OFFSET] = (byte)otherPermission;
        writeWord(owner, data, offset + INODE_OWNER_OFFSET);
        writeWord(group, data, offset + INODE_GROUP_OFFSET);
        if (type == CHARACTER_DEVICE || type == BLOCK_DEVICE) {
            data[offset + INODE_MAJOR_OFFSET] = (byte)(0xFF & major);
            data[offset + INODE_MINOR_OFFSET] = (byte)(0xFF & minor);
        } else {
            writeDWord(dirCount, data, offset + INODE_DIR_COUNT_OFFSET);
        }
        writeDWord(fileSize, data, offset + INODE_F_SIZE_OFFSET);
        writeDWord(usedBlockCount, data, offset + INODE_USED_BLOCKS_OFFSET);
        data[offset + INODE_LINKS_OFFSET] = (byte)(0xFF & links);
        timeToBytes(created, data, offset + INODE_CREATED_OFFSET);
        timeToBytes(modified, data, offset + INODE_MODIFIED_OFFSET);
        timeToBytes(accessed, data, offset + INODE_ACCESSED_OFFSET);
        timeToBytes(dumped, data, offset + INODE_DUMPED_OFFSET);

        for (int i = 0; i < INODE_BLOCKS; i++) {
            writeDWord(blocks[i], data, offset + INODE_PTRS_OFFSET + i * 4);
        }
    }

    protected void timeToBytes(CromixTime time, byte[] data, int offset) {
        if (time != null) {
            System.arraycopy(time.toBytes(), 0, data, offset, TIME_SIZE);
        } else {
            Arrays.fill(data, offset, offset + TIME_SIZE, (byte)0);
        }
    }

    public int getBlockNumber(int blockIndex) {
        return blocks[blockIndex];
    }

    public String getTypeChar() {
        switch (type) {
            case FILE:
                return "F";

            case DIRECTORY:
                return "D";

            case CHARACTER_DEVICE:
                return "C";

            case BLOCK_DEVICE:
                return "B";

            case PIPE:
                return "P";

            case SHARED_TEXT:
                return "S";

            default:
                return "U";
        }
    }

}
