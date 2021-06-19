package au.wildie.m68k.cromixfs.disk;

public interface DiskInfo {
    String getFormatLabel();
    Integer getTrackCount();
    Integer getTrackCount(int head);
    Integer getSectorErrorCount();
}
