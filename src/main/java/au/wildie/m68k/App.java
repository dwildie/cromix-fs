package au.wildie.m68k;

import au.wildie.m68k.cromixfs.disk.floppy.FileScan;
import au.wildie.m68k.cromixfs.disk.st.STDiskException;
import au.wildie.m68k.cromixfs.disk.vfd.InvalidVFDImageException;
import au.wildie.m68k.cromixfs.disk.vfd.VFDConverter;
import au.wildie.m68k.cromixfs.disk.vfd.VFDErrorsException;
import au.wildie.m68k.cromixfs.fs.FileSystemOps;
import au.wildie.m68k.cromixfs.fs.FileSystems;
import au.wildie.m68k.cromixfs.ftar.CromixFtar;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException, STDiskException, InvalidVFDImageException {
        if (args.length == 2 && args[0].equalsIgnoreCase("-l")) {
            if (!new File(args[1]).exists()) {
                System.out.printf("Cannot open image file %s\n", args[1]);
                return;
            }
            get(args[1]).list(System.out);
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
        } else if (args.length == 3 && args[0].equalsIgnoreCase("-c")) {
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
        System.out.println("java -jar archive.jar -l file.imd | file.hfe");
        System.out.println("java -jar archive.jar -x file.imd | file.hfe path");
        System.out.println("java -jar archive.jar -c file.imd path");
//        System.out.println("java -jar archive.jar -v file.imd | file.hfe path");
        System.out.println("java -jar archive.jar -s path");
    }
}
