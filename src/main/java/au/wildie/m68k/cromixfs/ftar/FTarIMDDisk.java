package au.wildie.m68k.cromixfs.ftar;

import au.wildie.m68k.cromixfs.disk.floppy.cromix.CromixFloppyInfo;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.disk.imd.IMDSector;

import java.io.PrintStream;
import java.util.Arrays;

public class FTarIMDDisk extends FTarDisk {
    public FTarIMDDisk(IMDImage image, PrintStream out) {
        super(image,
              FTarTrackInfo.from(image.getTrack(0,0)),
              FTarTrackInfo.from(image.getTrack(0, 1)),
              out);
    }

    public static FTarIMDDisk create(String formatLabel, PrintStream out) {
        IMDImage image = new IMDImage(new int[] {0, 3}, CromixFloppyInfo.LARGE_DOUBLE_DENSITY);
        IMDSector zero = image.getSector(0, 0, 1);
        Arrays.fill(zero.getData(), (byte)0);
        CromixFloppyInfo.setFormatLabel(formatLabel, zero.getData());
        return new FTarIMDDisk(image, out);
    }

    @Override
    public Integer getSectorErrorCount() {
        return ((IMDImage)image).getSectorErrorCount();
    }
}
