package au.wildie.m68k.cromixfs.disk;

public abstract class DiskImage {
    public abstract int getCylinders();
    public abstract int getHeads();
    public abstract DiskSector getSector(int cylinder, int head, int sectorNumber);
}

