package au.wildie.m68k.cromixfs.disk.imd;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static au.wildie.m68k.cromixfs.disk.imd.ImageException.CODE_ERROR;

@Getter
@Setter
public class IMDTrack {
    private int mode;
    private int cylinder;
    private int head;
    private int sectorCount;
    private int sectorSize;
    private int[] sectorMap;
    private int offset;
    private List<IMDSector> sectors =  new ArrayList<>();

    public IMDSector getSector(int sectorNumber) {
        return sectors.stream()
                .filter(sector -> sector.getNumber() == sectorNumber)
                .findFirst()
                .orElseThrow(() -> new ImageException(CODE_ERROR, String.format("Track cylinder %d, head %d: can't find sector %d", cylinder, head, sectorNumber)));
    }
}
