package au.wildie.m68k.cromixfs.fs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.CromixIMDFloppyDisk;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import org.junit.Test;

public class CromixFileSystemTest {
    private static final String CLDSDD_FS_IMAGE = "imd/094CR162.IMD";

    @Test
    public void check() throws IOException {
        InputStream src = getClass().getClassLoader().getResourceAsStream(CLDSDD_FS_IMAGE);
        assertThat(src, notNullValue());

        IMDImage image = IMDImage.fromStream(src, System.out);
        assertThat(image, notNullValue());

        DiskInterface disk = new CromixIMDFloppyDisk(image, System.out);
        assertThat(disk, notNullValue());
        assertThat(CromixFileSystem.isValid(disk), is(true));

        CromixFileSystem fs = new CromixFileSystem(disk);
        assertThat(fs, notNullValue());

        CromixFileSystemStats stats = fs.check(System.out);
    }
}