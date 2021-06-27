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

        CromixFileSystemStats expectedStats = new CromixFileSystemStats(new BlockStats(2307), new InodeStats(508));
        expectedStats.getInodeStats().setFileInodes(507);

        assertStats(stats, expectedStats);

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

            assertStats(stats, expectedStats);
        }
    }

    @Test
    public void list() throws IOException {
        File added = new File("/tmp/added.imd");
//        File added = new File("/tmp/blank.imd");

        try (FileInputStream src = new FileInputStream(added)) {
            IMDImage image = IMDImage.fromStream(src, System.out);
            assertThat(image, notNullValue());

            DiskInterface disk = new CromixIMDFloppyDisk(image, System.out);
            assertThat(disk, notNullValue());
            assertThat(CromixFileSystem.isValid(disk), is(true));

            CromixFileSystem fs = new CromixFileSystem(disk);
            assertThat(fs, notNullValue());

            fs.check(System.out);
            fs.list(System.out);
       }
    }

    @Test
    public void addDirectory() throws IOException {

        DiskInterface disk = CromixIMDFloppyDisk.create("CLDSDD", System.out);
        assertThat(disk, notNullValue());

        CromixFileSystem fs = CromixFileSystem.initialise(disk);
        assertThat(fs, notNullValue());
        assertThat(CromixFileSystem.isValid(disk), is(true));

        CromixFileSystemStats stats = fs.check(System.out);
        assertThat(stats, notNullValue());

        CromixFileSystemStats expectedStats = new CromixFileSystemStats(new BlockStats(2307), new InodeStats(508));
        expectedStats.getInodeStats().setDirectoryInodes(1);
        expectedStats.getInodeStats().setFreeInodes(507);
        expectedStats.getBlockStats().setOnFreeList(2306);
        expectedStats.getBlockStats().setDirectoryBlocks(1);
        expectedStats.getBlockStats().setFreeListBlocks(2306);
        expectedStats.getBlockStats().setDirectories(1);

        assertStats(stats, expectedStats);

        File file = new File("/tmp/blank");
        assertThat(file, notNullValue());
        assertThat(file.exists(), is(true));
        assertThat(file.isDirectory(), is(true));
        fs.addDirectory(file);

        fs.list(System.out);

        stats = fs.check(System.out);
        //assertThat(stats, notNullValue());

        expectedStats.getInodeStats().setDirectoryInodes(5);
        expectedStats.getInodeStats().setFileInodes(1);
        expectedStats.getInodeStats().setFreeInodes(502);
        expectedStats.getInodeStats().setFreeInodeListUsed(5);
        expectedStats.getInodeStats().setFreeInodeListAvailable(75);
        expectedStats.getBlockStats().setDirectoryBlocks(2);
        expectedStats.getBlockStats().setFileBlocks(1);
        expectedStats.getBlockStats().setOnFreeList(2304);
        expectedStats.getBlockStats().setFreeListBlocks(2304);
        expectedStats.getBlockStats().setDirectories(5);
        expectedStats.getBlockStats().setFiles(1);

//        assertStats(stats, expectedStats);

        File added = new File("/tmp/added.imd");
        if (added.exists()) {
            added.delete();
        }
        try (FileOutputStream archive = new FileOutputStream(added)) {
            fs.persist(archive);
        }

        try (FileInputStream src = new FileInputStream(added)) {
            IMDImage image = IMDImage.fromStream(src, System.out);
            assertThat(image, notNullValue());

            disk = new CromixIMDFloppyDisk(image, System.out);
            assertThat(disk, notNullValue());
            assertThat(CromixFileSystem.isValid(disk), is(true));

            fs = new CromixFileSystem(disk);
            assertThat(fs, notNullValue());

            stats = fs.check(System.out);
            assertThat(stats, notNullValue());

            fs.list(System.out);

            assertStats(stats, expectedStats);
        }
    }

    private void assertStats(CromixFileSystemStats stats, CromixFileSystemStats expectedStats) {
        assertThat(stats.getInodeStats().getDirectoryInodes(), is(expectedStats.getInodeStats().getDirectoryInodes()));
        assertThat(stats.getInodeStats().getFileInodes(), is(expectedStats.getInodeStats().getFileInodes()));
        assertThat(stats.getInodeStats().getDeviceInodes(), is(expectedStats.getInodeStats().getDeviceInodes()));
        assertThat(stats.getInodeStats().getPipeInodes(), is(expectedStats.getInodeStats().getPipeInodes()));
        assertThat(stats.getInodeStats().getSharedTextInodes(), is(expectedStats.getInodeStats().getSharedTextInodes()));
        assertThat(stats.getInodeStats().getUsedInodes(), is(expectedStats.getInodeStats().getUsedInodes()));
        assertThat(stats.getInodeStats().getErrorInodes(), is(expectedStats.getInodeStats().getErrorInodes()));
        assertThat(stats.getInodeStats().getFreeInodes(), is(expectedStats.getInodeStats().getFreeInodes()));
        assertThat(stats.getInodeStats().getTotalInodes(), is(expectedStats.getInodeStats().getTotalInodes()));
        assertThat(stats.getInodeStats().getExpectedInodes(), is(expectedStats.getInodeStats().getExpectedInodes()));
        assertThat(stats.getInodeStats().getFreeInodeListUsed(), is(expectedStats.getInodeStats().getFreeInodeListUsed()));
        assertThat(stats.getInodeStats().getFreeInodeListAvailable(), is(expectedStats.getInodeStats().getFreeInodeListAvailable()));

        assertThat(stats.getBlockStats().getDirectoryBlocks(), is(expectedStats.getBlockStats().getDirectoryBlocks()));
        assertThat(stats.getBlockStats().getFileBlocks(), is(expectedStats.getBlockStats().getFileBlocks()));
        assertThat(stats.getBlockStats().getOnFreeList(), is(expectedStats.getBlockStats().getOnFreeList()));
        assertThat(stats.getBlockStats().getOrphanedBlocks(), is(expectedStats.getBlockStats().getOrphanedBlocks()));
        assertThat(stats.getBlockStats().getTotalBlocks(), is(expectedStats.getBlockStats().getTotalBlocks()));
        assertThat(stats.getBlockStats().getAvailableBlocks(), is(expectedStats.getBlockStats().getAvailableBlocks()));
        assertThat(stats.getBlockStats().getDuplicateBlocks(), is(expectedStats.getBlockStats().getDuplicateBlocks()));
        assertThat(stats.getBlockStats().getFreeListBlocks(), is(expectedStats.getBlockStats().getFreeListBlocks()));
        assertThat(stats.getBlockStats().getFiles(), is(expectedStats.getBlockStats().getFiles()));
        assertThat(stats.getBlockStats().getDirectories(), is(expectedStats.getBlockStats().getDirectories()));
        assertThat(stats.getBlockStats().getDevices(), is(expectedStats.getBlockStats().getDevices()));
    }
}