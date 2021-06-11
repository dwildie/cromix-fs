package au.wildie.m68k.cromixfs.disk.hxc;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

public class HFETrackTest {

    @Test
    public void sectorSize() {
        assertThat(HFETrack.sectorSize((byte)0), is(128));
        assertThat(HFETrack.sectorSize((byte)1), is(256));
        assertThat(HFETrack.sectorSize((byte)2), is(512));
    }
}