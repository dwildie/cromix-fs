package au.wildie.m68k.cromixfs.fs;

import au.wildie.m68k.cromixfs.disk.DiskInfo;
import au.wildie.m68k.cromixfs.fs.cromix.Inode;
import java.io.IOException;
import java.io.PrintStream;

public interface FileSystemOps {
    FileSystemDirectoryNode tree();
    void list(PrintStream out) throws IOException;
    void extract(String path, PrintStream out) throws IOException;
    byte[] readFile(String name, Inode inode) throws IOException;
    String getName();
    DiskInfo getDisk();
    String getVersion();
}
