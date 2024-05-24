package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.fs.FileSystemFileNode;
import lombok.Getter;

@Getter
public class CromixFileSystemFileNode extends FileSystemFileNode implements CromixFileSystemNode {
    private final Inode inode;

    public CromixFileSystemFileNode(String name, Inode inode) {
        super(name);
        this.inode = inode;
    }
}
