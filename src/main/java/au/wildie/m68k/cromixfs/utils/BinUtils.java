package au.wildie.m68k.cromixfs.utils;

public class BinUtils {
    public static int readDWord(byte[] data, int offset) {
        return (((((0xFF & data[offset]) << 8) + (0xFF & data[offset + 1]) << 8) + (0xFF & data[offset + 2])) << 8) + (0xFF & data[offset + 3]);
    }

    public static int readWord(byte[] data, int offset) {
        return ((0xFF & data[offset]) << 8) + (0xFF & data[offset + 1]);
    }

    public static String readString(byte[] data, int offset) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; data[offset + i] != 0; i++) {
            str.append((char) data[offset + i]);
        }
        return str.toString();
    }

    public static void writeString(String str, byte[] data, int offset, int max) {
        for (int i = 0; i < str.length(); i++) {
            if (i < (max - 1)) {
                data[offset + i] = (byte) str.charAt(i);
            }
        }
        data[offset + str.length()] = 0;
    }

    public static void writeString(String str, byte[] data, int offset) {
        for (int i = 0; i < str.length(); i++) {
            data[offset + i] = (byte) str.charAt(i);
        }
    }

    public static int asciiToInt(byte[] data, int offset, int size) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < size; i++) {
            value.append((char) data[offset + i]);
        }
        return Integer.parseInt(value.toString());
    }

    public static int asciiOctalToInt(byte[] data, int offset, int size) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < size; i++) {
            value.append((char) data[offset + i]);
        }
        return Integer.parseInt(value.toString().trim(), 8);
    }

    public static void asciiIntToOctal(int value, byte[] data, int offset, int size) {
        String str = String.format("%" + (size - 1) + "o ", value);
        writeString(str, data, offset);
    }

    public static long asciiOctalToLong(byte[] data, int offset, int size) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < size; i++) {
            value.append((char) data[offset + i]);
        }
        return Long.parseLong(value.toString().trim(), 8);
    }

    public static void asciiLongToOctal(long value, byte[] data, int offset, int size) {
        String str = String.format("%" + (size - 1) + "o ", value);
        writeString(str, data, offset);
    }

}
