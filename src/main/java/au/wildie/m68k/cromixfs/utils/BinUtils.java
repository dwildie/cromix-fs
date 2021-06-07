package au.wildie.m68k.cromixfs.utils;

public class BinUtils {
    public static int readDWord(byte[] data, int offset) {
        return (((((0xFF & data[offset]) << 8) + (0xFF & data[offset + 1]) << 8) + (0xFF & data[offset + 2])) << 8) + (0xFF & data[offset + 3]);
    }

    public static int readWord(byte[] data, int offset) {
        return ((0xFF & data[offset]) << 8) + (0xFF & data[offset + 1]);
    }

    public static String readString(byte data[], int offset) {
        String str = "";

        for (int i = 0; data[offset + i] != 0; i++) {
            str += (char)data[offset + i];
        }

        return str;
    }

    public static int asciiToInt(byte data[], int offset, int size) {
        String value = "";
        for (int i = 0; i < size; i++) {
            value += (char)data[offset + i];
        }
        return Integer.parseInt(value);
    }
    public static long asciiOctalToLong(byte data[], int offset, int size) {
        String value = "";
        for (int i = 0; i < size; i++) {
            value += (char)data[offset + i];
        }
        return Long.parseLong(value.trim(), 8);
    }
}
