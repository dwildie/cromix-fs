package au.wildie.m68k.cromixfs.disk.floppy.cromix;

import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskDensity.DOUBLE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskDensity.SINGLE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSize.LARGE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSize.SMALL;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import au.wildie.m68k.cromixfs.disk.floppy.IMDFloppyException;
import au.wildie.m68k.cromixfs.disk.floppy.VFDFloppyImage;
import au.wildie.m68k.cromixfs.disk.vfd.VFDImage;
import org.apache.commons.io.FileUtils;


public class CromixVFDFloppyDisk extends VFDFloppyImage {

    private final CromixFloppyInfo info;

    public CromixVFDFloppyDisk(VFDImage image, String formatLabel, PrintStream out) {
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
        FileUtils.writeByteArrayToFile(file, image.toBytes());
    }

    @Override
    public byte[] getInterleave() {
        return info.getInterleave();
    }

    @Override
    public byte[] getSuperBlock() throws IOException {
        // Will be in the first track, 128byte sectors
        byte[] block = new byte[512];

        for (int i = 0; i < 4; i++) {
            System.arraycopy(image.read(0, 0, 5 + i), 0, block, 128 * i, 128);
        }

        return block;
    }

    @Override
    public byte[] getBlock(int blockNumber) throws IOException {
        int c = getCylinderForBlock(blockNumber);
        int h = getHeadForBlock(blockNumber);
        int s = getSectorForBlock(blockNumber);

        int is = info.getInterleave()[0xFF & s] + 1;

        return image.read(c, h, is);
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
