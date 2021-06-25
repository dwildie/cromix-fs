package au.wildie.m68k.cromixfs.utils;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class Int68000Test {
    @Test
    public void to2Bytes_0x1() {
        byte[] data = Int68000.to2Bytes(0x1);
        assertThat(data.length, is(2));
        assertThat(data[0], is((byte)0));
        assertThat(data[1], is((byte)1));
    }

    @Test
    public void to2Bytes_0x100() {
        byte[] data = Int68000.to2Bytes(0x100);
        assertThat(data.length, is(2));
        assertThat(data[0], is((byte)1));
        assertThat(data[1], is((byte)0));
    }

    @Test
    public void to4Bytes_0x1() {
        byte[] data = Int68000.to4Bytes(0x1);
        assertThat(data.length, is(4));
        assertThat(data[0], is((byte)0));
        assertThat(data[1], is((byte)0));
        assertThat(data[2], is((byte)0));
        assertThat(data[3], is((byte)1));
    }

    @Test
    public void to4Bytes_0x100() {
        byte[] data = Int68000.to4Bytes(0x100);
        assertThat(data.length, is(4));
        assertThat(data[0], is((byte)0));
        assertThat(data[1], is((byte)0));
        assertThat(data[2], is((byte)1));
        assertThat(data[3], is((byte)0));
    }

    @Test
    public void to4Bytes_0x10000() {
        byte[] data = Int68000.to4Bytes(0x10000);
        assertThat(data.length, is(4));
        assertThat(data[0], is((byte)0));
        assertThat(data[1], is((byte)1));
        assertThat(data[2], is((byte)0));
        assertThat(data[3], is((byte)0));
    }

    @Test
    public void to4Bytes_0x1000000() {
        byte[] data = Int68000.to4Bytes(0x1000000);
        assertThat(data.length, is(4));
        assertThat(data[0], is((byte)1));
        assertThat(data[1], is((byte)0));
        assertThat(data[2], is((byte)0));
        assertThat(data[3], is((byte)0));
    }
}