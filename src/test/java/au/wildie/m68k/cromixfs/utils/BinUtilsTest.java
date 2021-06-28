package au.wildie.m68k.cromixfs.utils;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BinUtilsTest {

    @Test
    public void readDWord() {
        byte[] data = new byte[] {(byte)0x0, (byte)0x0, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0xFF, (byte)0x0};
        assertThat(BinUtils.readDWord(data, 2), is(0x010203FF));
    }

    @Test
    public void readWord() {
        byte[] data = new byte[] {(byte)0x0, (byte)0x0, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0xFF, (byte)0x0};
        assertThat(BinUtils.readWord(data, 2), is(0x0102));
        assertThat(BinUtils.readWord(data, 4), is(0x03FF));
    }

    @Test
    public void writeDWord() {
        byte[] data = new byte[10];
        BinUtils.writeDWord(0x010203FF, data, 2);
        assertThat(data[0], is((byte)0));
        assertThat(data[1], is((byte)0));
        assertThat(data[2], is((byte)0x01));
        assertThat(data[3], is((byte)0x02));
        assertThat(data[4], is((byte)0x03));
        assertThat(data[5], is((byte)0xFF));
        assertThat(data[6], is((byte)0));
        assertThat(data[7], is((byte)0));
        assertThat(data[8], is((byte)0));
        assertThat(data[9], is((byte)0));
    }


    @Test
    public void writeWord() {
        byte[] data = new byte[10];
        BinUtils.writeWord(0x03FF, data, 2);
        assertThat(data[0], is((byte)0));
        assertThat(data[1], is((byte)0));
        assertThat(data[2], is((byte)0x03));
        assertThat(data[3], is((byte)0xFF));
        assertThat(data[4], is((byte)0x0));
        assertThat(data[5], is((byte)0x0));
        assertThat(data[6], is((byte)0));
        assertThat(data[7], is((byte)0));
        assertThat(data[8], is((byte)0));
        assertThat(data[9], is((byte)0));
    }
}