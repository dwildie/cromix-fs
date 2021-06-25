package au.wildie.m68k.cromixfs.disk;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public interface DiskInterface extends DiskInfo {
    byte[] getSuperBlock() throws IOException;
    void setSuperBlock(byte[] data);
    byte[] getBlock(int blockNumber) throws IOException;
    void writeImage(File file, boolean interleaved) throws IOException;

    void persist(OutputStream archive) throws IOException;
}
