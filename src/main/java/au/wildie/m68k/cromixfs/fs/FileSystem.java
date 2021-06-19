package au.wildie.m68k.cromixfs.fs;

import java.io.IOException;
import java.io.PrintStream;

public interface FileSystem extends FileSystemOps {
    void list(PrintStream out) throws IOException;
    void extract(String path, PrintStream out) throws IOException;
}
