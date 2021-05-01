package au.wildie.m68k.cromixfs.disk.st;

public class STDiskException extends Exception {
    public STDiskException(String msg) {
        super(msg);
    }

    public STDiskException(String msg, Throwable t) {
        super(msg, t);
    }
}
