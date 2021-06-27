package au.wildie.m68k.cromixfs.fs.cromix;

public class InodeException extends RuntimeException {

    public InodeException(int inodeNumber) {
        super(String.format("Inode %d was not found", inodeNumber));
    }
    public InodeException(String msg) {
        super(msg);
    }
}
