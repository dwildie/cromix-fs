package au.wildie.m68k.cromixfs.disk.floppy.vfd;

import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;


public class VFDImageTest {

    @Test
    public void from() throws IOException {
        InputStream imdFile = this.getClass().getClassLoader().getResourceAsStream("imd/848CR162.IMD");
        assertThat(imdFile, notNullValue());

        IMDImage imdImage = new IMDImage(IOUtils.toByteArray(imdFile), System.out);
        assertThat(imdImage, notNullValue());

        VFDImage vfdImage = VFDImage.from(imdImage);
        assertThat(vfdImage, notNullValue());

        assertThat(vfdImage.getInfo(), notNullValue());
        assertThat(vfdImage.getInfo().getCylinders(), is(77));
        assertThat(vfdImage.getInfo().getHeads(), is(2));

        assertThat(vfdImage.getInfo().getFirst(), notNullValue());
        assertThat(vfdImage.getInfo().getFirst().getSectors(), is(26));
        assertThat(vfdImage.getInfo().getFirst().getSectorBytes(), is(128));
        assertThat(vfdImage.getInfo().getFirst().getOffset(), is(ImageInfo.SIZE));

        assertThat(vfdImage.getInfo().getRest(), notNullValue());
        assertThat(vfdImage.getInfo().getRest().getSectors(), is(16));
        assertThat(vfdImage.getInfo().getRest().getSectorBytes(), is(512));
        assertThat(vfdImage.getInfo().getRest().getOffset(), is(ImageInfo.SIZE + vfdImage.getInfo().getFirst().size()));
    }


    @Test
    public void toBytes() throws IOException {
        InputStream imdFile = this.getClass().getClassLoader().getResourceAsStream("imd/848CR162.IMD");
        assertThat(imdFile, notNullValue());

        IMDImage imdImage = new IMDImage(IOUtils.toByteArray(imdFile), System.out);
        assertThat(imdImage, notNullValue());

        VFDImage vfdImage = VFDImage.from(imdImage);
        assertThat(vfdImage, notNullValue());

        int expectedLength = ImageInfo.SIZE + vfdImage.getInfo().getFirst().size() + (vfdImage.getInfo().getCylinders() * vfdImage.getInfo().getHeads() - 1) * vfdImage.getInfo().getRest().size();

        byte[] raw = vfdImage.toBytes();
        assertThat(raw, notNullValue());
        assertThat(raw.length, is(expectedLength));
    }
}