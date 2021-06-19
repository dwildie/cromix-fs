package au.wildie.m68k.cromixfs.ftar;

import au.wildie.m68k.cromixfs.disk.imd.IMDTrack;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class FTarTrackInfoTest {

    @Test
    public void fromSD() {
        IMDTrack track = new IMDTrack();
        track.setSectorCount(26);
        track.setSectorSize(128);

        FTarTrackInfo info = FTarTrackInfo.from(track);
        assertThat(info, notNullValue());

        assertThat(info.getSectorCount(), is(26));
        assertThat(info.getSectorSize(), is(128));
        assertThat(info.getBlockCount(), is(6));
    }

    @Test
    public void fromDD() {
        IMDTrack track = new IMDTrack();
        track.setSectorCount(16);
        track.setSectorSize(512);

        FTarTrackInfo info = FTarTrackInfo.from(track);
        assertThat(info, notNullValue());

        assertThat(info.getSectorCount(), is(16));
        assertThat(info.getSectorSize(), is(512));
        assertThat(info.getBlockCount(), is(16));
    }
}