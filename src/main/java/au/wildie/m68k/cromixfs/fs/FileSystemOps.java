package au.wildie.m68k.cromixfs.fs;

import au.wildie.m68k.cromixfs.disk.DiskInfo;
import au.wildie.m68k.cromixfs.disk.DiskInterface;

import java.io.IOException;
import java.io.PrintStream;

public interface FileSystemOps {
    void list(PrintStream out) throws IOException;
    void extract(String path, PrintStream out) throws IOException;
    String getName();
    DiskInfo getDisk();
}
