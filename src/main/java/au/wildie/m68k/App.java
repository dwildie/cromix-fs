package au.wildie.m68k;

import au.wildie.m68k.cromixfs.disk.floppy.CromixFloppyDisk;

import java.io.File;
import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException {
        if (args.length == 2 && args[0].equalsIgnoreCase("-l")) {
            if (!new File(args[1]).exists()) {
                System.out.println("Cannot open IMD file " + args[1]);
                return;
            }

            CromixFloppyDisk floppy = new CromixFloppyDisk(args[1]);

            floppy.list(System.out);
            return;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("-x")) {
            if (!new File(args[1]).exists()) {
                System.out.println("Cannot open IMD file " + args[1]);
                return;
            }

            File target = new File(args[2]);
            if (!target.exists()) {
                target.mkdirs();
            }
            CromixFloppyDisk floppy = new CromixFloppyDisk(args[1]);

            floppy.extract(args[2]);
            return;
        }

        showUsage();
        return;
    }

    private static void showUsage() {
        System.out.println("java -jar archive.jar -l file.imd ");
        System.out.println("java -jar archive.jar -x file.imd path");
    }
}
