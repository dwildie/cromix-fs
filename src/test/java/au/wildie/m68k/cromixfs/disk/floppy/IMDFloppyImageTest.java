package au.wildie.m68k.cromixfs.disk.floppy;

import au.wildie.m68k.cromixfs.disk.floppy.cromix.CromixIMDFloppyDisk;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class IMDFloppyImageTest {
    private static final String IMD_FILE = "/tmp/blank.imd";
    private static final String IMAGE_IN_FILE = "/tmp/blank_interleaved.img";
    private static final String IMAGE_NO_FILE = "/tmp/blank_not_interleaved.img";
    @Test
    public void writeImage() throws IOException {
        try (FileInputStream src = new FileInputStream(IMD_FILE)) {
            IMDImage image = IMDImage.fromStream(src, System.out);
            CromixIMDFloppyDisk floppy = new CromixIMDFloppyDisk(image, System.out);
            floppy.writeImage(IMAGE_IN_FILE, true);
            floppy.writeImage(IMAGE_NO_FILE, false);
        }
    }
}