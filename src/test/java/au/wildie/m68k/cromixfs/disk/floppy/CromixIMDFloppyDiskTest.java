package au.wildie.m68k.cromixfs.disk.floppy;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.SectorInvalidException;
import au.wildie.m68k.cromixfs.fs.FileSystem;
import au.wildie.m68k.cromixfs.fs.FileSystemOps;
import au.wildie.m68k.cromixfs.fs.FileSystems;
import au.wildie.m68k.cromixfs.ftar.CromixFtar;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CromixIMDFloppyDiskTest {
    private static final String CLDSDD_IMAGE = "imd/848CR162.IMD";
    private static final String CSSSDD_IMAGE = "imd/348C1105.IMD";
    private static final String UNIFORM_IMAGE = "imd/904C3140.IMD";
    private static final String UNIFORM1_IMAGE = "imd/152C0241.IMD";
    private static final String UNIFORM2_IMAGE = "imd/158DBMS.IMD";
    private static final String DAMAGED_FS_IMAGE = "imd/720CX172.IMD";

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
//        String srcPath = CLDSDD_IMAGE;
//        InputStream src = getClass().getClassLoader().getResourceAsStream(srcPath);
//        File out = Paths.get(System.getProperty("java.io.tmpdir"), String.format("%s.%s", FilenameUtils.removeExtension(srcPath), "img")).toFile();
        String srcPath = "/tmp/657C168.IMD";
        InputStream src = Files.newInputStream(new File(srcPath).toPath());
        File out = Paths.get(String.format("%s.%s", FilenameUtils.removeExtension(srcPath), "img")).toFile();
        FileSystem fs = (FileSystem)FileSystems.getIMDFloppyFileSystem(src, System.out);
        ((DiskInterface)fs.getDisk()).writeImage(out, true);
        System.out.println("done");
    }

    @Test
    public void listDamaged() throws IOException {
        InputStream src = getClass().getClassLoader().getResourceAsStream(DAMAGED_FS_IMAGE);
        FileSystem fs = (FileSystem)FileSystems.getIMDFloppyFileSystem(src, System.out);
        fs.list(System.out);
        System.out.println("done");
    }

    @Test
    public void listCSSSDD() throws IOException {
        InputStream src = getClass().getClassLoader().getResourceAsStream(CSSSDD_IMAGE);
        FileSystem fs = (FileSystem)FileSystems.getIMDFloppyFileSystem(src, System.out);
        fs.list(System.out);
        System.out.println("done");
    }

    @Test
    public void listCLDSDD() throws IOException {
        InputStream src = getClass().getClassLoader().getResourceAsStream(CLDSDD_IMAGE);
        FileSystem fs = (FileSystem)FileSystems.getIMDFloppyFileSystem(src, System.out);
        fs.list(System.out);
        System.out.println("done");
    }

    @Test
    public void listUniform() throws IOException {
        InputStream src = getClass().getClassLoader().getResourceAsStream(UNIFORM1_IMAGE);
        FileSystem fs = (FileSystem)FileSystems.getIMDFloppyFileSystem(src, System.out);
        fs.list(System.out);
        System.out.println("done");
    }

    @Test
    public void extractCLDSDD() throws IOException {
        InputStream src = getClass().getClassLoader().getResourceAsStream(CLDSDD_IMAGE);
        extract(src, new File(EXTRACT_PATH));
    }

    @Test
    public void extractUniform() throws IOException {
        InputStream src = getClass().getClassLoader().getResourceAsStream(UNIFORM_IMAGE);
        extract(src, new File(EXTRACT_PATH));
    }

    public void extract(InputStream src, File extractDir) throws IOException {
        if (extractDir.exists()) {
            FileUtils.deleteDirectory(extractDir);
        }
        extractDir.mkdirs();

        FileSystem fs = (FileSystem)FileSystems.getIMDFloppyFileSystem(src, System.out);
        fs.extract(EXTRACT_PATH, System.out);
        System.out.println("done");
    }
}