package au.wildie.m68k.cromixfs.disk.floppy.cdos;

import au.wildie.m68k.cromixfs.disk.floppy.IMDFloppyImage;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskDensity;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSides;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSize;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.disk.imd.IMDSector;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskDensity.DOUBLE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskDensity.SINGLE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSize.LARGE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSize.SMALL;

public class CDOSFloppyDisk extends IMDFloppyImage {
    private final CDOSFloppyInfo info;

    public CDOSFloppyDisk(IMDImage image, PrintStream out) {
        super(image, out);

        DiskSize diskSize = getFormatLabel().charAt(0) == 'L' ? LARGE : SMALL;
        DiskSides diskSides = getFormatLabel().charAt(2) == 'D' ? DiskSides.DOUBLE : DiskSides.SINGLE;
        DiskDensity diskDensity = getFormatLabel().charAt(4) == 'D' ? DOUBLE : SINGLE;
        info = CDOSFloppyInfo.get(diskSize, diskSides, diskDensity);
        out.format("Disk format: %s\n\n", getFormatLabel());
    }

    @Override
    public byte[] getSuperBlock() {
        // First block
        //return image.getSector(info.getFileArea()[0], info.getFileArea()[1],1).getData();
        return getBlock(0);
    }

    @Override
    public void flushSuperBlock(byte[] data) {
        // TODO
    }

    @Override
    public byte[] getBlock(int blockNumber) {
        int sectorNumber = info.getFileArea()[0] * info.getSectorsPerTrack() * 2 + info.getFileArea()[1] * info.getSectorsPerTrack() + blockNumber;

        int c = getCylinderForSectorNumber(sectorNumber);
        int h = getHeadForSectorNumber(sectorNumber);
        int s = getSectorSectorNumber(sectorNumber);

        int is = info.getInterleave()[0xFF & s] + 1;

        IMDSector sector = image.getSector(c,h,is);
        return sector.getData();
    }

    @Override
    public void persist(OutputStream archive) throws IOException {
        // TODO
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
    public byte[] getInterleave() {
        return new byte[0];
    }
}
