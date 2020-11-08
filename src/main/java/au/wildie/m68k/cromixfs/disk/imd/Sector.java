package au.wildie.m68k.cromixfs.disk.imd;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Sector {
    private int number;
    private int encoding;
    private int offset;
    private byte[] data;

    public Sector(int size) {
        data = new byte[size];
    }
}
