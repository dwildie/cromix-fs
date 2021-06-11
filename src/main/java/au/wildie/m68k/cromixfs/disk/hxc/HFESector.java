package au.wildie.m68k.cromixfs.disk.hxc;

import java.io.PrintStream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HFESector {
    private final int number; // 1 based
    private final byte[] data;

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

}
