package au.wildie.m68k.cromixfs.fs;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.imd.ImageException;
import lombok.Getter;

import java.io.*;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

import static au.wildie.m68k.cromixfs.fs.DumpMode.EXTRACT;
import static au.wildie.m68k.cromixfs.fs.DumpMode.LIST;
import static au.wildie.m68k.cromixfs.utils.BinUtils.*;

public class CromixFileSystem implements FileSystem {
    private static final int SUPER_VERSION_OFFSET           = 0x000;
    private static final int SUPER_CROMIX_OFFSET            = 0x002;
    private static final int SUPER_INODE_FIRST_OFFSET       = 0x008;
    private static final int SUPER_INODE_COUNT_OFFSET       = 0x00a;
    private static final int SUPER_BLOCK_COUNT_OFFSET       = 0x00c;
    private static final int SUPER_LAST_MODIFIED_OFFSET     = 0x010;
    private static final int SUPER_BLOCK_SIZE_OFFSET        = 0x016;
    private static final int SUPER_FREE_INODES_OFFSET       = 0x01c;
    private static final int SUPER_FREE_INODE_LIST_OFFSET   = 0x01e;
    private static final int SUPER_FREE_BLOCKS_OFFSET       = 0x15e;
    private static final int SUPER_FREE_BLOCK_LIST_OFFSET   = 0x160;

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

    private static final int TIME_SIZE = 6;

    // Always use forward slash as per cromix
    private static final String FILE_SEP = "/";

    @Getter
    private final DiskInterface disk;

    private final int versionMinor;
    private final int versionMajor;
    private final int inodeFirst;
    private final int inodeCount;
    private final int blockCount;
    private final int blockSize;

    public static boolean isValid(DiskInterface disk) {
        try {
            byte[] superBlock = disk.getSuperBlock();
            String cromix = readString(superBlock, SUPER_CROMIX_OFFSET);
            return cromix.equals("cromix");
        } catch (Exception ignored) {
        }
        return false;
    }

    public CromixFileSystem(DiskInterface disk) throws IOException {
//        disk.checkSupported();

        this.disk = disk;

        byte[] superBlock = disk.getSuperBlock();

        String cromix = readString(superBlock, SUPER_CROMIX_OFFSET);
        if (!cromix.equals("cromix")) {
            throw new CromixFileSystemException("Not a valid cromix filesystem");
        }

        versionMajor = superBlock[SUPER_VERSION_OFFSET];
        versionMinor = superBlock[SUPER_VERSION_OFFSET + 1];
        inodeFirst = readWord(superBlock, SUPER_INODE_FIRST_OFFSET);
        inodeCount = readWord(superBlock, SUPER_INODE_COUNT_OFFSET);
        blockCount = readDWord(superBlock, SUPER_BLOCK_COUNT_OFFSET);
        blockSize = readDWord(superBlock, SUPER_BLOCK_SIZE_OFFSET) == 0 ? superBlock.length : readDWord(superBlock, SUPER_BLOCK_SIZE_OFFSET);
    }

    public String getVersion() {
        return String.format("%02x%02x", versionMajor, versionMinor);
    }

    @Override
    public String getName() {
        return "Cromix filesystem";
    }

    @Override
    public void list(PrintStream out) throws IOException {
        out.printf("Version: %s\n", getVersion());
        readDirectory("", readINode( 1), LIST, null, out);
    }

    @Override
    public void extract(String path, PrintStream out) throws IOException {
        readDirectory("", readINode( 1), EXTRACT, path, out);
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

                        CromixTimeUtils modified = new CromixTimeUtils(Arrays.copyOfRange(entryINode, INODE_MODIFIED_OFFSET, INODE_MODIFIED_OFFSET + TIME_SIZE));

                        if (type == INODE_TYPE_CHAR || type == INODE_TYPE_BLOCK) {
                            int major = 0xFF & entryINode[INODE_MAJOR_OFFSET];
                            int minor = 0xFF & entryINode[INODE_MINOR_OFFSET];

                            out.printf("  %3d,%-3d", major, minor);
                        } else {
                            out.printf("%9d", size);
                        }
                        out.printf(" %s %2d %s %s %s %5d %5d %s %s%s%s%n",
                                getType(type),
                                lnks,
                                getPermission(usrP),
                                getPermission(grpP),
                                getPermission(othP),
                                owner,
                                group,
                                modified.toString(),
                                srcPath,
                                FILE_SEP,
                                name);

                        if (type == INODE_TYPE_DIR) {
                            File dir = null;
                            if (mode == EXTRACT) {
                                dir = new File(trgPath + FILE_SEP + name);
                                dir.mkdirs();
                            }
                            readDirectory(srcPath + FILE_SEP + name, entryINode, mode, mode == EXTRACT ? (trgPath + FILE_SEP + name) : (srcPath + FILE_SEP + name), out);
                            if (mode == EXTRACT) {
                                try {
                                    dir.setLastModified(modified.toDate().getTime());
                                } catch (ParseException e) {
                                    //
                                }
                            }
                        }
                        if (type == INODE_TYPE_FILE && mode == EXTRACT) {
                            extractFile(entryINode, size, modified, trgPath + FILE_SEP + name);
                        }
                    }
                }
            }
        }
        out.print("");
    }

    private void extractFile(byte[] inode, int size, CromixTimeUtils modified, String path) throws IOException {
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

    private byte[] readINode(int inodeNumber) throws IOException {
        int blockNumber = inodeFirst + (inodeNumber - 1) / 4;
        byte[] block = disk.getBlock(blockNumber);
        int startInode = ((inodeNumber - 1) % 4) * INODE_LENGTH;
        return Arrays.copyOfRange(block, startInode, startInode + INODE_LENGTH);
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
