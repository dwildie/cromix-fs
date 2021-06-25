package au.wildie.m68k.cromixfs.fs.cromix;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CromixFileSystemStats {
    private final BlockStats blockStats;
    private final InodeStats inodeStats;
}
