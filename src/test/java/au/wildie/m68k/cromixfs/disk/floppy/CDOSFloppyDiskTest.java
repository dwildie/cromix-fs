package au.wildie.m68k.cromixfs.disk.floppy;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.fs.FileSystem;
import au.wildie.m68k.cromixfs.fs.FileSystemOps;
import au.wildie.m68k.cromixfs.fs.FileSystems;
import au.wildie.m68k.cromixfs.fs.cdos.CDOSFileSystem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;



public class CDOSFloppyDiskTest {
    private static final String TEST_IMAGE =
            "/home/dwildie/cromemcos/m/cromemco/code/disks/005C0254.IMD"
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

        FileSystem fs = (FileSystem)FileSystems.getIMDFloppyFileSystem(TEST_IMAGE, System.out);
        assertThat(fs, instanceOf(CDOSFileSystem.class));

        File out = Paths.get(System.getProperty("java.io.tmpdir"), String.format("%s.%s", FilenameUtils.removeExtension(TEST_IMAGE), "img")).toFile();
        ((DiskInterface)fs.getDisk()).writeImage(out, true);

        System.out.println("done");
    }

    @Test
    public void list() throws IOException {

        File file = new File(TEST_IMAGE);
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("Image file %s does not exist", TEST_IMAGE));
        }

        FileSystemOps fs = FileSystems.getIMDFloppyFileSystem(TEST_IMAGE, System.out);
        assertThat(fs, instanceOf(CDOSFileSystem.class));

        fs.list(System.out);
        System.out.print("\ndone\n");
    }

//    @Test
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

        FileSystemOps fs = FileSystems.getIMDFloppyFileSystem(TEST_IMAGE, System.out);
        fs.extract(EXTRACT_PATH, System.out);
        System.out.println("done");
    }
}