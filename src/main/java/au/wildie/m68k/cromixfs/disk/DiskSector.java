package au.wildie.m68k.cromixfs.disk;

public abstract class DiskSector {
    public abstract byte[] getData();
    public abstract boolean isValid();
}
