package au.wildie.m68k.cromixfs.disk.floppy.cdos;

import au.wildie.m68k.cromixfs.disk.floppy.IMDFloppyException;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskDensity.DOUBLE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskDensity.SINGLE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSize.LARGE;
import static au.wildie.m68k.cromixfs.disk.floppy.cromix.DiskSize.SMALL;

@Getter
@AllArgsConstructor
public class CDOSFloppyInfo {
    private static final byte[] x8d = {0,6,12,2,8,14,4,10,1,7,13,3,9,15,5,11}; /* Cromix 8" DD */
    private static final byte[] x8s = {0,3,6,1,4,7,2,5};                       /* Cromix 8" SD */
    private static final byte[] x5d = {0,4,8,2,6,1,5,9,3,7};                   /* Cromix 5" DD */
    private static final byte[] x5s = {0,2,4,1,3};                             /* Cromix 5" SD */

    private final int cylinders;
    private final int heads;
    private final int sectorsPerTrack;
    private final int sectorsFirstTrack;
    private final int bytesPerSector;
    private final int[] fileArea;
    private final byte[] interleave;

    public static CDOSFloppyInfo get(DiskSize diskSize, DiskSides diskSides, DiskDensity diskDensity) {
        if (diskSize == LARGE) {
            if (diskDensity == DOUBLE) {
                // First track: 26 * 128 = 3328 bytes, 0x0D00
                // Other tracks: 16 * 512 = 8192 bytes, 0x2000
                return new CDOSFloppyInfo(77, 2, 16, 26, 512, new int[] {1,0}, x8d);
            }
            if (diskDensity == SINGLE) {
                // First track: 26 * 128 = 3328 bytes, 0x0D00
                // Other tracks:  8 * 512 = 4096 bytes, 0x1000
                return new CDOSFloppyInfo(77, 2, 8, 26, 512, new int[] {1,0}, x8s);
            }
        }
        if (diskSize == SMALL) {
            if (diskDensity == DOUBLE) {
                // First track: 18 * 128 = 2304 bytes, 0x0900
                // Other tracks: 10 * 512 = 5120 bytes, 0x1400
                return new CDOSFloppyInfo(40, 2, 10, 18, 512, new int[] {1,0}, x5d);
            }
            if (diskDensity == SINGLE) {
                // First track: 18 * 128 = 2304 bytes, 0x0900
                // Other tracks:  5 * 512 = 2560 bytes, 0x0700
                return new CDOSFloppyInfo(40, 2,  5, 18, 512, new int[] {1,0}, x5s);
            }
        }
        throw new IMDFloppyException(String.format("Unsupported CDOS disk format: size %s, sides %s, density %s", diskSize, diskSides, diskDensity));
    }
}
