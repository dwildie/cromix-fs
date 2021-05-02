package au.wildie.m68k.cromixfs.disk.floppy;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.disk.imd.Sector;
import au.wildie.m68k.cromixfs.disk.imd.Track;
import au.wildie.m68k.cromixfs.fs.FileSystem;

import java.io.*;
import java.util.Arrays;

import static au.wildie.m68k.cromixfs.disk.floppy.DiskDensity.DOUBLE;
import static au.wildie.m68k.cromixfs.disk.floppy.DiskDensity.SINGLE;
import static au.wildie.m68k.cromixfs.disk.floppy.DiskSize.LARGE;
import static au.wildie.m68k.cromixfs.disk.floppy.DiskSize.SMALL;

public class CromixFloppyDisk implements DiskInterface {

    private final IMDImage image;
    private final DiskSize diskSize;
    private final DiskDensity diskDensity;
    private final DiskSides diskSides;
    private final CromixFloppyInfo info;

    public CromixFloppyDisk(String fileName) {
        image = new IMDImage(0, fileName);

        Sector zero = image.getSector(0, 0, 1);
        byte[] fmt = Arrays.copyOfRange(zero.getData(), 120, 127);

        System.out.format("Disk format: %c%c%c%c%c%c\n\n", fmt[0], fmt[1], fmt[2], fmt[3], fmt[4], fmt[5]);

        if (fmt[0] != 'C') {
            throw new CromixFloppyException("Not a cromix disk");
        }

        diskSize = fmt[1] == 'L' ? LARGE : SMALL;
        diskDensity = fmt[2] == 'D' ? DOUBLE : SINGLE;
        diskSides = fmt[4] == 'D' ? DiskSides.DOUBLE : DiskSides.SINGLE;
        checkSupported();

        info = CromixFloppyInfo.get(diskSize, diskDensity);
    }

    public void list(PrintStream out) throws IOException {
        new FileSystem(this).list(out);
    }

    public void extract(String path) throws IOException {
        System.out.printf("Extracting to directory: %s\n", path);
        new FileSystem(this).extract(path);
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

    private void checkSupported() {
        if (diskDensity != DOUBLE) {
            throw new RuntimeException(String.format("%s density disks are not supported", diskSize));
        }
        if (diskSides != DiskSides.DOUBLE) {
            throw new RuntimeException(String.format("%s sided disks are not supported", diskSides));
        }
    }
}
