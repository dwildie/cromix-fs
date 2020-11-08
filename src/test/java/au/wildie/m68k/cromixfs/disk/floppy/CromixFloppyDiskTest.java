package au.wildie.m68k.cromixfs.disk.floppy;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class CromixFloppyDiskTest {
    private static final String SCRATCH_FILE = "/tmp/SCRATCH.IMD";
    private static final String EXTRACT_PATH = "/tmp/extract";

    @Test
    public void list() throws IOException {

        File file = new File(SCRATCH_FILE);
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("Image file %s does not exist", SCRATCH_FILE));
        }

        CromixFloppyDisk floppy = new CromixFloppyDisk(SCRATCH_FILE);
        floppy.list();
        System.out.println("done");
    }

    @Test
    public void extract() throws IOException {

        File imdFile = new File(SCRATCH_FILE);
        if (!imdFile.exists()) {
            throw new IllegalArgumentException(String.format("Image file %s does not exist", SCRATCH_FILE));
        }

        File extractDir = new File(EXTRACT_PATH);
        if (extractDir.exists()) {
            FileUtils.deleteDirectory(extractDir);
        }
        extractDir.mkdirs();

        CromixFloppyDisk floppy = new CromixFloppyDisk(SCRATCH_FILE);
        floppy.extract(EXTRACT_PATH);
        System.out.println("done");
    }
}