package au.wildie.m68k;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.floppy.FileScan;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.CromixIMDFloppyDisk;
import au.wildie.m68k.cromixfs.disk.st.STDiskException;
import au.wildie.m68k.cromixfs.disk.vfd.InvalidVFDImageException;
import au.wildie.m68k.cromixfs.disk.vfd.VFDConverter;
import au.wildie.m68k.cromixfs.disk.vfd.VFDErrorsException;
import au.wildie.m68k.cromixfs.fs.FileSystem;
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
 * cromix-fs
 *
 */
public class App {
    public static void main(String[] args ) throws IOException, STDiskException, InvalidVFDImageException {
        System.out.printf("%s version %s, Damian Wildie\n\n", getArtifactId(), getVersion());

        if (args.length == 2 && args[0].equalsIgnoreCase("-l")) {
            // List files
            if (!new File(args[1]).exists()) {
                System.out.printf("Cannot open image file %s\n", args[1]);
                return;
            }
            get(args[1]).list(System.out);
            return;
        } else if (args.length == 4 && args[0].equalsIgnoreCase("-l") && args[1].equalsIgnoreCase("-p")) {
            // List partition files
            if (!new File(args[3]).exists()) {
                System.out.printf("Cannot open image file %s\n", args[3]);
                return;
            }
            int partitionIndex = Integer.parseInt(args[2]);
            get(args[3], partitionIndex).list(System.out);
            return;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("-c")) {
            // Check filesystem
            if (!new File(args[1]).exists()) {
                System.out.printf("Cannot open image file %s\n", args[1]);
                return;
            }
            FileSystemOps fs = get(args[1]);
            if (fs instanceof CromixFileSystem) {
                ((CromixFileSystem)fs).check(System.out);
            }
            return;
        } else if (args.length == 4 && args[0].equalsIgnoreCase("-c") && args[1].equalsIgnoreCase("-p")) {
            // Check filesystem
            if (!new File(args[3]).exists()) {
                System.out.printf("Cannot open image file %s\n", args[3]);
                return;
            }
            int partitionIndex = Integer.parseInt(args[2]);
            FileSystemOps fs = get(args[3], partitionIndex);
            if (fs instanceof CromixFileSystem) {
                ((CromixFileSystem)fs).check(System.out);
            }
            return;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("-di")) {
            // Dump inodes
            if (!new File(args[1]).exists()) {
                System.out.printf("Cannot open image file %s\n", args[1]);
                return;
            }
            FileSystemOps fs = get(args[1]);
            if (fs instanceof CromixFileSystem) {
                ((CromixFileSystem)fs).dumpInodes(System.out);
            }
            return;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("-raw")) {
            // Convert to raw image
            if (!new File(args[1]).exists()) {
                System.out.printf("Cannot open image file %s\n", args[1]);
                return;
            }
            File raw = new File(args[2]);
            if (raw.exists()) {
                if (!raw.delete()) {
                    System.out.printf("Cannot delete output file %s\n", args[2]);
                    return;
                }
            }
            FileSystemOps fs = get(args[1]);
            ((DiskInterface)fs.getDisk()).writeImage(raw, true);
            return;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("-x")) {
            // Extract files
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
        } else if (args.length == 5 && args[0].equalsIgnoreCase("-x") && args[1].equalsIgnoreCase("-p")) {
            // Extract files
            if (!new File(args[3]).exists()) {
                System.out.printf("Cannot open image file %s\n", args[3]);
                return;
            }
            File target = new File(args[4]);
            if (!target.exists()) {
                target.mkdirs();
            }
            int partitionIndex = Integer.parseInt(args[2]);
            get(args[3], partitionIndex).extract(args[4], System.out);
            return;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("-f")) {
            // Create new ftar image
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
            // Append files to existing Cromix file system
            if (!new File(args[1]).exists()) {
                System.out.printf("Cannot open image file %s\n", args[1]);
                return;
            }
            File path = new File(args[2]);
            if (!path.exists()) {
                System.out.printf("Source path %s does not exist\n", args[2]);
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
        } else if ((args.length == 2 || args.length == 3) && (args[0].equalsIgnoreCase("-ml") || args[0].equalsIgnoreCase("-ms"))) {
            if (args.length == 3 && !new File(args[2]).exists()) {
                System.out.printf("Source path %s does not exist\n", args[1]);
                return;
            }
            CromixFileSystem fs = args[0].equalsIgnoreCase("-ml") ?
                    CromixFileSystem.initialise(CromixIMDFloppyDisk.createLarge(System.out))
                    :
                    CromixFileSystem.initialise(CromixIMDFloppyDisk.createSmall(System.out));

            if (args.length == 3) {
                fs.append(new File(args[2]), System.out);
            }

            File file = new File(args[1]);
            if (file.exists()) {
                file.delete();
            }
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

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
        return get(filename, null);
    }

    private static FileSystemOps get(String filename, Integer partitionIndex) throws IOException, InvalidVFDImageException, STDiskException {
        FileSystemOps fs;
        if (filename.toLowerCase().trim().endsWith(".imd")) {
            fs = FileSystems.getIMDFloppyFileSystem(filename, System.out);
        } else if (filename.toLowerCase().trim().endsWith(".vfd")) {
            fs = FileSystems.getVFDFloppyFileSystem(filename, System.out);
        } else if (filename.toLowerCase().trim().endsWith(".hfe")) {
            fs = FileSystems.getHFEFloppyFileSystem(filename, System.out);
        } else {
            fs = FileSystems.getSTFileSystem(filename, partitionIndex, System.out);
        }
        return fs;
    }

    private static void showUsage() {
        System.out.println();

        String jarName = getJarName();

        System.out.print("\nCheck a cromix image:\n");
        System.out.printf("  java -jar %s -c [-p partitionIndex] file.imd|file.img\n", jarName);

        System.out.print("\nDump cromix inodes:\n");
        System.out.printf("  java -jar %s -di file.imd\n", jarName);

        System.out.print("\nList files in an image:\n");
        System.out.printf("  java -jar %s -l [-p partitionIndex] file.imd|file.img\n", jarName);

        System.out.print("\nExtract files from an image to path:\n");
        System.out.printf("  java -jar %s -x [-p partitionIndex] file.imd|file.img path\n", jarName);

        System.out.print("\nCreate a new Cromix ftar image containing files from path:\n");
        System.out.printf("  java -jar %s -f file.imd path\n", jarName);

        System.out.print("\nAppend file(s) to an existing mountable Cromix image:\n");
        System.out.printf("  java -jar %s -a file.imd path\n", jarName);

        System.out.print("\nCreate a large (8\") mountable Cromix image containing files from path:\n");
        System.out.printf("  java -jar %s -ml file.imd path\n", jarName);

        System.out.print("\nCreate a small (5.25\") mountable Cromix image containing files from path:\n");
        System.out.printf("  java -jar %s -ms file.imd path\n", jarName);

        //        System.out.printf("\njava -jar %s -v file.imd | file.hfe path\n");

        System.out.print("\nScan path and display image information:\n");
        System.out.printf("  java -jar %s -s path\n", jarName);

        System.out.print("\nConvert IMD image to a raw image:\n");
        System.out.printf("  java -jar %s -raw file.imd file.img\n", jarName);
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
