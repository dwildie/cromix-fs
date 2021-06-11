package au.wildie.m68k.cromixfs.disk.hxc;

import java.io.PrintStream;
import lombok.Getter;

@Getter
public class HFESector {
    private final int number; // 1 based
    private final int cellIndex;
    private final int size;
    private final int initialCRC;
    private final int accessMark;
    private final byte[] data;
    private boolean modified = false;

    public HFESector(int number, int cellIndex, int size, int initialCRC, int accessMark) {
        this.number = number;
        this.cellIndex = cellIndex;
        this.size = size;
        this.initialCRC = initialCRC;
        this.accessMark = accessMark;
        data = new byte[size];
    }

    public void write(byte[] data) {
        if (data.length == size) {
            System.arraycopy(data, 0, this.data, 0, size);
            modified = true;
        }
    }

    protected void dump(PrintStream out) {
        out.printf("\nSector %d", number);
        for (int i = 0; i < data.length; i++) {
            if (i % 16 == 0) {
                out.printf("\n0x%04x: ", i);
            }
            out.printf(" %02x", data[i]);
        }
        out.print("\n");
    }

    protected int calculateChecksum() {
        return CheckSum.calculateCRC(data, 0, size, initialCRC);
    }
}
