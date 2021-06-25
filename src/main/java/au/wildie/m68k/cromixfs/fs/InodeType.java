package au.wildie.m68k.cromixfs.fs;


public enum InodeType {
    UNUSED, FILE, DIRECTORY, CHARACTER_DEVICE, BLOCK_DEVICE, PIPE, SHARED_TEXT, UNKNOWN;

    public static final int INODE_TYPE_FILE = 0x80;
    public static final int INODE_TYPE_DIR = 0x81;
    public static final int INODE_TYPE_CHAR = 0x82;
    public static final int INODE_TYPE_BLOCK = 0x83;
    public static final int INODE_TYPE_PIPE = 0x84;
    public static final int INODE_TYPE_SHARED_TEXT = 0x88;

    public static InodeType from(int value) {
        switch (value) {
            case 0:
                return UNUSED;
            case INODE_TYPE_FILE:
                return FILE;
            case INODE_TYPE_DIR:
                return DIRECTORY;
            case INODE_TYPE_CHAR:
                return CHARACTER_DEVICE;
            case INODE_TYPE_BLOCK:
                return BLOCK_DEVICE;
            case INODE_TYPE_PIPE:
                return PIPE;
            case INODE_TYPE_SHARED_TEXT:
                return SHARED_TEXT;
            default:
                return UNKNOWN;
        }
    }
}
