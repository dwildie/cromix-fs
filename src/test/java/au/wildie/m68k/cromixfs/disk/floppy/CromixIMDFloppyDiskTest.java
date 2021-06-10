package au.wildie.m68k.cromixfs.disk.floppy;

import au.wildie.m68k.cromixfs.fs.FileSystem;
import au.wildie.m68k.cromixfs.fs.FileSystems;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class CromixIMDFloppyDiskTest {
    //private static final String SCRATCH_FILE = "/home/dwildie/cromemcos/Cromemco_CRO-PLUS-CS_Release_5_Serial_10018_68020_Cromix-Plus/Cromemco_KERMIT-S_Release_1_Serial_10204_Kermit_Communications_Software_MOUNT_FORMAT.imd";

    private static final String TEST_IMAGE =
//            "/home/dwildie/cromemcos/HowardHarte/cromemco_imd/FLASHRAM_and_SDI2_Software_Development_05-11-86_Most_Files_Lost_07-19-90.imd"
//            "/home/dwildie/cromemcos/HowardHarte/Cromemco_CRO-PLUS-CS_Release_5_Serial_10018_68020_Cromix-Plus/Cromemco_KERMIT-S_Release_1_Serial_10204_Kermit_Communications_Software_MOUNT_FORMAT.imd"
            //"/home/dwildie/cromemcos/HowardHarte/Cromemco_CRO-PLUS-CS_Release_5_Serial_10018_68020_Cromix-Plus/Cromemco_CRO-PLUS-CS_Release_5_Serial_10018_68020_Cromix-Plus_Disk_1_of_9_BOOTABLE.imd"
            //"/home/dwildie/cromemcos/HowardHarte/Cromemco_CS1-D5E/DBASE_FRIDAY.imd"
            //"/tmp/mb/848CR162.IMD"
            "/tmp/mb/326FLASH.IMD"
            ;
    private static final String EXTRACT_PATH = "/tmp/extract";

    @Test
    public void dummy() {
    }

    @Test
    public void scan() throws IOException {
        new FileScan("/home/dwildie/cromemcos/HowardHarte").scan();
    }

    @Test
    public void writeImage() throws IOException {
        File file = new File(TEST_IMAGE);
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("Image file %s does not exist", TEST_IMAGE));
        }

        String imageFileName = String.format("%s.%s", FilenameUtils.removeExtension(TEST_IMAGE), "img");
        FileSystem fs = (FileSystem)FileSystems.getIMDFloppyFileSystem(TEST_IMAGE, System.out);
        fs.getDisk().writeImage(imageFileName, true);
        System.out.println("done");
    }

    @Test
    public void list() throws IOException {

        File file = new File(TEST_IMAGE);
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("Image file %s does not exist", TEST_IMAGE));
        }

        FileSystem fs = (FileSystem)FileSystems.getIMDFloppyFileSystem(TEST_IMAGE, System.out);
        fs.list(System.out);
        System.out.println("done");
    }

    @Test
    public void extract() throws IOException {

        File imdFile = new File(TEST_IMAGE);
        if (!imdFile.exists()) {
            throw new IllegalArgumentException(String.format("Image file %s does not exist", TEST_IMAGE));
        }

        File extractDir = new File(EXTRACT_PATH);
        if (extractDir.exists()) {
            FileUtils.deleteDirectory(extractDir);
        }
        extractDir.mkdirs();

        FileSystem fs = (FileSystem)FileSystems.getIMDFloppyFileSystem(TEST_IMAGE, System.out);
        fs.extract(EXTRACT_PATH, System.out);
        System.out.println("done");
    }
}