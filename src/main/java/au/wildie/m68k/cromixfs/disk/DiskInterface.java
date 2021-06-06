package au.wildie.m68k.cromixfs.disk;

import java.io.IOException;

public interface DiskInterface {
    byte[] getSuperBlock() throws IOException;
    byte[] getBlock(int blockNumber) throws IOException;
    void checkSupported();
    void writeImage(String fileName, boolean interleaved) throws IOException;
    String getFormatLabel();
    Integer getTrackCount();
    Integer getTrackCount(int head);
    Integer getSectorErrorCount();
}
