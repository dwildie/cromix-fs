package au.wildie.m68k;

import au.wildie.m68k.cromixfs.disk.floppy.FileScan;
import au.wildie.m68k.cromixfs.disk.floppy.vfd.VFDConverter;
import au.wildie.m68k.cromixfs.disk.st.STDiskException;
import au.wildie.m68k.cromixfs.fs.FileSystems;
import au.wildie.m68k.cromixfs.fs.FileSystem;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException, STDiskException {
        if (args.length == 2 && args[0].equalsIgnoreCase("-l")) {
            if (!new File(args[1]).exists()) {
                System.out.printf("Cannot open image file %s\n", args[1]);
                return;
            }

            FileSystem fs;
            if (args[1].toLowerCase().trim().endsWith(".imd")) {
                fs = FileSystems.getFloppyFileSystem(args[1], System.out);
            } else {
                fs = FileSystems.getSTFileSystem(args[1], System.out);
            }
            fs.list(System.out);
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

            FileSystem fs;
            if (args[1].toLowerCase().trim().endsWith(".imd")) {
                fs = FileSystems.getFloppyFileSystem(args[1], System.out);
            } else {
                fs = FileSystems.getSTFileSystem(args[1], System.out);
            }

            fs.extract(args[2], System.out);
            return;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("-s")) {
            if (!new File(args[1]).exists()) {
                System.out.printf("Path does not exist %s\n", args[1]);
                return;
            }
            new FileScan(args[1]).scan();
            return;
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("-v")) {
            File imdFile = new File(args[1]);
            File vfdFile = new File(args.length == 3 ? args[2] : FilenameUtils.removeExtension(imdFile.getPath()) + ".vfd");
            VFDConverter.imdToVfd(imdFile, vfdFile);
            System.out.printf("VFD image saved in %s\n", vfdFile.getPath());
            return;
        }

        showUsage();
    }

    private static void showUsage() {
        System.out.println("java -jar archive.jar -l file.imd ");
        System.out.println("java -jar archive.jar -x file.imd path");
        System.out.println("java -jar archive.jar -v file.imd path");
        System.out.println("java -jar archive.jar -s path");
    }
}
