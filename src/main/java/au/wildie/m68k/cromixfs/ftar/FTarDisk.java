package au.wildie.m68k.cromixfs.ftar;

import au.wildie.m68k.cromixfs.disk.*;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.CromixFloppyInfo;
import au.wildie.m68k.cromixfs.disk.imd.ImageException;
import org.apache.commons.lang3.StringUtils;

import java.io.OutputStream;
import java.io.PrintStream;

import static au.wildie.m68k.cromixfs.disk.imd.ImageException.CODE_END_OF_DISK;

public abstract class FTarDisk implements DiskInfo {
    public static final int FTAR_BLOCK_SIZE = 512;

    protected DiskImage image;
    protected final FTarTrackInfo[] trackInfo = new FTarTrackInfo[2];
    protected CromixFloppyInfo info = null;
    protected final Boolean uniform;
    protected int initialBlockNumber = 0;
    protected int currentBlockNumber;
    protected String formatLabel = null;

    public FTarDisk(DiskImage image, FTarTrackInfo track0, FTarTrackInfo track1, PrintStream out) {
        this.image = image;
        trackInfo[0] = track0;
        trackInfo[1] = track1;

        uniform = trackInfo[0].getSectorSize() == trackInfo[1].getSectorSize();
        if (!uniform) {
            // Read the disk label
            DiskSector zero = image.getSector(0, 0, 1);
            if (zero.isValid()) {
                formatLabel = CromixFloppyInfo.getFormatLabel(zero.getData());
            }
            if (!zero.isValid() || StringUtils.isBlank(formatLabel)) {
                // Sector is not available or no label, generate label from disk params
                formatLabel = "C";
                if (image.getCylinders() == 77) {
                    // Large
                    formatLabel += "L";
                    formatLabel += image.getHeads() == 2 ? "DS" : "SS";
                    formatLabel += trackInfo[1].getSectorCount() == 16 ? "DD" : "SD";
                } else {
                    // Small
                    formatLabel += "S";
                    formatLabel += image.getHeads() == 2 ? "DS" : "SS";
                    formatLabel += trackInfo[1].getSectorCount() == 10 ? "DD" : "SD";
                }
                formatLabel += "!";
            }

            initialBlockNumber = 1;
            out.printf("Cromemco Disk label: \"%s\"\n\n", formatLabel);
            info = CromixFloppyInfo.get(formatLabel);
        }
        currentBlockNumber = initialBlockNumber - 1;
    }

    @Override
    public String getFormatLabel() {
        return uniform ? "UNIFORM" : formatLabel;
    }

    @Override
    public Integer getTrackCount() {
        return image.getCylinders() * image.getHeads();
    }

    @Override
    public Integer getTrackCount(int head) {
        return image.getCylinders();
    }

    public void reset() {
        currentBlockNumber = initialBlockNumber - 1;
    }

    public int getBlockCount() {
        int count = trackInfo[0].getBlockCount();
        count += (image.getCylinders() * image.getHeads() - 1) * trackInfo[1].getBlockCount();
        return count;
    }

    public void skipBlocks(int skip) {
        currentBlockNumber += skip;
    }

    public int getCurrentBlockNumber() {
        return currentBlockNumber;
    }

    public int getBlockSize() {
        return FTAR_BLOCK_SIZE;
    }

    public byte[] getNextBlock() throws SectorInvalidException {
        if (currentBlockNumber + 1 >= getBlockCount()) {
            throw new ImageException(CODE_END_OF_DISK, String.format("Block %d does not exist", currentBlockNumber + 1));
        }

        return getBlock(++currentBlockNumber);
    }

    protected byte[] getBlock(int block) throws SectorInvalidException {
        int c = getCylinderForBlock(block);
        int h = getHeadForBlock(block);
        int s = getSectorForBlock(block);

        byte[] data = new byte[FTAR_BLOCK_SIZE];
        FTarTrackInfo tInfo = block < trackInfo[0].getBlockCount() ? trackInfo[0] : trackInfo[1];
        for (int i = 0; i < (FTAR_BLOCK_SIZE / tInfo.getSectorSize()); i++) {
            int sectorNumber = (info != null && block >= trackInfo[0].getBlockCount()) ? (info.getInterleave()[0xFF & (s + i)] + 1) : (s + i + 1);
            DiskSector sector = image.getSector(c, h, sectorNumber);
            if (!sector.isValid()) {
                throw new SectorInvalidException(c, h, sectorNumber);
            }
            System.arraycopy(sector.getData(), 0, data, tInfo.getSectorSize() * i, tInfo.getSectorSize());
        }
        return data;
    }

    public void setNextBlock(byte[] data) throws SectorInvalidException {
        if (currentBlockNumber + 1 >= getBlockCount()) {
            throw new ImageException(CODE_END_OF_DISK, String.format("Block %d does not exist", currentBlockNumber + 1));
        }

        setBlock(++currentBlockNumber, data);
    }

    protected void setBlock(int block, byte[] data) throws SectorInvalidException {
        int c = getCylinderForBlock(block);
        int h = getHeadForBlock(block);
        int s = getSectorForBlock(block);

        FTarTrackInfo tInfo = block < trackInfo[0].getBlockCount() ? trackInfo[0] : trackInfo[1];
        for (int i = 0; i < (FTAR_BLOCK_SIZE / tInfo.getSectorSize()); i++) {
            int sectorNumber = (info != null && block >= trackInfo[0].getBlockCount()) ? (info.getInterleave()[0xFF & (s + i)] + 1) : (s + i + 1);
            DiskSector sector = image.getSector(c, h, sectorNumber);
            if (!sector.isValid()) {
                throw new SectorInvalidException(c, h, sectorNumber);
            }
            System.arraycopy(data, tInfo.getSectorSize() * i, sector.getData(), 0, tInfo.getSectorSize());
        }
    }


    protected int getCylinderForBlock(int block) {
        if (block < getCylinderBlockCount(0)) {
            return 0;
        }
        return 1 + ((block - getCylinderBlockCount(0)) / (2 * trackInfo[1].getSectorCount()));
    }

    protected int getHeadForBlock(int block) {
        if (block < trackInfo[0].getBlockCount()) {
            return 0;
        }
        if (block < (trackInfo[0].getBlockCount() + trackInfo[1].getBlockCount())) {
            return 1;
        }
        return (1 + (block - trackInfo[0].getBlockCount()) / trackInfo[1].getSectorCount()) % getHeads();
    }

    protected int getSectorForBlock(int block) {
        if (block < trackInfo[0].getBlockCount()) {
            return block * FTAR_BLOCK_SIZE / trackInfo[0].getSectorSize();
        }
        return (block - trackInfo[0].getBlockCount()) % trackInfo[1].getSectorCount();
    }

    protected int getHeads() {
        return image.getHeads();
    }

    public int getCylinderBlockCount(int cyl) {
        return cyl == 0 ? trackInfo[0].getBlockCount() + trackInfo[1].getBlockCount() : 2 * trackInfo[1].getBlockCount();
    }

    public void persist(OutputStream archive) {
        image.persist(archive);
    }
}
