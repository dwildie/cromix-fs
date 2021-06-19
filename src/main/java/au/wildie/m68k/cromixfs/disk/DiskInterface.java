package au.wildie.m68k.cromixfs.disk;

import java.io.File;
import java.io.IOException;

public interface DiskInterface extends DiskInfo {
    byte[] getSuperBlock() throws IOException;
    byte[] getBlock(int blockNumber) throws IOException;
    void writeImage(File file, boolean interleaved) throws IOException;
}
