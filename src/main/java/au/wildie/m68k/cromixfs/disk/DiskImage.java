package au.wildie.m68k.cromixfs.disk;

import java.io.OutputStream;

public abstract class DiskImage {
    public abstract int getCylinders();
    public abstract int getHeads();
    public abstract DiskSector getSector(int cylinder, int head, int sectorNumber);
    public abstract void persist(OutputStream archive);
}

