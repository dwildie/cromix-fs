package au.wildie.m68k.cromixfs.fs;

import au.wildie.m68k.cromixfs.disk.DiskInterface;

import java.io.*;
import java.util.Arrays;

import static au.wildie.m68k.cromixfs.fs.DumpMode.EXTRACT;
import static au.wildie.m68k.cromixfs.fs.DumpMode.LIST;

public class FileSystem {
    private static final int SUPER_INODE_FIRST_OFFSET = 0x08;
    private static final int SUPER_INODE_COUNT_OFFSET = 0x0a;
    private static final int SUPER_CROMIX_OFFSET = 0x02;

    private static final int INODE_LENGTH = 0x80;

    private static final int INODE_OWNER_OFFSET     = 0x00;
    private static final int INODE_GROUP_OFFSET     = 0x02;
    private static final int INODE_P_OWN_OFFSET     = 0x04;
    private static final int INODE_P_GRP_OFFSET     = 0x05;
    private static final int INODE_P_OTH_OFFSET     = 0x06;
    private static final int INODE_TYPE_OFFSET      = 0x07;
    private static final int INODE_LINKS_OFFSET     = 0x08;
    private static final int INODE_F_SIZE_OFFSET    = 0x0A;
    private static final int INODE_NUMBER_OFFSET    = 0x0E;
    private static final int INODE_PARENT_OFFSET    = 0x10;
    private static final int INODE_D_COUNT_OFFSET   = 0x12;
    private static final int INODE_MAJOR_OFFSET     = 0x12;
    private static final int INODE_MINOR_OFFSET     = 0x13;
    private static final int INODE_BLOCKS_OFFSET    = 0x14;
    private static final int INODE_CREATED_OFFSET   = 0x18;
    private static final int INODE_MODIFIED_OFFSET  = 0x1E;
    private static final int INODE_ACCESSED_OFFSET  = 0x24;
    private static final int INODE_DUMPED_OFFSET    = 0x2A;
    private static final int INODE_PTRS_OFFSET      = 0x30;


    private static final int INODE_TYPE_FILE = 0x80;
    private static final int INODE_TYPE_DIR = 0x81;
    private static final int INODE_TYPE_CHAR = 0x82;
    private static final int INODE_TYPE_BLOCK = 0x83;
    private static final int INODE_TYPE_PIPE = 0x84;

    private static final int DIR_ENTRY_LENGTH = 0x20;

    private static final int BLOCK_POINTER_COUNT = 0x80;

    private final DiskInterface disk;

    private final int inodeFirst;
    private final int inodeCount;

    public FileSystem(DiskInterface disk) {
        disk.checkSupported();

        this.disk = disk;

        byte[] superBlock = disk.getSuperBlock();

        String cromix = readString(superBlock, SUPER_CROMIX_OFFSET);
        if (!cromix.equals("cromix")) {
            throw new CromixFileSystemException("Not a valid cromix filesystem");
        }

        inodeFirst = readWord(superBlock, SUPER_INODE_FIRST_OFFSET);
        inodeCount = readWord(superBlock, SUPER_INODE_COUNT_OFFSET);
    }


    public void list(PrintStream out) throws IOException {
        readDirectory("", readINode( 1), LIST, null, out);
    }

    public void extract(String path) throws IOException {
        readDirectory("", readINode( 1), EXTRACT, path, System.out);
    }

    private void readDirectory(String srcPath, byte[] inode, DumpMode mode, String trgPath, PrintStream out) throws IOException {
        for (int i = 0; i < 0x10; i++) {
            int blockNumber = readDWord(inode, INODE_PTRS_OFFSET + i * 4);
            if (blockNumber != 0) {
                byte[] data = disk.getBlock(blockNumber);

                for (int j = 0; j < 0x10; j++) {
                    if (data[j * DIR_ENTRY_LENGTH + 0x1c] == (byte)0x80) {
                        String name = "";
                        for (int k = 0; k < 0x18 && data[j * DIR_ENTRY_LENGTH + k] != 0; k++) {
                            name = name + (char) data[j * DIR_ENTRY_LENGTH + k];
                        }

                        int entryInodeNumber = readWord(data, j * DIR_ENTRY_LENGTH + 0x1E);
                        byte[] entryINode = readINode(entryInodeNumber);

                        int type = 0xFF & entryINode[INODE_TYPE_OFFSET];
                        int owner = readWord(entryINode, INODE_OWNER_OFFSET);
                        int group = readWord(entryINode, INODE_GROUP_OFFSET);
                        int size = type == INODE_TYPE_DIR ? readWord(entryINode, INODE_D_COUNT_OFFSET) : readDWord(entryINode, INODE_F_SIZE_OFFSET);
                        int usrP = 0xFF & entryINode[INODE_P_OWN_OFFSET];
                        int grpP = 0xFF & entryINode[INODE_P_GRP_OFFSET];
                        int othP = 0xFF & entryINode[INODE_P_OTH_OFFSET];
                        int lnks = 0xFF & entryINode[INODE_LINKS_OFFSET];

                        if (type == INODE_TYPE_CHAR || type == INODE_TYPE_BLOCK) {
                            int major = 0xFF & entryINode[INODE_MAJOR_OFFSET];
                            int minor = 0xFF & entryINode[INODE_MINOR_OFFSET];

                            out.printf("  %3d,%-3d", major, minor);
                        } else {
                            out.printf("%9d", size);
                        }
                        out.printf(" %s %2d %s %s %s %5d %5d %s%s%s%n", getType(type), lnks, getPermission(usrP), getPermission(grpP), getPermission(othP), owner, group, srcPath, File.separator, name);

                        if (type == INODE_TYPE_DIR) {
                            if (mode == EXTRACT) {
                                new File(trgPath + File.separator + name).mkdirs();
                            }
                            readDirectory(srcPath + File.separator + name, entryINode, mode, mode == EXTRACT ? (trgPath + File.separator + name) : (srcPath + File.separator + name), out);
                        }
                        if (type == INODE_TYPE_FILE && mode == EXTRACT) {
                            extractFile(entryINode, size, trgPath + File.separator + name);
                        }
                    }
                }
            }
        }
        out.print("");
    }

    private void extractFile(byte[] inode, int size, String path) throws IOException {
        int remainingBytes = size;
        File file = new File(path);
        try (OutputStream out = new FileOutputStream(file)) {
            // Read first 16 data blocks
            for (int i = 0; i < 0x10 && remainingBytes > 0; i++) {
                int blockNumber = readDWord(inode, INODE_PTRS_OFFSET + i * 4);
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
            int blockNumber = readDWord(inode, INODE_PTRS_OFFSET + 0x10 * 4);
            if (blockNumber != 0) {
                remainingBytes = writePointerBlock(blockNumber, out, remainingBytes);
            }

            if (remainingBytes == 0) {
                out.flush();
                return;
            }

            // 18th pointer
            blockNumber = readDWord(inode, INODE_PTRS_OFFSET + 0x11 * 4);
            if (blockNumber != 0) {
                remainingBytes = writePointerPointerBlock(blockNumber, out, remainingBytes);
            }

            if (remainingBytes == 0) {
                out.flush();
                return;
            }

            // 19th pointer
            blockNumber = readDWord(inode, INODE_PTRS_OFFSET + 0x12 * 4);
            if (blockNumber != 0) {
                remainingBytes = writePointerPointerPointerBlock(blockNumber, out, remainingBytes);
            }

            if (remainingBytes != 0) {
                String error = String.format("Did not read all bytes for %s, %d remaining", path, remainingBytes);
                System.out.println(error);
                //throw new RuntimeException(error);
            }

            out.flush();
        }
    }

    private int writePointerPointerPointerBlock(int ptrPtrPtrBlockNumber, OutputStream out, int remainingBytes) throws IOException {
        byte[] block = disk.getBlock(ptrPtrPtrBlockNumber);
        for (int i = 0; i < BLOCK_POINTER_COUNT && remainingBytes > 0; i++) {
            int ptrPtrBlockNumber = readDWord(block, i * 4);
            if (ptrPtrBlockNumber != 0) {
                remainingBytes = writePointerPointerBlock(ptrPtrBlockNumber, out, remainingBytes);
            }
        }
        return remainingBytes;
    }

    private int writePointerPointerBlock(int ptrPtrBlockNumber, OutputStream out, int remainingBytes) throws IOException {
        byte[] block = disk.getBlock(ptrPtrBlockNumber);
        for (int i = 0; i < BLOCK_POINTER_COUNT && remainingBytes > 0; i++) {
            int ptrBlockNumber = readDWord(block, i * 4);
            if (ptrBlockNumber != 0) {
                remainingBytes = writePointerBlock(ptrBlockNumber, out, remainingBytes);
            }
        }
        return remainingBytes;
    }

    private int writePointerBlock(int ptrBlockNumber, OutputStream out, int remainingBytes) throws IOException {
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

    private byte[] readINode(int inodeNumber) {
        int blockNumber = inodeFirst + (inodeNumber - 1) / 4;
        byte[] block = disk.getBlock(blockNumber);
        int startInode = ((inodeNumber - 1) % 4) * INODE_LENGTH;
        return Arrays.copyOfRange(block, startInode, startInode + INODE_LENGTH);
    }

    private int readDWord(byte[] data, int offset) {
        return (((((0xFF & data[offset]) << 8) + (0xFF & data[offset + 1]) << 8) + (0xFF & data[offset + 2])) << 8) + (0xFF & data[offset + 3]);
    }

    private int readWord(byte[] data, int offset) {
        return ((0xFF & data[offset]) << 8) + (0xFF & data[offset + 1]);
    }

    private String readString(byte data[], int offset) {
        String str = "";

        for (int i = 0; data[offset + i] != 0; i++) {
            str += (char)data[offset + i];
        }

        return str;
    }

    private String getType(int type) {
        if (type == INODE_TYPE_FILE) {
            return "F";
        }
        if (type == INODE_TYPE_DIR) {
            return "D";
        }
        if (type == INODE_TYPE_CHAR) {
            return "C";
        }
        if (type == INODE_TYPE_BLOCK) {
            return "B";
        }
        if (type == INODE_TYPE_PIPE) {
            return "P";
        }

        return "U";
    }

    private String getPermission(int value) {
        return ((value & 0x1) != 0 ? "r" : "-")
                + ((value & 0x2) != 0 ? "e" : "-")
                + ((value & 0x4) != 0 ? "w" : "-")
                + ((value & 0x8) != 0 ? "a" : "-");
    }

}
