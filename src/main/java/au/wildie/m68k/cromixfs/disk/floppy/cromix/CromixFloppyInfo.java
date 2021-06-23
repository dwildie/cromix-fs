package au.wildie.m68k.cromixfs.disk.floppy.cromix;

import au.wildie.m68k.cromixfs.disk.floppy.IMDFloppyException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskDensity.DOUBLE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskDensity.SINGLE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSize.LARGE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSize.SMALL;

@Getter
@AllArgsConstructor
public class CromixFloppyInfo {

    public static final int LABEL_START = 120;
    public static final int LABEL_END = 127;

    private static final byte[] x8d = {0,6,12,2,8,14,4,10,1,7,13,3,9,15,5,11}; /* Cromix 8" DD */
    private static final byte[] x8s = {0,3,6,1,4,7,2,5};                       /* Cromix 8" SD */
    private static final byte[] x5d = {0,4,8,2,6,1,5,9,3,7};                   /* Cromix 5" DD */
    private static final byte[] x5s = {0,2,4,1,3};                             /* Cromix 5" SD */

    private static final byte[] secTrkLSD = {26, 15,  8,  4};
    private static final byte[] secTrkLDD = {52, 26, 15,  8};
    private static final byte[] secTrkSSD = {16,  9,  5,  2};
    private static final byte[] secTrkSDD = {24, 16,  9,  5};

    private final int cylinders;
    private final int heads;
    private final int sectorsPerTrack;
    private final int sectorsFirstTrack;
    private final int bytesPerSector;
    private final int bytesPerSectorFirstTrack;
    private final int blockOffset;
    private final byte[] interleave;

    // First track: 26 * 128 = 3328 bytes, 0x0D00, Other tracks: 16 * 512 = 8192 bytes, 0x2000
    public static CromixFloppyInfo LARGE_DOUBLE_DENSITY = new CromixFloppyInfo(77, 2, 16, 26, 512, 128,10, x8d);

    // First track: 26 * 128 = 3328 bytes, 0x0D00, Other tracks:  8 * 512 = 4096 bytes, 0x1000
    public static CromixFloppyInfo LARGE_SINGLE_DENSITY = new CromixFloppyInfo(77, 2, 8, 26, 512, 128, 18, x8s);

    // First track: 18 * 128 = 2304 bytes, 0x0900, Other tracks: 10 * 512 = 5120 bytes, 0x1400
    public static CromixFloppyInfo SMALL_DOUBLE_DENSITY = new CromixFloppyInfo(40, 2, 10, 18, 512, 128, 6, x5d);

    // First track: 18 * 128 = 2304 bytes, 0x0900, Other tracks:  5 * 512 = 2560 bytes, 0x0700
    public static CromixFloppyInfo SMALL_SINGLE_DENSITY = new CromixFloppyInfo(40, 2,  5, 18, 512, 128, 13, x5s);

    public static String getFormatLabel(byte[] sector) {
        return new String(Arrays.copyOfRange(sector, LABEL_START, LABEL_END)).replaceAll("\\P{InBasic_Latin}", " ").trim();
    }

    public static void setFormatLabel(String formatLabel, byte[] sector) {
        for (int i = 0; i < (LABEL_END - LABEL_START); i++) {
            if (i < formatLabel.length()) {
                sector[LABEL_START + i] = (byte) formatLabel.charAt(i);
            }
        }
    }

    public static CromixFloppyInfo get(String label) {
        DiskSize diskSize = label.charAt(1) == 'L' ? LARGE : SMALL;
        DiskSides diskSides = label.charAt(2) == 'D' ? DiskSides.DOUBLE : DiskSides.SINGLE;
        DiskDensity diskDensity = label.charAt(4) == 'D' ? DOUBLE : SINGLE;
        return get(diskSize, diskSides, diskDensity);
    }

    public static CromixFloppyInfo getUniform(int cylinders, int heads, int sectorSize, int mode) {

        int sectors;
        if (cylinders == 77) {
            // Large
            sectors = secTrkLDD[2];
        } else {
            // Small
            sectors = secTrkSDD[2];
        }
        return new CromixFloppyInfo(cylinders, heads, sectors, sectors, sectorSize, sectorSize,0,null);
    }

    public static CromixFloppyInfo get(DiskSize diskSize, DiskSides diskSides, DiskDensity  diskDensity) {
        if (diskSize == LARGE) {
            if (diskDensity == DOUBLE) {
                return LARGE_DOUBLE_DENSITY;
            }
            if (diskDensity == SINGLE) {
                return LARGE_SINGLE_DENSITY;
            }
        }
        if (diskSize == SMALL) {
            if (diskDensity == DOUBLE) {
                return SMALL_DOUBLE_DENSITY;
            }
            if (diskDensity == SINGLE) {
                return SMALL_SINGLE_DENSITY;
            }
        }
        throw new IMDFloppyException(String.format("Unsupported Cromix disk format: size %s, sides %s, density %s", diskSize, diskSides, diskDensity));
    }
}
