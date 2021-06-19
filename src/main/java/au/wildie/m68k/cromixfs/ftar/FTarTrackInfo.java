package au.wildie.m68k.cromixfs.ftar;

import au.wildie.m68k.cromixfs.disk.hxc.HFETrack;
import au.wildie.m68k.cromixfs.disk.imd.IMDTrack;
import lombok.Getter;

import static au.wildie.m68k.cromixfs.ftar.FTarIMDDisk.FTAR_BLOCK_SIZE;

@Getter
public class FTarTrackInfo {
    private int blockCount;
    private int sectorCount;
    private int sectorSize;

    public static FTarTrackInfo from(IMDTrack track) {
        FTarTrackInfo info = new FTarTrackInfo();
        info.sectorCount = track.getSectorCount();
        info.sectorSize = track.getSectorSize();
        info.blockCount = info.sectorCount / (FTAR_BLOCK_SIZE / info.sectorSize);
        return info;
    }

    public static FTarTrackInfo from(HFETrack track) {
        FTarTrackInfo info = new FTarTrackInfo();
        info.sectorCount = track.getSectorCount();
        info.sectorSize = track.getSectorSize();
        info.blockCount = info.sectorCount / (FTAR_BLOCK_SIZE / info.sectorSize);
        return info;
    }
}
