package au.wildie.m68k.cromixfs.ftar;

import au.wildie.m68k.cromixfs.CromemcoTest;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CromixIMDFtarTest extends CromemcoTest {
    private static final String CLDSDD_IMAGE = "imd/093CR151.IMD";
//    private static final String CLDSDD_IMAGE = "imd/095CR162.IMD";
    private static final String CLDSDD_IMAGE_NEXT = "imd/096CR162.IMD";
    private static final String UNIFORM_IMAGE = "imd/849CR162.IMD";

    private static String EXTRACT_PATH = "/tmp/CromixFtarTest/extract";

    @Test
    public void listUniform() throws IOException {
        list(UNIFORM_IMAGE);
    }

    @Test
    public void listDSDD() throws IOException {
        list(CLDSDD_IMAGE);
    }

    @Test
    public void listDSDDNext() throws IOException {
        list(CLDSDD_IMAGE_NEXT);
    }

    public void list(String resourcePath) throws IOException {
        InputStream imdFile = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertThat(imdFile, notNullValue());

        IMDImage imdImage = new IMDImage(IOUtils.toByteArray(imdFile), System.out);
        assertThat(imdImage, notNullValue());

        CromixFtar ftar = new CromixFtar(new FTarIMDDisk(imdImage, System.out), System.out);
        ftar.list(System.out);
        System.out.println("Done");
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

        CromixFtar ftar = new CromixFtar(new FTarIMDDisk(imdImage, getDummyPrintStream()), getDummyPrintStream());
        ftar.extract(extractDir.getPath(), System.out);
    }
}