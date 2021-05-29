package au.wildie.m68k.cromixfs.fs;


import au.wildie.m68k.cromixfs.disk.floppy.cdos.CDOSFloppyDisk;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.CromixFloppyDisk;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.IMDFloppyException;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.disk.imd.Sector;
import au.wildie.m68k.cromixfs.disk.st.CromixStDisk;
import au.wildie.m68k.cromixfs.disk.st.STDiskException;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

public class FileSystems {
    public static FileSystem getFloppyFileSystem(String fileName, PrintStream out) throws IOException {
        IMDImage image = IMDImage.fromFile(0, fileName, out);

        Sector zero = image.getSector(0, 0, 1);
        String formatLabel = new String(Arrays.copyOfRange(zero.getData(), 120, 127)).replaceAll("\\P{InBasic_Latin}", "");

        if (formatLabel.charAt(0) == 'L' && formatLabel.charAt(1) == 'G') {
            // Large (8") CDOS
            return new CDOSFileSystem(new CDOSFloppyDisk(image, formatLabel, out));
        }
        if (formatLabel.charAt(0) == 'S' && formatLabel.charAt(1) == 'M') {
            // Small (5.25") CDOS
            return new CDOSFileSystem(new CDOSFloppyDisk(image, formatLabel, out));
        }
        if (formatLabel.charAt(0) == 'C') {
            // Large or small Cromix floppy
            return new CromixFileSystem(new CromixFloppyDisk(image, formatLabel, out));
        }

        throw new IMDFloppyException(String.format("Unrecognised disk, format label: \"%s\"", formatLabel));
    }

    public static FileSystem getSTFileSystem(String fileName, PrintStream out) throws STDiskException {
        return new CromixFileSystem(new CromixStDisk(fileName));
    }
}
