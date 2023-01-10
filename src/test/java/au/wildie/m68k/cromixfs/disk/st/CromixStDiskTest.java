package au.wildie.m68k.cromixfs.disk.st;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class CromixStDiskTest {
//    private static final String IMG_FILE = "/home/dwildie/m68000/cromix/disk0_848.img";
//    private static final String LIST_FILE =    "/home/dwildie/m68000/cromix/disk0_848.list";
//    private static final String EXTRACT_PATH = "/home/dwildie/m68000/cromix/disk0_848.dump";

    private static final String IMG_FILE =     "/home/dwildie/mfm/20220705.D45.C1536.H16.master.img";
//    private static final String LIST_FILE =    "/home/dwildie/ide/idecromix.list";
//    private static final String EXTRACT_PATH = "/home/dwildie/ide/idecromix.dump";

    @Test
    public void list() throws IOException, STDiskException {

        File file = new File(IMG_FILE);
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("Image file %s does not exist", IMG_FILE));
        }

//        String listFile = String.format("%s.%s", FilenameUtils.removeExtension(IMG_FILE), "list");
//        try (FileOutputStream out = new FileOutputStream(listFile)) {
            CromixStDisk stDisk = new CromixStDisk(IMG_FILE, 7);
            stDisk.getPartitionTable().logTable(new PrintStream(System.out));
            stDisk.list(new PrintStream(System.out));
//        }
        System.out.println("done");
    }

    @Test
    public void extract() throws IOException, STDiskException {

        File imdFile = new File(IMG_FILE);
        if (!imdFile.exists()) {
            throw new IllegalArgumentException(String.format("Image file %s does not exist", IMG_FILE));
        }

        String extractPath = String.format("%s.%s", FilenameUtils.removeExtension(IMG_FILE), "dump");

        File extractDir = new File(extractPath);
        if (extractDir.exists()) {
            FileUtils.deleteDirectory(extractDir);
        }
        extractDir.mkdirs();

        CromixStDisk stDisk = new CromixStDisk(IMG_FILE, 0);
        stDisk.extract(extractPath, System.out);
        System.out.println("done");
    }

    @Test
    public void check() throws IOException, STDiskException {

        File file = new File(IMG_FILE);
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("Image file %s does not exist", IMG_FILE));
        }

        CromixStDisk stDisk = new CromixStDisk(IMG_FILE, null);
        stDisk.check(System.out);

        System.out.println("done");
    }
}