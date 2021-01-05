package au.wildie.m68k.cromixfs.disk.floppy;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.disk.imd.Sector;
import au.wildie.m68k.cromixfs.disk.imd.Track;
import au.wildie.m68k.cromixfs.fs.FileSystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static au.wildie.m68k.cromixfs.disk.floppy.DiskDensity.DOUBLE;
import static au.wildie.m68k.cromixfs.disk.floppy.DiskDensity.SINGLE;
import static au.wildie.m68k.cromixfs.disk.floppy.DiskSize.LARGE;
import static au.wildie.m68k.cromixfs.disk.floppy.DiskSize.SMALL;

public class CromixFloppyDisk implements DiskInterface {

    private IMDImage image;
    private DiskSize diskSize;
    private DiskDensity diskDensity;
    private DiskSides diskSides;
    private CromixFloppyInfo info;

    public CromixFloppyDisk(String fileName) {
        image = new IMDImage(0, fileName);

        Sector zero = image.getSector(0, 0, 1);
        byte[] typeIde = Arrays.copyOfRange(zero.getData(), 120, 127);

        if (typeIde[0] != 'C') {
            throw new CromixFloppyException("Not a cromix disk");
        }

        diskSize = typeIde[1] == 'L' ? LARGE : SMALL;
        diskDensity = typeIde[2] == 'D' ? DOUBLE : SINGLE;
        diskSides = typeIde[4] == 'D' ? DiskSides.DOUBLE : DiskSides.SINGLE;
        checkSupported();

        info = CromixFloppyInfo.get(diskSize, diskDensity);
    }

    public void list() throws IOException {
        new FileSystem(this).list();
    }

    public void extract(String path) throws IOException {
        System.out.printf("Extracting to directory: %s\n", path);
        new FileSystem(this).extract(path);
    }

    public void writeImage(String fileName) throws IOException {
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
                        out.write(track.getSector(info.getInterleave()[i] + 1).getData());
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
        return (block + (info.getSectorsFirstTrack() - info.getSectorsPerTrack())) / (2 * info.getSectorsPerTrack());
    }

    private int getHeadForBlock(int block) {
        return ((block + (info.getSectorsFirstTrack() - info.getSectorsPerTrack())) / info.getSectorsPerTrack()) % info.getHeads();
    }

    private int getSectorForBlock(int block) {
        return (block + (info.getSectorsFirstTrack() - info.getSectorsPerTrack())) % info.getSectorsPerTrack();
    }

    private void checkSupported() {
        if (diskSize != LARGE) {
            throw new RuntimeException(String.format("%s size disks are not supported", diskSize));
        }
        if (diskDensity != DOUBLE) {
            throw new RuntimeException(String.format("%s density disks are not supported", diskSize));
        }
        if (diskSides != DiskSides.DOUBLE) {
            throw new RuntimeException(String.format("%s sided disks are not supported", diskSides));
        }
    }
}
