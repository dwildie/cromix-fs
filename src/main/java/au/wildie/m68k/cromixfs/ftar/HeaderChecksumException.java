package au.wildie.m68k.cromixfs.ftar;

import lombok.Getter;

@Getter
public class HeaderChecksumException extends Exception {
    private final Thead thead;
    HeaderChecksumException(String msg, Thead thead) {
        super(msg);
        this.thead = thead;
    }
}
