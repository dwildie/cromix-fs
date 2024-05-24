package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.fs.FileSystemDirectoryNode;
import au.wildie.m68k.cromixfs.fs.FileSystemNode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CromixFileSystemDirectoryNode extends FileSystemDirectoryNode implements CromixFileSystemNode {
    private final Inode inode;
    private final List<FileSystemNode> children = new ArrayList<>();

    public CromixFileSystemDirectoryNode(String name, Inode inode) {
        super(name);
        this.inode = inode;
    }

    public void add(FileSystemNode node) {
        children.add(node);
    }
}
