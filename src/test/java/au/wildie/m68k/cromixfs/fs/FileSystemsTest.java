package au.wildie.m68k.cromixfs.fs;

import au.wildie.m68k.cromixfs.CromemcoTest;
import au.wildie.m68k.cromixfs.ftar.CromixFtar;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileSystemsTest extends CromemcoTest {
    private static final String CLDSDD_FTAR_IMAGE = "imd/093CR151.IMD";
    private static final String CLDSDD_FS_IMAGE = "imd/094CR162.IMD";
    private static final String UNIFORM_FS_IMAGE = "imd/904C3140.IMD";

    @Test
    public void getIMDFloppyFileSystem_ftar() throws IOException {
        InputStream src = getClass().getClassLoader().getResourceAsStream(CLDSDD_FTAR_IMAGE);
        FileSystemOps fs = FileSystems.getIMDFloppyFileSystem(src, getDummyPrintStream());
        assertThat(fs, notNullValue());
        assertThat(fs, instanceOf(CromixFtar.class));
    }

    @Test
    public void getIMDFloppyFileSystem_fs() throws IOException {
        InputStream src = getClass().getClassLoader().getResourceAsStream(CLDSDD_FS_IMAGE);
        FileSystemOps fs = FileSystems.getIMDFloppyFileSystem(src, getDummyPrintStream());
        assertThat(fs, notNullValue());
        assertThat(fs, instanceOf(CromixFileSystem.class));
    }

    @Test
    public void getIMDFloppyFileSystem_uniform() throws IOException {
        InputStream src = getClass().getClassLoader().getResourceAsStream(UNIFORM_FS_IMAGE);
        FileSystemOps fs = FileSystems.getIMDFloppyFileSystem(src, getDummyPrintStream());
        assertThat(fs, notNullValue());
        assertThat(fs, instanceOf(CromixFileSystem.class));
    }

}