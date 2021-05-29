package au.wildie.m68k.cromixfs.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Int68000 {
    public static byte[] to4Bytes(int i) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(i).array();
    }
    public static byte[] to2Bytes(int i) {
        return ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort((short)i).array();
    }
}
