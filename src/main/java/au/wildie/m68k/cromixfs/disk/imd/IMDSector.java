package au.wildie.m68k.cromixfs.disk.imd;

import au.wildie.m68k.cromixfs.disk.DiskSector;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IMDSector extends DiskSector {
    private int number;
    private int encoding;
    private int offset;
    private int srcOffset;
    private boolean valid;
    private byte[] data;

    public IMDSector(int size) {
        data = new byte[size];
    }
}
