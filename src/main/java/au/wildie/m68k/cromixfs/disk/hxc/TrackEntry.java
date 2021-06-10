package au.wildie.m68k.cromixfs.disk.hxc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TrackEntry {
    private int offset;
    private int length;
}
