package au.wildie.m68k.cromixfs.disk;

public class SectorInvalidException extends Exception {
    public SectorInvalidException(int cylinder, int head, int sectorNumber) {
        super(String.format("Invalid sector: cylinder=%d, head=%d, number=%d", cylinder, head, sectorNumber));
    }
}
