package au.wildie.m68k.cromixfs.disk.imd;

import au.wildie.m68k.cromixfs.disk.DiskSector;
import lombok.Getter;
import lombok.Setter;

import static au.wildie.m68k.cromixfs.disk.imd.IMDImage.SECTOR_ENCODING_NORMAL;

@Getter
@Setter
public class IMDSector extends DiskSector {
    private int number;
    private int encoding;
    private int offset;
    private int srcOffset;
    private boolean valid;
    private byte[] data;

    public IMDSector(int number, int offset, int size) {
        this.number = number;
        this.encoding = SECTOR_ENCODING_NORMAL;
        this.offset = offset;
        this.srcOffset = offset;
        this.valid = true;
        this.data = new byte[size];

        for (int i = 0; i < size; i++) {
            this.data[i] = (byte)0xE5;
        }
    }

    public IMDSector(int size) {
        data = new byte[size];
    }
}
