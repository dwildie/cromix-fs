package au.wildie.m68k.cromixfs.disk.floppy.cromix;

import au.wildie.m68k.cromixfs.disk.floppy.IMDFloppyImage;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.disk.imd.Sector;
import au.wildie.m68k.cromixfs.disk.imd.Track;

import java.io.*;

import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskDensity.DOUBLE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskDensity.SINGLE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSize.LARGE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSize.SMALL;

public class CromixFloppyDisk extends IMDFloppyImage {

    private final CromixFloppyInfo info;

    public CromixFloppyDisk(IMDImage image, String formatLabel, PrintStream out) {
        super(image, formatLabel, out);

        if (formatLabel.charAt(0) != 'C') {
            throw new IMDFloppyException("Not a cromix disk");
        }

        DiskSize diskSize = formatLabel.charAt(1) == 'L' ? LARGE : SMALL;
        DiskSides diskSides = formatLabel.charAt(2) == 'D' ? DiskSides.DOUBLE : DiskSides.SINGLE;
        DiskDensity diskDensity = formatLabel.charAt(4) == 'D' ? DOUBLE : SINGLE;
        info = CromixFloppyInfo.get(diskSize, diskSides, diskDensity);
    }

    public void writeImage(String fileName, boolean interleaved) throws IOException {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }

        try (OutputStream out = new FileOutputStream(file)) {
            for (Track track : image.getTracks()) {
                if (track.getCylinder() == 0 && track.getHead() == 0) {
                    // Write track 0
                    for (int i = 0; i < track.getSectorCount(); i++) {
                        out.write(track.getSector(i + 1).getData());
                    }
                } else {
                    for (int i = 0; i < track.getSectorCount(); i++) {
                        int sectorNumber = interleaved ? i + 1 : info.getInterleave()[i] + 1;
                        out.write(track.getSector(sectorNumber).getData());
                    }
                }
            }
            out.flush();
        }
    }

    @Override
    public byte[] getInterleave() {
        return info.getInterleave();
    }

    @Override
    public byte[] getSuperBlock() {
        // Will be in the first track, 128byte sectors
        byte[] block = new byte[512];

        for (int i = 0; i < 4; i++) {
            System.arraycopy(image.getSector(0, 0, 5 + i).getData(), 0, block, 128 * i, 128);
        }

        return block;
    }

    @Override
    public byte[] getBlock(int blockNumber) {
        int c = getCylinderForBlock(blockNumber);
        int h = getHeadForBlock(blockNumber);
        int s = getSectorForBlock(blockNumber);

        int is = info.getInterleave()[0xFF & s] + 1;

        Sector sector = image.getSector(c,h,is);
        return sector.getData();
    }

    private int getCylinderForBlock(int block) {
        return (block + info.getBlockOffset()) / (2 * info.getSectorsPerTrack());
    }

    private int getHeadForBlock(int block) {
        return ((block + info.getBlockOffset()) / info.getSectorsPerTrack()) % info.getHeads();
    }

    private int getSectorForBlock(int block) {
        return (block + info.getBlockOffset()) % info.getSectorsPerTrack();
    }

    @Override
    public void checkSupported() {
//        if (diskDensity != DOUBLE) {
//            throw new RuntimeException(String.format("%s density disks are not supported", diskDensity));
//        }
//        if (diskSides != DiskSides.DOUBLE) {
//            throw new RuntimeException(String.format("%s sided disks are not supported", diskSides));
//        }
    }
}
