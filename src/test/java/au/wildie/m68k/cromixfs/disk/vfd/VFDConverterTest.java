package au.wildie.m68k.cromixfs.disk.vfd;

import static org.apache.commons.io.FilenameUtils.getName;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class VFDConverterTest {
    private static String TEST_IMD_FILE = "imd/849CR162.IMD";

    @Test
    public void ImdToVfd() throws IOException {
        InputStream imdFile = this.getClass().getClassLoader().getResourceAsStream(TEST_IMD_FILE);
        assertThat(imdFile, notNullValue());

        try {
            byte[] vfd = VFDConverter.imdToVfd(imdFile, true);
            File vfdFile = Paths.get(System.getProperty("java.io.tmpdir"), getName(removeExtension(TEST_IMD_FILE) + ".vfd")).toFile();
            FileUtils.writeByteArrayToFile(vfdFile, vfd);
            System.out.printf("VFD File path: %s\n", vfdFile.getPath());
        } catch (VFDErrorsException e) {
            System.out.println(e.getMessage());
        }
    }
}