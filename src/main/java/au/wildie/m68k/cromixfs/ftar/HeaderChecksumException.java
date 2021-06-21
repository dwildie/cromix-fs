package au.wildie.m68k.cromixfs.ftar;

import lombok.Getter;

@Getter
public class HeaderChecksumException extends Exception {
    private final FTarHeader thead;
    HeaderChecksumException(String msg, FTarHeader thead) {
        super(msg);
        this.thead = thead;
    }
}
