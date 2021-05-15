package au.wildie.m68k.cromixfs.disk.floppy.cdos;

import au.wildie.m68k.cromixfs.disk.floppy.IMDFloppyImage;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskDensity;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSides;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSize;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.disk.imd.Sector;

import java.io.PrintStream;

import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskDensity.DOUBLE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskDensity.SINGLE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSize.LARGE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSize.SMALL;

public class CDOSFloppyDisk extends IMDFloppyImage {
    private final CDOSFloppyInfo info;

    public CDOSFloppyDisk(IMDImage image, String formatLabel, PrintStream out) {
        super(image, formatLabel, out);

        DiskSize diskSize = formatLabel.charAt(0) == 'L' ? LARGE : SMALL;
        DiskSides diskSides = formatLabel.charAt(2) == 'D' ? DiskSides.DOUBLE : DiskSides.SINGLE;
        DiskDensity diskDensity = formatLabel.charAt(4) == 'D' ? DOUBLE : SINGLE;
        info = CDOSFloppyInfo.get(diskSize, diskSides, diskDensity);
    }

    @Override
    public byte[] getSuperBlock() {
        // First block
        //return image.getSector(info.getFileArea()[0], info.getFileArea()[1],1).getData();
        return getBlock(0);
    }

    @Override
    public byte[] getBlock(int blockNumber) {
        int sectorNumber = info.getFileArea()[0] * info.getSectorsPerTrack() * 2 + info.getFileArea()[1] * info.getSectorsPerTrack() + blockNumber;

        int c = getCylinderForSectorNumber(sectorNumber);
        int h = getHeadForSectorNumber(sectorNumber);
        int s = getSectorSectorNumber(sectorNumber);

        int is = info.getInterleave()[0xFF & s] + 1;

        Sector sector = image.getSector(c,h,is);
        return sector.getData();
    }

    private int getCylinderForSectorNumber(int sectorNumber) {
        return sectorNumber / (2 * info.getSectorsPerTrack());
    }

    private int getHeadForSectorNumber(int sectorNumber) {
        return (sectorNumber / info.getSectorsPerTrack()) % info.getHeads();
    }

    private int getSectorSectorNumber(int sectorNumber) {
        return sectorNumber % info.getSectorsPerTrack();
    }

    @Override
    public void checkSupported() {

    }

    @Override
    public byte[] getInterleave() {
        return new byte[0];
    }
}
