package au.wildie.m68k.cromixfs.fs;


import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.floppy.HFEFloppyException;
import au.wildie.m68k.cromixfs.disk.floppy.IMDFloppyException;
import au.wildie.m68k.cromixfs.disk.floppy.VFDFloppyException;
import au.wildie.m68k.cromixfs.disk.floppy.cdos.CDOSFloppyDisk;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.CromixFloppyInfo;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.CromixHFEFloppyDisk;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.CromixIMDFloppyDisk;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.CromixVFDFloppyDisk;
import au.wildie.m68k.cromixfs.disk.hxc.HFEImage;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.disk.st.CromixStDisk;
import au.wildie.m68k.cromixfs.disk.st.STDiskException;
import au.wildie.m68k.cromixfs.disk.vfd.InvalidVFDImageException;
import au.wildie.m68k.cromixfs.disk.vfd.VFDImage;
import au.wildie.m68k.cromixfs.ftar.CromixFtar;
import au.wildie.m68k.cromixfs.ftar.FTarIMDDisk;
import au.wildie.m68k.cromixfs.ftar.FTarHFEDisk;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class FileSystems {
    public static FileSystemOps getIMDFloppyFileSystem(String fileName, PrintStream out) throws IOException {
        return getIMDFloppyFileSystem(IMDImage.fromFile(0, fileName, out), out);
    }

    public static FileSystemOps getIMDFloppyFileSystem(InputStream src, PrintStream out) throws IOException {
        return getIMDFloppyFileSystem(IMDImage.fromStream(src, out), out);
    }

    public static FileSystemOps getIMDFloppyFileSystem(IMDImage image, PrintStream out) throws IOException {
        String formatLabel = CromixFloppyInfo.getFormatLabel(image.getSector(0, 0, 1).getData());

        if (formatLabel.length() >= 6) {
            if (formatLabel.charAt(0) == 'L' && formatLabel.charAt(1) == 'G') {
                // Large (8") CDOS
                return new CDOSFileSystem(new CDOSFloppyDisk(image, out));
            }
            if (formatLabel.charAt(0) == 'S' && formatLabel.charAt(1) == 'M') {
                // Small (5.25") CDOS
                return new CDOSFileSystem(new CDOSFloppyDisk(image, out));
            }
            if (formatLabel.charAt(0) == 'C') {
                // Large or small Cromix floppy
                DiskInterface disk = new CromixIMDFloppyDisk(image, out);
                if (CromixFileSystem.isValid(disk)) {
                    return new CromixFileSystem(disk);
                }
            }
        }

        // Could be uniform or ftar
        try {
            DiskInterface disk = new CromixIMDFloppyDisk(image, out);
            if (CromixFileSystem.isValid(disk)) {
                return new CromixFileSystem(disk);
            }
        } catch (IMDFloppyException ignored) {
            // swallow it
        }

        CromixFtar ftar = new CromixFtar(new FTarIMDDisk(image, out));
        if (ftar.isValid()) {
            return ftar;
        }

        throw new IMDFloppyException(String.format("Unrecognised disk, format label: \"%s\"", formatLabel));
    }

    public static FileSystemOps getHFEFloppyFileSystem(String fileName, PrintStream out) throws IOException {
        return getHFEFloppyFileSystem(HFEImage.from(FileUtils.readFileToByteArray(new File(fileName))), out);
    }

    public static FileSystemOps getHFEFloppyFileSystem(InputStream file, PrintStream out) throws IOException {
        return getHFEFloppyFileSystem(HFEImage.from(file), out);
    }

    public static FileSystemOps getHFEFloppyFileSystem(HFEImage image, PrintStream out) throws IOException {
        byte[] zero = image.read(0, 0, 1);
        String formatLabel = CromixFloppyInfo.getFormatLabel(zero);
        if (formatLabel.length() >= 6 && formatLabel.charAt(0) == 'C') {
            // Large or small Cromix floppy
            DiskInterface disk = new CromixHFEFloppyDisk(image, formatLabel, out);
            if (CromixFileSystem.isValid(disk)) {
                return new CromixFileSystem(disk);
            }
        }

        CromixFtar ftar = new CromixFtar(new FTarHFEDisk(image, out));
        if (ftar.isValid()) {
            return ftar;
        }

        throw new HFEFloppyException(String.format("Unrecognised disk, format label: \"%s\"", formatLabel));
    }

    public static FileSystem getVFDFloppyFileSystem(String fileName, PrintStream out) throws IOException, InvalidVFDImageException {
        return getVFDFloppyFileSystem(VFDImage.fromFile(0, fileName, out), out);
    }

    public static FileSystem getVFDFloppyFileSystem(InputStream file, PrintStream out) throws IOException, InvalidVFDImageException {
        return getVFDFloppyFileSystem(VFDImage.fromStream(file), out);
    }

    protected static FileSystem getVFDFloppyFileSystem(VFDImage image, PrintStream out) throws IOException {
        byte[] zero = image.read(0, 0, 1);
        String formatLabel = CromixFloppyInfo.getFormatLabel(zero);
        if (formatLabel.charAt(0) == 'C') {
            // Large or small Cromix floppy
            return new CromixFileSystem(new CromixVFDFloppyDisk(image, formatLabel, out));
        }
        throw new VFDFloppyException(String.format("Unrecognised disk, format label: \"%s\"", formatLabel));
    }

    public static FileSystem getSTFileSystem(String fileName, PrintStream out) throws STDiskException, IOException {
        return new CromixFileSystem(new CromixStDisk(fileName));
    }
}
