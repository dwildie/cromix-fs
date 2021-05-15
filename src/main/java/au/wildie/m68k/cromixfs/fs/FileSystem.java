package au.wildie.m68k.cromixfs.fs;

import au.wildie.m68k.cromixfs.disk.DiskInterface;

import java.io.IOException;
import java.io.PrintStream;

public interface FileSystem {
    void list(PrintStream out) throws IOException;
    void extract(String path, PrintStream out) throws IOException;
    DiskInterface getDisk();
}
