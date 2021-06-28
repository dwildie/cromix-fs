package au.wildie.m68k.cromixfs.fs.cromix;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class BlockUsageItem {
    private final int number;
    private boolean file;
    private boolean directory;
    private boolean onFreeList;

    public boolean isUsed() {
        return file || directory;
    }

    public boolean isOrphaned() {
        return !file && !directory && !onFreeList;
    }

    public boolean isDuplicate() {
        return ((file ? 1 : 0) + (directory ? 1 : 0) + (onFreeList ? 1 : 0)) > 1;
    }

    public void setFile() {
        file = true;
    }

    public void setDirectory() {
        directory = true;
    }

    public void setOnFreeList() { onFreeList = true; }
}
