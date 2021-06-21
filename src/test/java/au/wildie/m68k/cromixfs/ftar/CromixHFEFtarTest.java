package au.wildie.m68k.cromixfs.ftar;

import au.wildie.m68k.cromixfs.CromemcoTest;
import au.wildie.m68k.cromixfs.disk.hxc.HFEImage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CromixHFEFtarTest extends CromemcoTest {
    private static final String CLDSDD_IMAGE = "hfe/093CR151_IMD.hfe";
//    private static final String CLDSDD_IMAGE = "hfe/095CR162_IMD.hfe";
    private static final String CLDSDD_IMAGE_NEXT = "hfe/096CR162_IMD.hfe";
    private static final String UNIFORM_IMAGE = "hfe/849CR162_IMD.hfe";

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

        HFEImage image = new HFEImage(IOUtils.toByteArray(imdFile));
        assertThat(image, notNullValue());

        CromixFtar ftar = new CromixFtar(new FTarHFEDisk(image, System.out));
        ftar.list(System.out);
        System.out.println("Done");
    }

    @Test
    public void extract() throws IOException {
        InputStream imdFile = this.getClass().getClassLoader().getResourceAsStream("imd/849CR162.IMD");
        assertThat(imdFile, notNullValue());

        HFEImage image = new HFEImage(IOUtils.toByteArray(imdFile));
        assertThat(image, notNullValue());

        File extractDir = new File(EXTRACT_PATH);
        if (extractDir.exists()) {
            FileUtils.deleteDirectory(extractDir);
        }
        extractDir.mkdirs();

        CromixFtar ftar = new CromixFtar(new FTarHFEDisk(image, getDummyPrintStream()));
        ftar.extract(extractDir.getPath(), System.out);
    }
}