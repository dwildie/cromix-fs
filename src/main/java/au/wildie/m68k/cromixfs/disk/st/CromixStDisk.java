package au.wildie.m68k.cromixfs.disk.st;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.fs.FileSystem;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

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
        new FileSystem(this).list(out);
    }

    public void extract(String path) throws IOException {
        System.out.printf("Extracting to directory: %s\n", path);
        new FileSystem(this).extract(path);
    }

    @Override
    public byte[] getSuperBlock() {
        return getBlock(1);
    }

    @Override
    public byte[] getBlock(int block) {
        int c = getCylinderForBlock(0, block);
        int h = getHeadForBlock(block);
        int s = getSectorForBlock(block);
        return media[c][h][s];
    }

    public void checkSupported() {
        // yes
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
