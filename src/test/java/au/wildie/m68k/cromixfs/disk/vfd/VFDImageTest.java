package au.wildie.m68k.cromixfs.disk.vfd;

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
    public void read() throws IOException {
        InputStream imdFile = this.getClass().getClassLoader().getResourceAsStream("imd/848CR162.IMD");
        assertThat(imdFile, notNullValue());

        IMDImage imdImage = new IMDImage(IOUtils.toByteArray(imdFile), System.out);
        assertThat(imdImage, notNullValue());

        VFDImage vfdImage = VFDImage.from(imdImage);
        assertThat(vfdImage, notNullValue());

        imdImage.getTracks().forEach(track -> track.getSectors().forEach(sector -> {
                String desc = String.format("cyl=%2d, head=%d, sector=%2d: ", track.getCylinder(), track.getHead(), sector.getNumber() - 1);
                System.out.printf("%soffsets: IMD=0x%x, VFD=0x%x\n",
                        desc,
                        sector.getSrcOffset(),
                        vfdImage.getTrackAndOffset(track.getCylinder(), track.getHead(), sector.getNumber()).getRight());

                byte[] imdData = sector.getData();
                byte[] vfdData = null;
                try {
                    vfdData = vfdImage.read(track.getCylinder(), track.getHead(), sector.getNumber());
                } catch (IOException e) {
                    assertThat(desc + e.getMessage(), vfdData, is(not(anything())));
                }
                assertThat(desc + "imdData should not be null", imdData, notNullValue());
                assertThat(desc + "vfdData should not be null", vfdData, notNullValue());
                assertThat(desc + "sectors should be the same size", vfdData.length, is(imdData.length));
                for (int i = 0; i < imdData.length; i++) {
                    assertThat(desc + String.format("byte[%d] is different", i), vfdData[i], is(imdData[i]));
                }
        }));
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