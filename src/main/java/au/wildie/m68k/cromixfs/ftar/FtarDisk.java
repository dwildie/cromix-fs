package au.wildie.m68k.cromixfs.ftar;

import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.disk.imd.Sector;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FtarDisk {
    private final IMDImage image;
    private int nextBlockNumber = 0;

    public byte[] getNextBlock() {
        return getBlock(nextBlockNumber++);
    }

    public int skipBlocks(int skip) {
        nextBlockNumber += skip;
        return nextBlockNumber;
    }

    protected byte[] getBlock(int blockNumber) {
        int c = getCylinderForBlock(blockNumber);
        int h = getHeadForBlock(blockNumber);
        int s = getSectorForBlock(blockNumber);

        Sector sector = image.getSector(c,h,s+1);
        return sector.getData();
    }

    private int getCylinderForBlock(int block) {
        return (block) / (2 * getSectorsPerTrack());
    }

    private int getHeadForBlock(int block) {
        return ((block) / getSectorsPerTrack()) % getHeads();
    }

    private int getSectorForBlock(int block) {
        return (block) % getSectorsPerTrack();
    }

    private int getSectorsPerTrack() {
        return image.getTrack(0, 0).getSectorCount();
    }

    private int getHeads() {
        return image.getHeads();
    }

    public int getBlockSize() {
        return image.getTrack(0,0).getSectorSize();
    }
}
