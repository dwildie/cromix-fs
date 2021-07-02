package au.wildie.m68k.cromixfs.disk.st;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class CromixStDiskTest {
    private static final String IMD_FILE = "/home/dwildie/m68000/cromix/disk0_848.img";
    private static final String LIST_FILE =    "/home/dwildie/m68000/cromix/disk0_848.list";
    private static final String EXTRACT_PATH = "/home/dwildie/m68000/cromix/disk0_848.dump";

    @Test
    public void list() throws IOException, STDiskException {

        File file = new File(IMD_FILE);
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("Image file %s does not exist", IMD_FILE));
        }

        try (FileOutputStream out = new FileOutputStream(LIST_FILE)) {
            CromixStDisk stDisk = new CromixStDisk(IMD_FILE);
            stDisk.list(new PrintStream(out));
        }
        System.out.println("done");
    }

    @Test
    public void extract() throws IOException, STDiskException {

        File imdFile = new File(IMD_FILE);
        if (!imdFile.exists()) {
            throw new IllegalArgumentException(String.format("Image file %s does not exist", IMD_FILE));
        }

        File extractDir = new File(EXTRACT_PATH);
        if (extractDir.exists()) {
            FileUtils.deleteDirectory(extractDir);
        }
        extractDir.mkdirs();

        CromixStDisk stDisk = new CromixStDisk(IMD_FILE);
        stDisk.extract(EXTRACT_PATH, System.out);
        System.out.println("done");
    }
}