package au.wildie.m68k.cromixfs.disk;

public interface DiskInterface {
    byte[] getSuperBlock();
    byte[] getBlock(int blockNumber);
}
