package au.wildie.m68k.cromixfs.disk;

import java.io.IOException;

public interface DiskInterface {
    byte[] getSuperBlock();
    byte[] getBlock(int blockNumber);
    void checkSupported();
    void writeImage(String fileName, boolean interleaved) throws IOException;
    String getFormatLabel();
    Integer getTrackCount();
    Integer getTrackCount(int head);
    Integer getSectorErrorCount();
}
