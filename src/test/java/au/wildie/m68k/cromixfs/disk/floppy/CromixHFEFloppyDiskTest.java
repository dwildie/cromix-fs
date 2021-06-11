package au.wildie.m68k.cromixfs.disk.floppy;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import au.wildie.m68k.cromixfs.fs.FileSystem;
import au.wildie.m68k.cromixfs.fs.FileSystems;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class CromixHFEFloppyDiskTest {
    private static final String HFE_TEST_FILE = "hfe/848CR162.hfe";
    private static final String EXTRACT_PATH = "/tmp/extract";

    @Test
    public void list() throws IOException {
        InputStream hfeFile = this.getClass().getClassLoader().getResourceAsStream(HFE_TEST_FILE);
        assertThat(hfeFile, notNullValue());

        FileSystem fs = FileSystems.getHFEFloppyFileSystem(hfeFile, System.out);
        fs.list(System.out);
        System.out.println("done");
    }

    @Test
    public void extract() throws IOException {
        InputStream hfeFile = this.getClass().getClassLoader().getResourceAsStream(HFE_TEST_FILE);
        assertThat(hfeFile, notNullValue());

        File extractDir = new File(EXTRACT_PATH);
        if (extractDir.exists()) {
            FileUtils.deleteDirectory(extractDir);
        }
        extractDir.mkdirs();

        FileSystem fs = FileSystems.getHFEFloppyFileSystem(hfeFile, System.out);
        fs.extract(EXTRACT_PATH, System.out);
        System.out.println("done");
    }
}