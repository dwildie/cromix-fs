package au.wildie.m68k.cromixfs.ftar;

import au.wildie.m68k.cromixfs.CromemcoTest;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.fs.FileSystem;
import au.wildie.m68k.cromixfs.fs.FileSystemOps;
import au.wildie.m68k.cromixfs.fs.FileSystems;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.*;

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

    @Test
    public void listFile() throws IOException {
        InputStream imdFile = new FileInputStream("/mnt/Damian/cloud/synced/retroComputing/dominique/DISK8.IMD");
        assertThat(imdFile, notNullValue());

        IMDImage imdImage = new IMDImage(IOUtils.toByteArray(imdFile), System.out);
        assertThat(imdImage, notNullValue());

        CromixFtar ftar = new CromixFtar(new FTarIMDDisk(imdImage, System.out));
        ftar.list(System.out);
        System.out.println("Done");
    }

    public void list(String resourcePath) throws IOException {
        InputStream imdFile = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertThat(imdFile, notNullValue());

        IMDImage imdImage = new IMDImage(IOUtils.toByteArray(imdFile), System.out);
        assertThat(imdImage, notNullValue());

        CromixFtar ftar = new CromixFtar(new FTarIMDDisk(imdImage, System.out));
        ftar.list(System.out);
        System.out.println("Done");
    }

    @Test
    public void extractCLDSDD() throws IOException {
        InputStream imdFile = this.getClass().getClassLoader().getResourceAsStream(CLDSDD_IMAGE);
        extract(imdFile, new File(EXTRACT_PATH));
    }

    @Test
    public void extractUniform() throws IOException {
        InputStream imdFile = this.getClass().getClassLoader().getResourceAsStream(UNIFORM_IMAGE);
        extract(imdFile, new File(EXTRACT_PATH));
    }

    public void extract(InputStream imdFile, File extractDir) throws IOException {
        IMDImage imdImage = new IMDImage(IOUtils.toByteArray(imdFile), System.out);
        assertThat(imdImage, notNullValue());

        if (extractDir.exists()) {
            FileUtils.deleteDirectory(extractDir);
        }
        extractDir.mkdirs();

        CromixFtar ftar = new CromixFtar(new FTarIMDDisk(imdImage, getDummyPrintStream()));
        ftar.extract(extractDir.getPath(), System.out);
    }

    @Test
    public void create() throws IOException {
        CromixFtar ftar = new CromixFtar("CLDSDD", System.out);
        File file = new File("/tmp/archive.imd");
        if (file.exists()) {
            file.delete();
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream archive = new FileOutputStream(file)) {
            ftar.create("/tmp/CromixFtarTest/extract", archive, System.out);
        }
    }

    @Test
    public void listCreated() throws IOException {
        InputStream src = new FileInputStream("/tmp/archive.imd");
        FileSystemOps fs = FileSystems.getIMDFloppyFileSystem(src, System.out);
        fs.list(System.out);
        System.out.println("done");
    }
}