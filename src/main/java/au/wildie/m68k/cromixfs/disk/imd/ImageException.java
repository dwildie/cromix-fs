package au.wildie.m68k.cromixfs.disk.imd;

import lombok.Getter;

public class ImageException extends RuntimeException {
    public static String CODE_END_OF_DISK = "endOfDisk";
    public static String CODE_ERROR = "error";

    @Getter
    private final String code;

    public ImageException(String code, String msg, Throwable t) {
        super(msg, t);
        this.code = code;

    }
    public ImageException(String code, String msg) {
        super(msg);
        this.code = code;
    }
}
