package au.wildie.m68k.cromixfs.disk.floppy;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.vfd.VFDImage;
import lombok.Getter;
import org.apache.commons.io.FileUtils;

public abstract class VFDFloppyImage implements DiskInterface {
    protected final VFDImage image;
    @Getter private final String formatLabel;
    private final PrintStream out;

    public VFDFloppyImage(VFDImage image, String formatLabel, PrintStream out) {
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

        FileUtils.writeByteArrayToFile(file, image.toBytes());
    }

    public abstract byte[] getInterleave();

    @Override
    public Integer getTrackCount() {
        return image.getInfo().getCylinders() * image.getInfo().getHeads();
    }

    @Override
    public Integer getTrackCount(int head) {
        return image.getInfo().getCylinders();
    }

    @Override
    public Integer getSectorErrorCount() {
        return 0;
    }
}
