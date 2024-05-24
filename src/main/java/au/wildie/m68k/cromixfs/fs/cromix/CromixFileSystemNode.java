package au.wildie.m68k.cromixfs.fs.cromix;

public interface CromixFileSystemNode {
    Inode getInode();
    String getName();
}
