package au.wildie.m68k.cromixfs.fs;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import lombok.Getter;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

public class CDOSFileSystem implements FileSystem {
    private static final int LABEL_ENTRY_OFFSET_FLAG = 0;
    private static final int LABEL_ENTRY_OFFSET_NAME = 1;
    private static final int LABEL_ENTRY_OFFSET_MONTH = 9;
    private static final int LABEL_ENTRY_OFFSET_DAY = 10;
    private static final int LABEL_ENTRY_OFFSET_YEAR = 11;
    private static final int LABEL_ENTRY_OFFSET_CLUSTER_SIZE = 12;
    private static final int LABEL_ENTRY_OFFSET_FLAGS = 13;
    private static final int LABEL_ENTRY_OFFSET_DIR_RECORDS = 15;
    private static final int LABEL_ENTRY_OFFSET_CLUSTER_0 = 16;

    private static final int LABEL_ENTRY_LENGTH_NAME = 8;


    private static final int DIR_ENTRY_OFFSET_FLAG = 0;
    private static final int DIR_ENTRY_OFFSET_FILENAME = 1;
    private static final int DIR_ENTRY_OFFSET_EXTENSION = 9;
    private static final int DIR_ENTRY_OFFSET_EXTENT = 12;

    private static final int DIR_ENTRY_LENGTH_FILENAME = 8;
    private static final int DIR_ENTRY_LENGTH_EXTENSION = 3;

    private static final int FLAG_LABEL = 0x81;
    private static final int FLAG_ERASED = 0xE5;

    @Getter
    private final DiskInterface disk;
    private final String labelName;
    private final String created;
    private final int recordSize = 128;
    private final int directoryEntrySize = 32;
    private final int entriesPerRecord = recordSize / directoryEntrySize;
    private final int blocksPerCluster;
    private final int recordsPerCluster;
    private final byte flags;
    private final int directoryRecords;
    private final int[] clusterNumbers;

    public CDOSFileSystem(DiskInterface disk) throws IOException {
        this.disk = disk;

        byte[] label = getDisk().getSuperBlock();
        if ((label[LABEL_ENTRY_OFFSET_FLAG] & 0xFF) != FLAG_LABEL) {
            throw new CDOSFileSystemException(String.format("Unexpected first byte in disk label: %02x", label[0]));
        }
        labelName = new String(Arrays.copyOfRange(label, LABEL_ENTRY_OFFSET_NAME, LABEL_ENTRY_OFFSET_NAME + LABEL_ENTRY_LENGTH_NAME));
        created = String.format("%d-%d-%d", (1900 + label[LABEL_ENTRY_OFFSET_YEAR]), label[LABEL_ENTRY_OFFSET_MONTH], label[LABEL_ENTRY_OFFSET_DAY]);
        recordsPerCluster = (label[LABEL_ENTRY_OFFSET_CLUSTER_SIZE] & 0xFF);
        blocksPerCluster = recordsPerCluster / 4;
        flags = label[LABEL_ENTRY_OFFSET_FLAGS];
        directoryRecords = (label[LABEL_ENTRY_OFFSET_DIR_RECORDS] & 0xFF);
        if (twoByteClusterPointers()) {
            // 8 two byte cluster pointers
            clusterNumbers = new int[8];
            for (int i = 0; i < clusterNumbers.length; i++) {
                clusterNumbers[i] = label[LABEL_ENTRY_OFFSET_CLUSTER_0 + i * 2] * 0x100 + label[LABEL_ENTRY_OFFSET_CLUSTER_0 + i * 2 + 1];
            }
        } else {
            // 16 single byte cluster pointers
            clusterNumbers = new int[16];
            for (int i = 0; i < clusterNumbers.length; i++) {
                clusterNumbers[i] = label[LABEL_ENTRY_OFFSET_CLUSTER_0 + i];
            }
        }
    }

    @Override
    public void list(PrintStream out) throws IOException {
        for (int i = 0; i < directoryRecords * entriesPerRecord; i++) {
            byte[] entry = getDirectoryEntry(i);
            int flag = entry[DIR_ENTRY_OFFSET_FLAG] & 0xFF;
            if (flag != FLAG_LABEL && flag != FLAG_ERASED) {
                if ((entry[DIR_ENTRY_OFFSET_EXTENT] & 0xFF) == 0) {
                    String fileName = String.format("%s.%s",
                            new String(Arrays.copyOfRange(entry, DIR_ENTRY_OFFSET_FILENAME, DIR_ENTRY_OFFSET_FILENAME + DIR_ENTRY_LENGTH_FILENAME)).trim(),
                            new String(Arrays.copyOfRange(entry, DIR_ENTRY_OFFSET_EXTENSION, DIR_ENTRY_OFFSET_EXTENSION + DIR_ENTRY_LENGTH_EXTENSION)).trim());

                    out.printf("%s\n", fileName);
                }
            }
        }
    }

    protected byte[] getDirectoryEntry(int entryNumber) throws IOException {
        int clusterNumber = clusterNumbers[(entryNumber / entriesPerRecord) / recordsPerCluster];
        byte[] cluster = getCluster(clusterNumber);
        int entryOffset = (entryNumber % (entriesPerRecord * recordsPerCluster)) * directoryEntrySize;
        return Arrays.copyOfRange(cluster, entryOffset, entryOffset + directoryEntrySize);
    }

    @Override
    public void extract(String path, PrintStream out) throws IOException {

    }

    protected boolean twoByteClusterPointers() {
        return (flags & 0x80) != 0;
    }

    protected byte[] getCluster(int clusterNumber) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (int i = 0; i < blocksPerCluster; i++) {
            out.write(disk.getBlock(clusterNumber * blocksPerCluster + i));
        }

        return out.toByteArray();
    }
}
