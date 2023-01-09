package au.wildie.m68k.cromixfs.fs.cromix;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@Getter
@AllArgsConstructor
public class CromixFileSystemStats {
    private final Integer dcheckErrors;
    private final Integer iblockErrors;
    private final BlockStats blockStats;
    private final InodeStats inodeStats;

    public boolean hasErrors() {
        return Optional.ofNullable(dcheckErrors).map(d -> d != 0).orElse(false)
            || Optional.ofNullable(iblockErrors).map(d -> d != 0).orElse(false)
            || inodeStats.getErrorInodes() != 0;
    }
}
