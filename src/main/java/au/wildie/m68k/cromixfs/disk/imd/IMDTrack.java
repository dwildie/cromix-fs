package au.wildie.m68k.cromixfs.disk.imd;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static au.wildie.m68k.cromixfs.disk.imd.ImageException.CODE_ERROR;

@Getter
@Setter
@NoArgsConstructor
public class IMDTrack {
    private int mode;
    private int cylinder;
    private int head;
    private int sectorCount;
    private int sectorSize;
    private int[] sectorMap;
    private int offset;
    private List<IMDSector> sectors =  new ArrayList<>();

    public IMDTrack(int mode, int cylinder, int head, int sectorCount, int sectorSize, int offset) {
        this.mode = mode;
        this.cylinder = cylinder;
        this.head = head;
        this.sectorCount = sectorCount;
        this.sectorSize = sectorSize;
        this.offset = offset;

        sectorMap = new int[sectorCount];
        for (int i = 0; i < sectorCount; i++) {
            sectorMap[i] = i + 1;
            sectors.add(new IMDSector(i+ 1, offset + i * sectorSize, sectorSize));
        }
    }

    public IMDSector getSector(int sectorNumber) {
        return sectors.stream()
                .filter(sector -> sector.getNumber() == sectorNumber)
                .findFirst()
                .orElseThrow(() -> new ImageException(CODE_ERROR, String.format("Track cylinder %d, head %d: can't find sector %d", cylinder, head, sectorNumber)));
    }
}
