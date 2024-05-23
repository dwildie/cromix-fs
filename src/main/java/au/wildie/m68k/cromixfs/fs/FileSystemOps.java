package au.wildie.m68k.cromixfs.fs;

import au.wildie.m68k.cromixfs.disk.DiskInfo;
import java.io.IOException;
import java.io.PrintStream;

public interface FileSystemOps {
    FileSystemTreeDirectoryNode tree();
    void list(PrintStream out) throws IOException;
    void extract(String path, PrintStream out) throws IOException;
    String getName();
    DiskInfo getDisk();
    String getVersion();
}
