package au.wildie.m68k.cromixfs.fs.cromix;

import lombok.Getter;

@Getter
public class BlockUnavailableException extends RuntimeException {
    private final int blockNumber;

    public BlockUnavailableException(int blockNumber, Throwable t) {
        super(String.format("Block %d is unavailable", blockNumber), t);
        this.blockNumber = blockNumber;
    }
}
