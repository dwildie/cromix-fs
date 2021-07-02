package au.wildie.m68k.cromixfs.fs.cromix;

public class FreeBlockListException extends RuntimeException {
    public FreeBlockListException(String msg) {
        super(msg);
    }
    public FreeBlockListException(String msg, Throwable t) {
        super(msg, t);
    }
}
