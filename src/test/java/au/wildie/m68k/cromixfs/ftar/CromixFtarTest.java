package au.wildie.m68k.cromixfs.ftar;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class CromixFtarTest {
    private static String EXTRACT_PATH = "/tmp/CromixFtarTest/extract";

    @Test
    public void list() throws IOException {
        InputStream imdFile = this.getClass().getClassLoader().getResourceAsStream("imd/849CR162.IMD");
        assertThat(imdFile, notNullValue());

        IMDImage imdImage = new IMDImage(IOUtils.toByteArray(imdFile), System.out);
        assertThat(imdImage, notNullValue());

        CromixFtar ftar = new CromixFtar(imdImage);
        ftar.list(System.out);
    }

    @Test
    public void extract() throws IOException {
        InputStream imdFile = this.getClass().getClassLoader().getResourceAsStream("imd/849CR162.IMD");
        assertThat(imdFile, notNullValue());

        IMDImage imdImage = new IMDImage(IOUtils.toByteArray(imdFile), System.out);
        assertThat(imdImage, notNullValue());

        File extractDir = new File(EXTRACT_PATH);
        if (extractDir.exists()) {
            FileUtils.deleteDirectory(extractDir);
        }
        extractDir.mkdirs();

        CromixFtar ftar = new CromixFtar(imdImage);
        ftar.extract(extractDir.getPath(), System.out);
    }
}