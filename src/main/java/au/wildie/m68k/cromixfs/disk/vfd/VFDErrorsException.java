package au.wildie.m68k.cromixfs.disk.vfd;

import lombok.Getter;

@Getter
public class VFDErrorsException extends Exception {
    private final Integer count;

    public VFDErrorsException(String msg, Integer count) {
        super(msg);
        this.count = count;
    }
}
