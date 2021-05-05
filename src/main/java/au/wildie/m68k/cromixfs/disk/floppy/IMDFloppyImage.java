package au.wildie.m68k.cromixfs.disk.floppy;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.disk.imd.Track;
import lombok.Getter;

import java.io.*;

public abstract class IMDFloppyImage implements DiskInterface {
    protected final IMDImage image;
    @Getter private final String formatLabel;
    private final PrintStream out;

    public IMDFloppyImage(IMDImage image, String formatLabel, PrintStream out) {
        this.image = image;
        this.formatLabel = formatLabel;
        this.out = out;
        out.format("Disk format: %s\n\n", formatLabel);
    }
    @Override
    public void writeImage(String fileName, boolean interleaved) throws IOException {
        out.printf("Writing image to %s\n\n", fileName);

        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }

        try (OutputStream out = new FileOutputStream(file)) {
            for (Track track : image.getTracks()) {
                if (track.getCylinder() == 0 && track.getHead() == 0) {
                    // Write track 0
                    for (int i = 0; i < track.getSectorCount(); i++) {
                        out.write(track.getSector(i + 1).getData());
                    }
                } else {
                    for (int i = 0; i < track.getSectorCount(); i++) {
                        int sectorNumber = interleaved ? i + 1 : getInterleave()[i] + 1;
                        out.write(track.getSector(sectorNumber).getData());
                    }
                }
            }
            out.flush();
        }
    }

    public abstract byte[] getInterleave();

    @Override
    public Integer getTrackCount() {
        return image.getTrackCount();
    }

    @Override
    public Integer getTrackCount(int head) {
        return image.getTrackCount(head);
    }

    @Override
    public Integer getSectorErrorCount() {
        return image.getSectorErrorCount();
    }
}
