package au.wildie.m68k.cromixfs.disk.st;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.fs.cromix.CromixFileSystem;

import java.io.*;

public class CromixStDisk implements DiskInterface {
    private static final int SECTOR_SIZE = 512; // Disks are always 512 byte sectors

    private final byte[][][][] media;
    private final STDCDiskInfo info;

    public CromixStDisk(String fileName) throws STDiskException {
        byte[] sector = new byte[SECTOR_SIZE];

        try (InputStream in = new FileInputStream(fileName)) {
            if (in.read(sector, 0, SECTOR_SIZE) != SECTOR_SIZE) {
                throw new STDiskException("Could not read sector 0");
            }
        } catch (IOException e) {
            throw new STDiskException(String.format("Error reading initial sector from file \"%s\"\n", fileName), e);
        }

        info = new STDCDiskInfo(sector);
        if (info.getBytesPerSector() != SECTOR_SIZE) {
            throw new STDiskException(String.format("Unexpected drive geometry, sector size is %d", info.getBytesPerSector()));
        }

        // Allocate space for the media
        media = new byte[info.getCylinderCount()][info.getSurfaceCount()][info.getSectorsPerTrack()][info.getBytesPerSector()];

        try (InputStream in = new FileInputStream(fileName)) {
            for (int c = 0; c < info.getCylinderCount(); c++) {
                for (int h = 0; h < info.getSurfaceCount(); h++) {
                    for (int s = 0; s < info.getSectorsPerTrack(); s++) {
                        try {
                            int bytesRead = in.read(media[c][h][s]);
                            if (bytesRead != SECTOR_SIZE) {
                                throw new STDiskException(String.format("Could only read %d bytes for sector: cylinder=%d, surface=%d, sector=%d", bytesRead, c, h, s));
                            }
                        } catch (IOException e) {
                            throw new STDiskException(String.format("Failed to sector: cylinder=%d, surface=%d, sector=%d", c, h, s), e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new STDiskException(String.format("Error reading disk image file \"%s\"\n", fileName), e);
        }
    }


    public void list(PrintStream out) throws IOException {
        new CromixFileSystem(this).list(out);
    }

    public void extract(String path, PrintStream out) throws IOException {
        System.out.printf("Extracting to directory: %s\n", path);
        new CromixFileSystem(this).extract(path, out);
    }


    public void check(PrintStream out) throws IOException {
        new CromixFileSystem(this).check(out);
    }

    @Override
    public byte[] getSuperBlock() {
        return getBlock(1);
    }

    @Override
    public void flushSuperBlock(byte[] data) {
        // TODO
    }

    @Override
    public byte[] getBlock(int block) {
        if (block == 0x0095) {
            System.out.print("stop");
        }
        int c = getCylinderForBlock(0, block);
        int h = getHeadForBlock(block);
        int s = getSectorForBlock(block);
        return media[c][h][s];
    }

    @Override
    public void writeImage(File file, boolean interleaved) throws IOException {
        throw new IOException("Unimplemented operation");
    }

    @Override
    public void persist(OutputStream archive) throws IOException {
        // TODO
    }

    @Override
    public String getFormatLabel() {
        return null;
    }

    @Override
    public Integer getTrackCount() {
        return null;
    }

    @Override
    public Integer getTrackCount(int head) {
        return null;
    }

    @Override
    public Integer getSectorErrorCount() {
        return null;
    }

    private int getCylinderForBlock(int unit, int block) {
        return block / (info.getSurfaceCount() * info.getSectorsPerTrack()) + getStartingCylinder(unit);
    }

    private int getStartingCylinder(int unit) {
        return 1; // Changes when partition table is present
    }

    private int getHeadForBlock(int block) {
        return (block / info.getSectorsPerTrack()) % info.getSurfaceCount();
    }

    private int getSectorForBlock(int block) {
        return (block % info.getSectorsPerTrack());
    }

}
