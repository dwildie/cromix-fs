package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.fs.FileSystemTreeDirectoryNode;
import au.wildie.m68k.cromixfs.fs.FileSystemTreeNode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CromixFileSystemTreeDirectoryNode extends FileSystemTreeDirectoryNode {
    private final Inode inode;
    private final List<FileSystemTreeNode> children = new ArrayList<>();

    public CromixFileSystemTreeDirectoryNode(String name, Inode inode) {
        super(name);
        this.inode = inode;
    }

    public void add(FileSystemTreeNode node) {
        children.add(node);
    }
}
