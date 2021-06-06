package au.wildie.m68k.cromixfs.disk.floppy;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import au.wildie.m68k.cromixfs.disk.vfd.InvalidVFDImageException;
import au.wildie.m68k.cromixfs.fs.FileSystem;
import au.wildie.m68k.cromixfs.fs.FileSystems;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class CromixVFDFloppyDiskTest {
    private static final String VFD_TEST_FILE = "vfd/848CR162.vfd";
    private static final String EXTRACT_PATH = "/tmp/extract";

    @Test
    public void list() throws IOException, InvalidVFDImageException {
        InputStream vfdFile = this.getClass().getClassLoader().getResourceAsStream(VFD_TEST_FILE);
        assertThat(vfdFile, notNullValue());

        FileSystem fs = FileSystems.getVFDFloppyFileSystem(vfdFile, System.out);
        fs.list(System.out);
        System.out.println("done");
    }

    @Test
    public void extract() throws IOException, InvalidVFDImageException {
        InputStream vfdFile = this.getClass().getClassLoader().getResourceAsStream(VFD_TEST_FILE);
        assertThat(vfdFile, notNullValue());

        File extractDir = new File(EXTRACT_PATH);
        if (extractDir.exists()) {
            FileUtils.deleteDirectory(extractDir);
        }
        extractDir.mkdirs();

        FileSystem fs = FileSystems.getVFDFloppyFileSystem(vfdFile, System.out);
        fs.extract(EXTRACT_PATH, System.out);
        System.out.println("done");
    }
}