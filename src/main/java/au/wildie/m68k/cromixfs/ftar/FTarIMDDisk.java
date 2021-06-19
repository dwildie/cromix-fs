package au.wildie.m68k.cromixfs.ftar;

import au.wildie.m68k.cromixfs.disk.imd.IMDImage;

import java.io.PrintStream;

public class FTarIMDDisk extends FTarDisk {
    public FTarIMDDisk(IMDImage image, PrintStream out) {
        super(image,
              FTarTrackInfo.from(image.getTrack(0,0)),
              FTarTrackInfo.from(image.getTrack(0, 1)),
              out);
    }

    @Override
    public Integer getSectorErrorCount() {
        return ((IMDImage)image).getSectorErrorCount();
    }
}
