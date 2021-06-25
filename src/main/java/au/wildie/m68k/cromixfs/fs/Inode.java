package au.wildie.m68k.cromixfs.fs;

import static au.wildie.m68k.cromixfs.fs.CromixTime.TIME_SIZE;
import static au.wildie.m68k.cromixfs.utils.BinUtils.readDWord;
import static au.wildie.m68k.cromixfs.utils.BinUtils.readWord;
import java.util.Arrays;
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
    public static final int INODE_D_COUNT_OFFSET     = 0x12;
    public static final int INODE_MAJOR_OFFSET       = 0x12;
    public static final int INODE_MINOR_OFFSET       = 0x13;
    public static final int INODE_USED_BLOCKS_OFFSET = 0x14;
    public static final int INODE_CREATED_OFFSET     = 0x18;
    public static final int INODE_MODIFIED_OFFSET    = 0x1E;
    public static final int INODE_ACCESSED_OFFSET    = 0x24;
    public static final int INODE_DUMPED_OFFSET      = 0x2A;
    public static final int INODE_PTRS_OFFSET        = 0x30;


    public static final int INODE_BLOCKS = 20;

    private int number;
    private int parent;
    private InodeType type;
    private int[] permissions = new int[3];
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
        Inode iNode = new Inode();

        iNode.number = 0xFFFF & readWord(raw, INODE_NUMBER_OFFSET);
        iNode.parent = 0xFFFF & readWord(raw, INODE_PARENT_OFFSET);
        iNode.type = InodeType.from(0xFF & raw[INODE_TYPE_OFFSET]);
        iNode.permissions[0] =  0xFF & raw[INODE_P_OWN_OFFSET];
        iNode.permissions[1] =  0xFF & raw[INODE_P_GRP_OFFSET];
        iNode.permissions[2] =  0xFF & raw[INODE_P_OTH_OFFSET];
        iNode.owner = 0xFFFF & readWord(raw, INODE_OWNER_OFFSET);
        iNode.group = 0xFFFF & readWord(raw, INODE_GROUP_OFFSET);
        iNode.dirCount = 0xFFFF & readWord(raw, INODE_D_COUNT_OFFSET);
        iNode.fileSize = readDWord(raw, INODE_F_SIZE_OFFSET);
        iNode.usedBlockCount = readDWord(raw, INODE_USED_BLOCKS_OFFSET);
        iNode.major = 0xFF & raw[INODE_MAJOR_OFFSET];
        iNode.minor = 0xFF & raw[INODE_MINOR_OFFSET];
        iNode.userPermission = 0xFF & raw[INODE_P_OWN_OFFSET];
        iNode.groupPermission = 0xFF & raw[INODE_P_GRP_OFFSET];
        iNode.otherPermission = 0xFF & raw[INODE_P_OTH_OFFSET];
        iNode.links = 0xFF & raw[INODE_LINKS_OFFSET];
        iNode.created = CromixTime.from(Arrays.copyOfRange(raw, INODE_CREATED_OFFSET, INODE_CREATED_OFFSET + TIME_SIZE));
        iNode.modified = CromixTime.from(Arrays.copyOfRange(raw, INODE_MODIFIED_OFFSET, INODE_MODIFIED_OFFSET + TIME_SIZE));
        iNode.accessed = CromixTime.from(Arrays.copyOfRange(raw, INODE_ACCESSED_OFFSET, INODE_ACCESSED_OFFSET + TIME_SIZE));
        iNode.dumped = CromixTime.from(Arrays.copyOfRange(raw, INODE_DUMPED_OFFSET, INODE_DUMPED_OFFSET + TIME_SIZE));

        for (int i = 0; i < INODE_BLOCKS; i++) {
            iNode.blocks[i] = readDWord(raw, INODE_PTRS_OFFSET + i * 4);
        }
        return iNode;
    }

    public int getBlock(int blockIndex) {
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

            default:
                return "U";
        }
    }

}
