package au.wildie.m68k.cromixfs.fs;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CromixFileSystemStats {
    private final CromixBlockStats blockStats;
    private final CromixInodeStats inodeStats;
}
