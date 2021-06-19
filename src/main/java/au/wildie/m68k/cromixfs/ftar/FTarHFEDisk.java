package au.wildie.m68k.cromixfs.ftar;

import au.wildie.m68k.cromixfs.disk.hxc.HFEImage;

import java.io.PrintStream;

public class FTarHFEDisk extends FTarDisk {
    public FTarHFEDisk(HFEImage image, PrintStream out) {
        super(image,
              FTarTrackInfo.from(image.getTrack(0,0)),
              FTarTrackInfo.from(image.getTrack(0, 1)),
              out);
    }

    public Integer getSectorErrorCount() {
        return -1;
    }
}
