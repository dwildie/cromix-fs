package au.wildie.m68k.cromixfs.disk.st;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.PrintStream;
import java.util.Optional;

public class STDCPartitionTable {
    public static final int TABLE_SIZE = 32;
    private final STDCDiskInfo info;
    private final Entry[] table = new Entry[TABLE_SIZE];

    public STDCPartitionTable(STDCDiskInfo info, byte[][][][] media) {
        this(info, media[0][0][info.getStartOfPartitionTable() / info.getBytesPerSector()]);
    }

    public STDCPartitionTable(STDCDiskInfo info, byte[] sector) {
        this.info = info;

        table[0] = new Entry(1, info.getCylinderCount() - 1);
        table[TABLE_SIZE - 1] = new Entry(0, info.getCylinderCount());

        for (int i = 0 ; i < TABLE_SIZE; i++) {
            int startCylinder = ((0xFF & sector[i * 2]) << 8) + (0xFF & sector[i * 2 + 1]);
            if (startCylinder == 0xe5e5) {
                break;
            }
            table[i + 1] = new Entry(startCylinder, info.getCylinderCount() - startCylinder);
            table[i].numberOfCylinders = startCylinder - table[i].startCylinder;
        }
    }

    public Optional<Integer> getStartCylinder(int partitionIndex) {
        return Optional.ofNullable(table[partitionIndex]).map(Entry::getStartCylinder);
    }

    @ToString
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class Entry {
        private int startCylinder;
        private int numberOfCylinders;
    }

    public void logTable(PrintStream out) {
        out.println("Partition #    Starting Cylinder   Number of cylinders   Size (Kbytes)");
        for (int i = 0; i < TABLE_SIZE; i++) {
            if (table[i] != null) {
                out.printf("   %2d               %5d                 %5d         %,10d    \n",
                        i,
                        table[i].startCylinder,
                        table[i].numberOfCylinders,
                        (table[i].numberOfCylinders * info.getSurfaceCount() * info.getSectorsPerTrack() * info.getBytesPerSector()) / 1024);
            }
        }
    }
}