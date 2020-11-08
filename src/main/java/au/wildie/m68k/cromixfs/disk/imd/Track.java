package au.wildie.m68k.cromixfs.disk.imd;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Track {
    private int mode;
    private int cylinder;
    private int head;
    private int sectorCount;
    private int sectorSize;
    private int[] sectorMap;
    private int offset;
    private List<Sector> sectors =  new ArrayList<>();

    public Sector getSector(int sectorNumber) {
        return sectors.stream()
                .filter(sector -> sector.getNumber() == sectorNumber)
                .findFirst()
                .orElseThrow(() -> new ImageException(String.format("Track cylinder %d, head %d: can't find sector %d", cylinder, head, sectorNumber)));
    }
}
