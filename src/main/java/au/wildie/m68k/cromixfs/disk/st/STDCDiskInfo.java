package au.wildie.m68k.cromixfs.disk.st;

import lombok.Getter;

@Getter
public class STDCDiskInfo {
    private int cylinderCount;          // 2 bytes
    private int alternateTrackCount;    // 2 bytes
    private int surfaceCount;           // 1 byte
    private int sectorsPerTrack;        // 1 byte
    private int bytesPerSector;         // 2 bytes
    private int startOfAltTrackTable;   // 2 bytes
    private int startCylinder;          // 2 bytes
    private int altTrackCylinder;       // 2 bytes
    private int startOfPartitionTable;  // 2 bytes
    private byte[] hardDiskIdentifier;  // 4 bytes
    private int writePrecompCylinder;   // 2 bytes

    public STDCDiskInfo(byte[][][][] media) {
        this(media[0][0][0]);
    }

    public STDCDiskInfo(byte[] sector) {
        cylinderCount =         ((0xFF & sector[0x68]) << 8) + (0xFF & sector[0x69]);
        alternateTrackCount =   ((0xFF & sector[0x6A]) << 8) + (0xFF & sector[0x6B]);
        surfaceCount =           (0xFF & sector[0x6C]);
        sectorsPerTrack =        (0xFF & sector[0x6D]);
        bytesPerSector =        ((0xFF & sector[0x6E]) << 8) + (0xFF & sector[0x6F]);
        startOfAltTrackTable =  ((0xFF & sector[0x70]) << 8) + (0xFF & sector[0x71]);
        startCylinder =         ((0xFF & sector[0x72]) << 8) + (0xFF & sector[0x73]);
        altTrackCylinder =      ((0xFF & sector[0x74]) << 8) + (0xFF & sector[0x75]);
        startOfPartitionTable = ((0xFF & sector[0x76]) << 8) + (0xFF & sector[0x77]);
        hardDiskIdentifier =    new byte[]{sector[0x78],sector[0x79],sector[0x7A],sector[0x7B]};
        writePrecompCylinder =  ((0xFF & sector[0x7C]) << 8) + (0xFF & sector[0x7D]);
    }
}
