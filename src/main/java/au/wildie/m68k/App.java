package au.wildie.m68k;

import au.wildie.m68k.cromixfs.disk.floppy.FileScan;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.CromixIMDFloppyDisk;
import au.wildie.m68k.cromixfs.disk.st.STDiskException;
import au.wildie.m68k.cromixfs.disk.vfd.InvalidVFDImageException;
import au.wildie.m68k.cromixfs.disk.vfd.VFDConverter;
import au.wildie.m68k.cromixfs.disk.vfd.VFDErrorsException;
import au.wildie.m68k.cromixfs.fs.FileSystemOps;
import au.wildie.m68k.cromixfs.fs.FileSystems;
import au.wildie.m68k.cromixfs.fs.cromix.CromixFileSystem;
import au.wildie.m68k.cromixfs.ftar.CromixFtar;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args ) throws IOException, STDiskException, InvalidVFDImageException {
        System.out.printf("%s version %s, Damian Wildie\n\n", getArtifactId(), getVersion());

        if (args.length == 2 && args[0].equalsIgnoreCase("-l")) {
            if (!new File(args[1]).exists()) {
                System.out.printf("Cannot open image file %s\n", args[1]);
                return;
            }
            get(args[1]).list(System.out);
            return;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("-c")) {
            if (!new File(args[1]).exists()) {
                System.out.printf("Cannot open image file %s\n", args[1]);
                return;
            }
            FileSystemOps fs = get(args[1]);
            if (fs instanceof CromixFileSystem) {
                ((CromixFileSystem)fs).check(System.out);
            }
            return;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("-di")) {
            if (!new File(args[1]).exists()) {
                System.out.printf("Cannot open image file %s\n", args[1]);
                return;
            }
            FileSystemOps fs = get(args[1]);
            if (fs instanceof CromixFileSystem) {
                ((CromixFileSystem)fs).dumpInodes(System.out);
            }
            return;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("-x")) {
            if (!new File(args[1]).exists()) {
                System.out.printf("Cannot open image file %s\n", args[1]);
                return;
            }
            File target = new File(args[2]);
            if (!target.exists()) {
                target.mkdirs();
            }
            get(args[1]).extract(args[2], System.out);
            return;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("-f")) {
            if (!new File(args[2]).exists()) {
                System.out.printf("Source path %s does not exist\n", args[1]);
                return;
            }
            CromixFtar ftar = new CromixFtar("CLDSDD", System.out);
            File file = new File(args[1]);
            if (file.exists()) {
                file.delete();
            }
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (FileOutputStream archive = new FileOutputStream(file)) {
                ftar.create(args[2], archive, System.out);
            }
            return;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("-a")) {
            if (!new File(args[1]).exists()) {
                System.out.printf("Cannot open image file %s\n", args[1]);
                return;
            }
            File path = new File(args[2]);
            if (!path.exists()) {
                System.out.printf("Source path %s does not exist\n", args[1]);
                return;
            }
            FileSystemOps fs = get(args[1]);
            if (!(fs instanceof CromixFileSystem)) {
                System.out.println("Not a Cromix filesystem");
                return;
            }
            ((CromixFileSystem)fs).append(path, System.out);
            try (FileOutputStream archive = new FileOutputStream(args[1])) {
                ((CromixFileSystem)fs).persist(archive);
            }
            return;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("-m")) {
            if (!new File(args[2]).exists()) {
                System.out.printf("Source path %s does not exist\n", args[1]);
                return;
            }
            CromixFileSystem fs = CromixFileSystem.initialise(CromixIMDFloppyDisk.create("CLDSDD", System.out));
            File file = new File(args[1]);
            if (file.exists()) {
                file.delete();
            }
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            fs.addDirectory(new File(args[2]), System.out);

            try (FileOutputStream archive = new FileOutputStream(file)) {
                fs.persist(archive);
            }
            return;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("-s")) {
            if (!new File(args[1]).exists()) {
                System.out.printf("Path does not exist %s\n", args[1]);
                return;
            }
            new FileScan(args[1]).scan();
            return;
        } else if (args.length >= 2 && (args[0].equalsIgnoreCase("-v") || args[0].equalsIgnoreCase("-vi"))) {
            File imdFile = new File(args[1]);
            File vfdFile = new File(args.length == 3 ? args[2] : FilenameUtils.removeExtension(imdFile.getPath()) + ".vfd");
            try {
                VFDConverter.imdToVfd(imdFile, vfdFile, args[0].equalsIgnoreCase("-v"));
                System.out.printf("VFD image saved in %s\n", vfdFile.getPath());
            } catch (VFDErrorsException e) {
                System.out.println(e.getMessage());
            }
            return;
        }

        showUsage();
    }

    private static FileSystemOps get(String filename) throws IOException, InvalidVFDImageException, STDiskException {
        FileSystemOps fs;
        if (filename.toLowerCase().trim().endsWith(".imd")) {
            fs = FileSystems.getIMDFloppyFileSystem(filename, System.out);
        } else if (filename.toLowerCase().trim().endsWith(".vfd")) {
            fs = FileSystems.getVFDFloppyFileSystem(filename, System.out);
        } else if (filename.toLowerCase().trim().endsWith(".hfe")) {
            fs = FileSystems.getHFEFloppyFileSystem(filename, System.out);
        } else {
            fs = FileSystems.getSTFileSystem(filename, System.out);
        }
        return fs;
    }

    private static void showUsage() {
        System.out.println();

        String jarName = getJarName();

        System.out.print("\nCheck a cromix image:\n");
        System.out.printf("  java -jar %s -c file.imd\n", jarName);

        System.out.print("\nDump cromix inodes:\n");
        System.out.printf("  java -jar %s -di file.imd\n", jarName);

        System.out.print("\nList files in an image:\n");
        System.out.printf("  java -jar %s -l file.imd\n", jarName);

        System.out.print("\nExtract files from an image to path:\n");
        System.out.printf("  java -jar %s -x file.imd path\n", jarName);

        System.out.print("\nCreate a new Cromix ftar image containing files from path:\n");
        System.out.printf("  java -jar %s -f file.imd path\n", jarName);

        System.out.print("\nAppend file(s) to an existing mountable Cromix image:\n");
        System.out.printf("  java -jar %s -a file.imd path\n", jarName);

        System.out.print("\nCreate a mountable Cromix image containing files from path:\n");
        System.out.printf("  java -jar %s -m file.imd path\n", jarName);

        //        System.out.printf("\njava -jar %s -v file.imd | file.hfe path\n");

        System.out.print("\nScan path and display image information:\n");
        System.out.printf("  java -jar %s -s path\n", jarName);
    }

    private static String getJarName() {
        try {
            return new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getName();
        } catch (URISyntaxException e) {
            return "cromix-fs.jar";
        }
    }

    private static String getArtifactId() {
        try {
            final Properties properties = new Properties();
            properties.load(App.class.getClassLoader().getResourceAsStream("project.properties"));
            return properties.getProperty("artifactId");
        } catch (IOException e) {
            return "?";
        }
    }

    private static String getVersion() {
        try {
            final Properties properties = new Properties();
            properties.load(App.class.getClassLoader().getResourceAsStream("project.properties"));
            return properties.getProperty("version");
        } catch (IOException e) {
            return "?.?";
        }
    }
}
