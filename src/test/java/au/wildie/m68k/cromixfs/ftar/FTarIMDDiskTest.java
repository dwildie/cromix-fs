package au.wildie.m68k.cromixfs.ftar;

import au.wildie.m68k.cromixfs.CromemcoTest;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.disk.imd.IMDTrack;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FTarIMDDiskTest extends CromemcoTest {

    @Test
    public void trackDD() {
        IMDImage image = mock(IMDImage.class);
        when(image.getHeads()).thenReturn(2);

        IMDTrack track0 = new IMDTrack();
        track0.setSectorCount(26);
        track0.setSectorSize(128);
        when(image.getTrack(0, 0)).thenReturn(track0);

        IMDTrack track1 = new IMDTrack();
        track1.setSectorCount(16);
        track1.setSectorSize(512);
        when(image.getTrack(0, 1)).thenReturn(track1);

        FTarIMDDisk disk = new FTarIMDDisk(image, getDummyPrintStream());

        assertThat(disk.getCylinderForBlock(0), is(0));
        assertThat(disk.getHeadForBlock(0), is(0));
        assertThat(disk.getSectorForBlock(0), is(0));

        assertThat(disk.getCylinderForBlock(1), is(0));
        assertThat(disk.getHeadForBlock(1), is(0));
        assertThat(disk.getSectorForBlock(1), is(4));

        assertThat(disk.getCylinderForBlock(2), is(0));
        assertThat(disk.getHeadForBlock(2), is(0));
        assertThat(disk.getSectorForBlock(2), is(8));

        assertThat(disk.getCylinderForBlock(5), is(0));
        assertThat(disk.getHeadForBlock(5), is(0));
        assertThat(disk.getSectorForBlock(5), is(20));

        assertThat(disk.getCylinderForBlock(6), is(0));
        assertThat(disk.getHeadForBlock(6), is(1));
        assertThat(disk.getSectorForBlock(6), is(0));

        assertThat(disk.getCylinderForBlock(7), is(0));
        assertThat(disk.getHeadForBlock(7), is(1));
        assertThat(disk.getSectorForBlock(7), is(1));

        assertThat(disk.getCylinderForBlock(8), is(0));
        assertThat(disk.getHeadForBlock(8), is(1));
        assertThat(disk.getSectorForBlock(8), is(2));

        assertThat(disk.getCylinderForBlock(19), is(0));
        assertThat(disk.getHeadForBlock(19), is(1));
        assertThat(disk.getSectorForBlock(19), is(13));

        assertThat(disk.getCylinderForBlock(20), is(0));
        assertThat(disk.getHeadForBlock(20), is(1));
        assertThat(disk.getSectorForBlock(20), is(14));

        assertThat(disk.getCylinderForBlock(21), is(0));
        assertThat(disk.getHeadForBlock(21), is(1));
        assertThat(disk.getSectorForBlock(21), is(15));

        assertThat(disk.getCylinderForBlock(22), is(1));
        assertThat(disk.getHeadForBlock(22), is(0));
        assertThat(disk.getSectorForBlock(22), is(0));

        assertThat(disk.getCylinderForBlock(23), is(1));
        assertThat(disk.getHeadForBlock(23), is(0));
        assertThat(disk.getSectorForBlock(23), is(1));

        assertThat(disk.getCylinderForBlock(37), is(1));
        assertThat(disk.getHeadForBlock(37), is(0));
        assertThat(disk.getSectorForBlock(37), is(15));

        assertThat(disk.getCylinderForBlock(38), is(1));
        assertThat(disk.getHeadForBlock(38), is(1));
        assertThat(disk.getSectorForBlock(38), is(0));

        assertThat(disk.getCylinderForBlock(53), is(1));
        assertThat(disk.getHeadForBlock(53), is(1));
        assertThat(disk.getSectorForBlock(53), is(15));

        assertThat(disk.getCylinderForBlock(54), is(2));
        assertThat(disk.getHeadForBlock(54), is(0));
        assertThat(disk.getSectorForBlock(54), is(0));
    }
}