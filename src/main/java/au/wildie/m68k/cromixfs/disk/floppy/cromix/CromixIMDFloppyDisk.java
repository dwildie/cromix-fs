package au.wildie.m68k.cromixfs.disk.floppy.cromix;

import au.wildie.m68k.cromixfs.disk.floppy.IMDFloppyException;
import au.wildie.m68k.cromixfs.disk.floppy.IMDFloppyImage;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.disk.imd.IMDSector;
import au.wildie.m68k.cromixfs.disk.imd.IMDTrack;
import org.apache.commons.lang3.StringUtils;

import java.io.*;

public class CromixIMDFloppyDisk extends IMDFloppyImage {

    private final CromixFloppyInfo info;
    private boolean uniform = false;

    public CromixIMDFloppyDisk(IMDImage image, PrintStream out) {
        super(image, out);

        int track0SectorSize = image.getTrack(0, 0).getSectorSize();
        int track1SectorSize = image.getTrack(0, 1).getSectorSize();
        if (track0SectorSize == track1SectorSize) {
            uniform = true;
            info = CromixFloppyInfo.getUniform(image.getCylinders(), image.getHeads(), track0SectorSize, image.getTrack(0, 0).getMode());
        } else {
            if (StringUtils.isBlank(super.getFormatLabel()) || super.getFormatLabel().charAt(0) != 'C') {
                throw new IMDFloppyException("Not a cromix disk");
            }

            info = CromixFloppyInfo.get(super.getFormatLabel());
        }
        out.format("Disk format: %s\n\n", getFormatLabel());
    }

    @Override
    public String getFormatLabel() {
        return uniform ? "Uniform" : super.getFormatLabel();
    }

    public void writeImage(String fileName, boolean interleaved) throws IOException {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }

        try (OutputStream out = new FileOutputStream(file)) {
            for (IMDTrack track : image.getTracks()) {
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

        if (uniform) {
            // super block is the second sector
            IMDSector zero = image.getSector(0,0,2);
            System.arraycopy(zero.getData(), 0, block, 0, zero.getData().length);
        } else {
            for (int i = 0; i < 4; i++) {
                System.arraycopy(image.getSector(0, 0, 5 + i).getData(), 0, block, 128 * i, 128);
            }
        }
        return block;
    }

    @Override
    public byte[] getBlock(int blockNumber) {
        int c = getCylinderForBlock(blockNumber);
        int h = getHeadForBlock(blockNumber);
        int s = getSectorForBlock(blockNumber);

        int is = (uniform ? s : info.getInterleave()[0xFF & s]) + 1;

        IMDSector sector = image.getSector(c,h,is);
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

//    @Override
//    public void checkSupported() {
//        if (diskDensity != DOUBLE) {
//            throw new RuntimeException(String.format("%s density disks are not supported", diskDensity));
//        }
//        if (diskSides != DiskSides.DOUBLE) {
//            throw new RuntimeException(String.format("%s sided disks are not supported", diskSides));
//        }
//    }
}
