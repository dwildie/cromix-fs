package au.wildie.m68k.cromixfs.disk.imd;

public class ImageException extends RuntimeException {
    public ImageException(String msg, Throwable t) {
        super(msg, t);
    }
    public ImageException(String msg) {
        super(msg);
    }
}
