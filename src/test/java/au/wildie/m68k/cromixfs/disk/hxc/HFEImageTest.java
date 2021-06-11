package au.wildie.m68k.cromixfs.disk.hxc;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class HFEImageTest {

    @Test
    public void test() throws IOException {
        InputStream src = this.getClass().getClassLoader().getResourceAsStream("hfe/848CR162.hfe");
        HFEImage image = HFEImage.from(src);
        assertThat(image, notNullValue());

        byte[] data = image.read(0, 0, 1);
        data = image.read(0, 1, 1);
    }
}