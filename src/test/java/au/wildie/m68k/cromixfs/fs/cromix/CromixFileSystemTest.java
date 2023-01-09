package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.CromixIMDFloppyDisk;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.fs.FileSystemOps;
import au.wildie.m68k.cromixfs.fs.FileSystems;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Random;

import static org.hamcrest.CoreMatchers.*;
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
        DiskInterface disk = CromixIMDFloppyDisk.createLarge(System.out);
        assertThat(disk, notNullValue());

        CromixFileSystem fs = CromixFileSystem.initialise(disk);
        assertThat(fs, notNullValue());
        assertThat(CromixFileSystem.isValid(disk), is(true));

        CromixFileSystemStats stats = fs.check(System.out);
        assertThat(stats, notNullValue());

        CromixFileSystemStats expectedStats = new CromixFileSystemStats(0, 0, new BlockStats(2307), new InodeStats(508));
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
        File added = new File("/tmp/BLANKIBM_DSK.imd");
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
    public void createAndAppendLarge() throws IOException {
        createAndAppend(CromixIMDFloppyDisk.createLarge(System.out));
    }

    @Test
    public void createAndAppendSmall() throws IOException {
        createAndAppend(CromixIMDFloppyDisk.createSmall(System.out));
    }

    private void createAndAppend(DiskInterface disk1) throws IOException {
        assertThat(disk1, notNullValue());

        CromixFileSystem fs1 = CromixFileSystem.initialise(disk1);
        assertThat(fs1, notNullValue());
        assertThat(CromixFileSystem.isValid(disk1), is(true));

        CromixFileSystemStats stats = fs1.check(System.out);
        assertThat(stats, notNullValue());

        File dir1 = Files.createTempDirectory(Paths.get("/tmp"), "test1-").toFile();
        assertThat(dir1, notNullValue());
        assertThat(dir1.exists(), is(true));
        assertThat(dir1.isDirectory(), is(true));
        String file1 = createTextFile(dir1.getAbsolutePath(), "file-1");
        String file2 = createTextFile(dir1.getAbsolutePath(), "file-2");

        fs1.addDirectory(dir1, System.out);

        fs1.list(System.out);

        fs1.check(System.out);

        File image1 = Files.createTempFile("cromix-1-", ".imd").toFile();
        try (FileOutputStream imd = new FileOutputStream(image1)) {
            fs1.persist(imd);
        }

        FileSystemOps fso2 = FileSystems.getIMDFloppyFileSystem(image1.getPath(), System.out);
        assertThat(fso2, instanceOf(CromixFileSystem.class));

        CromixFileSystem fs2 = (CromixFileSystem)fso2;
        fs2.check(System.out);

        File dir2 = Files.createTempDirectory(Paths.get("/tmp"), "test2-").toFile();
        assertThat(dir2, notNullValue());
        assertThat(dir2.exists(), is(true));
        assertThat(dir2.isDirectory(), is(true));
        String file3 = createTextFile(dir2.getAbsolutePath(), "file-3");
        String file4 = createTextFile(dir2.getAbsolutePath(), "file-4");

        fs2.append(dir2, System.out);
        fs2.check(System.out);

        File image2 = Files.createTempFile("cromix-2-", ".imd").toFile();
        try (FileOutputStream imd = new FileOutputStream(image2)) {
            fs2.persist(imd);
        }

        FileSystemOps fso3 = FileSystems.getIMDFloppyFileSystem(image2.getPath(), System.out);
        assertThat(fso3, instanceOf(CromixFileSystem.class));

        CromixFileSystem fs3 = (CromixFileSystem)fso3;
        fs3.check(System.out);

        FileUtils.deleteDirectory(dir1);
        FileUtils.deleteDirectory(dir2);
        image1.delete();
        image2.delete();
    }

    @Test
    public void append() throws IOException {
        DiskInterface disk = CromixIMDFloppyDisk.createLarge(System.out);
        assertThat(disk, notNullValue());

        CromixFileSystem fs = CromixFileSystem.initialise(disk);
        assertThat(fs, notNullValue());
        assertThat(CromixFileSystem.isValid(disk), is(true));

        CromixFileSystemStats stats = fs.check(System.out);
        assertThat(stats, notNullValue());

        File t1 = Files.createTempDirectory(Paths.get("/tmp"), "test1-").toFile();
        assertThat(t1, notNullValue());
        assertThat(t1.exists(), is(true));
        assertThat(t1.isDirectory(), is(true));
        String file1 = createTextFile(t1.getAbsolutePath(), "file-1");
        String file2 = createTextFile(t1.getAbsolutePath(), "file-2");

        fs.addDirectory(t1, System.out);

        fs.list(System.out);

        fs.check(System.out);

        File t2 = Files.createTempDirectory(Paths.get("/tmp"), "test2-").toFile();
        assertThat(t2, notNullValue());
        assertThat(t2.exists(), is(true));
        assertThat(t2.isDirectory(), is(true));
        String file3 = createTextFile(t2.getAbsolutePath(), "file-3");
        String file4 = createTextFile(t2.getAbsolutePath(), "file-4");

        fs.append(t2, System.out);

        fs.list(System.out);

        fs.check(System.out);

        File t3 = Files.createTempDirectory(Paths.get("/tmp"), "test3-").toFile();
        if (t3.exists()) {
            t3.delete();
        }
        t3.mkdirs();
        fs.extract(t3.getPath(), System.out);
        assertThat(compare(file1, t3.getPath()), is(true));
        assertThat(compare(file2, t3.getPath()), is(true));
        assertThat(compare(file3, t3.getPath()), is(true));
        assertThat(compare(file4, t3.getPath()), is(true));

        FileUtils.deleteDirectory(t1);
        FileUtils.deleteDirectory(t2);
        FileUtils.deleteDirectory(t3);
    }

    private boolean compare(String file, String dir) throws IOException {
        File a = new File(file);
        File b = Paths.get(dir, a.getName()).toFile();
        System.out.printf("%s %s\n", a.getPath(), b.getPath());
        return FileUtils.contentEquals(a,b);
    }

    private String createTextFile(String dir, String name) throws IOException {
        Random random = new Random(System.nanoTime());
        File file = Paths.get(dir, String.format("%s.txt", name)).toFile();
        byte[] content = new byte[random.nextInt(10000) + 100];
        random.nextBytes(content);
        FileUtils.writeByteArrayToFile(file, Base64.getEncoder().encode(content));
        return file.getPath();
    }

    @Test
    public void check() throws IOException {
        File imdFile = new File("/tmp/marcus/fred.imd");

        try (FileInputStream src = new FileInputStream(imdFile)) {
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
    public void m() throws IOException {
        DiskInterface disk = CromixIMDFloppyDisk.createLarge(System.out);
        assertThat(disk, notNullValue());

        CromixFileSystem fs = CromixFileSystem.initialise(disk);
        assertThat(fs, notNullValue());
        assertThat(CromixFileSystem.isValid(disk), is(true));

        File file = new File("/home/dwildie/cromemcos/m/cromemco/code/disks/640 gen directories");
        assertThat(file, notNullValue());
        assertThat(file.exists(), is(true));
        assertThat(file.isDirectory(), is(true));
        fs.addDirectory(file, System.out);

    }

    @Test
    public void addDirectory() throws IOException {

        DiskInterface disk = CromixIMDFloppyDisk.createLarge(System.out);
        assertThat(disk, notNullValue());

        CromixFileSystem fs = CromixFileSystem.initialise(disk);
        assertThat(fs, notNullValue());
        assertThat(CromixFileSystem.isValid(disk), is(true));

        CromixFileSystemStats stats = fs.check(System.out);
        assertThat(stats, notNullValue());

        CromixFileSystemStats expectedStats = new CromixFileSystemStats(0, 0, new BlockStats(2307), new InodeStats(508));
        expectedStats.getInodeStats().setDirectoryInodes(1);
        expectedStats.getInodeStats().setFreeInodes(507);
        expectedStats.getBlockStats().setOnFreeList(2306);
        expectedStats.getBlockStats().setDirectoryBlocks(1);
        expectedStats.getBlockStats().setFreeListBlocks(2306);
        expectedStats.getBlockStats().setDirectories(1);

        assertStats(stats, expectedStats);

        File file = new File("/home/dwildie/cromemcos/m/cromemco/code/disks/094 Cromix 162 disk1 bootable");
        assertThat(file, notNullValue());
        assertThat(file.exists(), is(true));
        assertThat(file.isDirectory(), is(true));
        fs.addDirectory(file, System.out);

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