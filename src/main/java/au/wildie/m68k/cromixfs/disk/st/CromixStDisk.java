package au.wildie.m68k.cromixfs.disk.st;

import au.wildie.m68k.cromixfs.disk.DiskInterface;

public class CromixStDisk implements DiskInterface {
    public CromixStDisk(String fileName) {

    }
    @Override
    public byte[] getSuperBlock() {
        return new byte[0];
    }

    @Override
    public byte[] getBlock(int blockNumber) {
        return new byte[0];
    }
}
