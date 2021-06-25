package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.CromixIMDFloppyDisk;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import org.junit.Test;

import java.io.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CromixFileSystemTest {
    private static final String CLDSDD_FS_IMAGE = "imd/094CR162.IMD";
    private static final String BLANK_CLDSDDST = "imd/CLDSDDST.IMD";

    @Test
    public void checkCLDSDD() throws IOException {
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
        assertThat(stats, notNullValue());
    }

    @Test
    public void checkBlankCLDSDD() throws IOException {
        InputStream src = getClass().getClassLoader().getResourceAsStream(BLANK_CLDSDDST);
        assertThat(src, notNullValue());

        IMDImage image = IMDImage.fromStream(src, System.out);
        assertThat(image, notNullValue());

        DiskInterface disk = new CromixIMDFloppyDisk(image, System.out);
        assertThat(disk, notNullValue());
        assertThat(CromixFileSystem.isValid(disk), is(true));

        CromixFileSystem fs = new CromixFileSystem(disk);
        assertThat(fs, notNullValue());

        CromixFileSystemStats stats = fs.check(System.out);
        assertThat(stats, notNullValue());

        fs.list(System.out);
    }

    @Test
    public void create() throws IOException {
        DiskInterface disk = CromixIMDFloppyDisk.create("CLDSDD", System.out);
        assertThat(disk, notNullValue());

        CromixFileSystem fs = CromixFileSystem.initialise(disk);
        assertThat(fs, notNullValue());
        assertThat(CromixFileSystem.isValid(disk), is(true));

        CromixFileSystemStats stats = fs.check(System.out);
        assertThat(stats, notNullValue());

        int expectedFreeInodes = 507;
        int expectedTotalInodes = 508;
        int expectedBlocks = 2307;

        assertStats(stats, expectedFreeInodes, expectedTotalInodes, expectedBlocks);

        File file = new File("/tmp/created.imd");
        if (file.exists()) {
            file.delete();
        }
        try (FileOutputStream archive = new FileOutputStream(file)) {
            fs.persist(archive);
        }

        try (FileInputStream src = new FileInputStream(file)) {
            IMDImage image = IMDImage.fromStream(src, System.out);
            assertThat(image, notNullValue());

            disk = new CromixIMDFloppyDisk(image, System.out);
            assertThat(disk, notNullValue());
            assertThat(CromixFileSystem.isValid(disk), is(true));

            fs = new CromixFileSystem(disk);
            assertThat(fs, notNullValue());

            stats = fs.check(System.out);
            assertThat(stats, notNullValue());

            assertStats(stats, expectedFreeInodes, expectedTotalInodes, expectedBlocks);
        }
    }

    private void assertStats(CromixFileSystemStats stats, int expectedFreeInodes, int expectedTotalInodes, int expectedBlocks) {
        assertThat(stats.getInodeStats().getDirectoryInodes(), is(1));
        assertThat(stats.getInodeStats().getFileInodes(), is(0));
        assertThat(stats.getInodeStats().getDeviceInodes(), is(0));
        assertThat(stats.getInodeStats().getPipeInodes(), is(0));
        assertThat(stats.getInodeStats().getSharedTextInodes(), is(0));
        assertThat(stats.getInodeStats().getUsedInodes(), is(1));
        assertThat(stats.getInodeStats().getErrorInodes(), is(0));
        assertThat(stats.getInodeStats().getFreeInodes(), is(expectedFreeInodes));
        assertThat(stats.getInodeStats().getTotalInodes(), is(expectedTotalInodes));
        assertThat(stats.getInodeStats().getExpectedInodes(), is(expectedTotalInodes));
        assertThat(stats.getInodeStats().getFreeInodeListUsed(), is(0));
        assertThat(stats.getInodeStats().getFreeInodeListAvailable(), is(0));

        assertThat(stats.getBlockStats().getDirectoryBlocks(), is(0));
        assertThat(stats.getBlockStats().getFileBlocks(), is(0));
        assertThat(stats.getBlockStats().getOnFreeList(), is(expectedBlocks));
        assertThat(stats.getBlockStats().getOrphanedBlock(), is(0));
        assertThat(stats.getBlockStats().getTotalBlocks(), is(expectedBlocks));
        assertThat(stats.getBlockStats().getAvailableBlocks(), is(expectedBlocks));
        assertThat(stats.getBlockStats().getDuplicateBlocks(), is(0));
        assertThat(stats.getBlockStats().getFreeListBlocks(), is(expectedBlocks));
        assertThat(stats.getBlockStats().getFiles(), is(0));
        assertThat(stats.getBlockStats().getDirectories(), is(1));
        assertThat(stats.getBlockStats().getDevices(), is(0));
    }
}