package au.wildie.m68k.cromixfs.fs.cdos;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.floppy.cdos.CDOSFloppyDisk;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CDOSFileSystemTest {

    @Test
    public void list() throws IOException {
        File added = new File("/tmp/BLANKIBM_DSK.imd");
//        File added = new File("/tmp/blank.imd");

        try (FileInputStream src = new FileInputStream(added)) {
            IMDImage image = IMDImage.fromStream(src, System.out);
            assertThat(image, notNullValue());

            DiskInterface disk = new CDOSFloppyDisk(image, System.out);
            assertThat(disk, notNullValue());
            //assertThat(CDOSFileSystem.isValid(disk), is(true));

            CDOSFileSystem fs = new CDOSFileSystem(disk);
            assertThat(fs, notNullValue());

            //fs.check(System.out);
            fs.list(System.out);
        }
    }

}