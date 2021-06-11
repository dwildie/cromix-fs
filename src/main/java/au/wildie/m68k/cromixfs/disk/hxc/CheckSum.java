package au.wildie.m68k.cromixfs.disk.hxc;

public class CheckSum {
    public static int calculateCRC(byte[] data, int offset, int length, int initialCRC) {
        // Big-endian, x^16+x^12+x^5+1 = (1) 0001 0000 0010 0001 = 0x1021
        int crc = initialCRC;

        for (int i = 0; i < length; i++) {
            crc = (crc ^ (data[i + offset] << 8));
            for (int j = 0; j <= 7; j++)	{
                if ((crc & 0x8000) == 0x8000) {
                    crc = ((crc << 1) ^ 0x1021);
                }
                else {
                    crc = (crc << 1);
                }
            }
        }

        return (crc & 0xffff);
    }
}
