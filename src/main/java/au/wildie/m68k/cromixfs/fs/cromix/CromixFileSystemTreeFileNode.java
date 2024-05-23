package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.fs.FileSystemTreeFileNode;
import lombok.Getter;

@Getter
public class CromixFileSystemTreeFileNode extends FileSystemTreeFileNode {
    private final Inode inode;

    public CromixFileSystemTreeFileNode(String name, Inode inode) {
        super(name);
        this.inode = inode;
    }
}
