package au.wildie.m68k.cromixfs.disk.hxc;

import static au.wildie.m68k.cromixfs.disk.hxc.HFEHeader.ISOIBM_FM_ENCODING;
import static au.wildie.m68k.cromixfs.disk.hxc.HFEHeader.ISOIBM_MFM_ENCODING;
import static au.wildie.m68k.cromixfs.disk.hxc.HFEImage.SIZE_BLOCK;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class HFETrack {
    public static final int MARK_DELETED = 0xfb;

    public static int ADDRESS_CYLINDER = 0;
    public static int ADDRESS_HEAD = 1;
    public static int ADDRESS_SECTOR = 2;
    public static int ADDRESS_SIZE = 3;

    private final int cylinder;
    private final int head;
    private final int encoding;
    private final int bitRate;
    private final TrackEntry entry;
    private final byte[] content;

    private List<HFESector> sectors = new ArrayList<>();

    private int codeRate;
    private int cellCount;
    private int cellIndex;
    private int shiftRegister;
    private int readValue;
    private byte cellGroup;
    private boolean first = true;

    protected void read() {
        codeRate = bitRate / (isFMEncoding() ? 125 : 250);
        cellCount = entry.getLength() * 4; // A cell is 2 bits
        cellIndex = 0;

        int crc = 0;
        byte[] header = new byte[4];

        while (cellIndex < cellCount) {
            if (advanceToNextIDAccessMark()) {
                if (isMFMEncoding()) {
                    crc = 0xb230;
                    readByte();
                } else {
                    crc = 0xef21;
                }

                header[ADDRESS_CYLINDER] = readByte();  // Cylinder
                header[ADDRESS_HEAD] = readByte();  // Head
                header[ADDRESS_SECTOR] = readByte();  // Sector
                header[ADDRESS_SIZE] = readByte();  // Sector size, 0=128, 1=256, 2=512, 3=1024, etc.

                int expectedCRC = readBits(16);
                int calculatedCrc = calculateCRC(header, 0, header.length, crc);
                if (calculatedCrc != expectedCRC) {
                    System.out.printf("Header CRC error, expected 0x%04x, calculated 0x%04x", expectedCRC, calculatedCrc);
                }

                if (advanceToNextDataAccessMark()) {
                    int accessMark = readValue;

                    if (isMFMEncoding()) {
                        crc = (accessMark == MARK_DELETED) ? 0xe295 : 0xd2f6;
                    } else {
                        crc = (accessMark == MARK_DELETED) ? 0xbf84 : 0x8fe7;
                    }

                    byte[] sector = new byte[sectorSize(header[ADDRESS_SIZE])];
                    for (int i = 0; i < sector.length; i++) {
                        sector[i] = readByte();
                    }
                    sectors.add(new HFESector(header[ADDRESS_SECTOR], sector));
                }
            }
        }
    }

    protected static int sectorSize(byte val) {
        return 0x80 << val;
    }

    protected boolean isMFMEncoding() {
        return encoding == ISOIBM_MFM_ENCODING;
    }

    protected boolean isFMEncoding() {
        return encoding == ISOIBM_FM_ENCODING;
    }

    protected boolean advanceToNextIDAccessMark() {
        int count = isFMEncoding() ? 1 : 3;
        int accessMark = isFMEncoding() ? 0xf57e : 0x4489;

        while (cellIndex < cellCount && count != 0) {
            readBit();
            if (shiftRegister == accessMark) {
                count--;
            }
        }

        return (count == 0);
    }

    protected boolean advanceToNextDataAccessMark() {
        readValue = 0;
        int count = isFMEncoding() ? 1 : 3;
        if (isFMEncoding()) {
            while (cellIndex < cellCount && count != 0) {
                readValue = ((readValue << 1 ) | readBit()) & 0xff;
                if ((shiftRegister & 0xfffa) == 0xf56a) {
                    count--;
                }
            }
        } else {
            while (cellIndex < cellCount && count != 0) {
                readBit();
                if (shiftRegister == 0x4489) {
                    count--;
                }
                if (count == 0) {
                    readValue = readByte();  // read the ident field
                }
            }
        }

        return (count == 0);
    }

    private byte readByte() {
        return (byte)readBits(8);
    }

    private int readBits(int count) {
        readValue = 0;
        for (int i = 0; i < count; i++) {
            readValue <<= 1;
            readValue |= readBit();
        }
        return readValue;
    }

    private int readBit() {
        int value = 0;
        // FM images are shifted by one cell position
        if (first && (codeRate == 4)) {
            readNextCell();
        }
        int level = readNextCell();
        shiftRegister = ((shiftRegister << 1) | level) & 0xffff;
        if (codeRate == 4) {
            readNextCell();  // ignore the next cell
        }
        value = readNextCell();
        shiftRegister = ((shiftRegister << 1) | value) & 0xffff;
        if (codeRate == 4) {
            readNextCell();  // ignore the next cell
        }
        return value;
    }

    private int readNextCell() {
        if ((cellIndex % 8)==0) {
            int position = cellIndex / 8;
            int blockIndex = position / (SIZE_BLOCK / 2);
            int blockOffset = position % (SIZE_BLOCK / 2);
            int trackOffset = blockIndex * SIZE_BLOCK + head * (SIZE_BLOCK / 2) + blockOffset;
            if (trackOffset >= entry.getLength()) {
                cellIndex = cellCount;
                cellGroup = 0;
            } else {
                cellGroup = (byte)((content[entry.getOffset() * SIZE_BLOCK + trackOffset]) & 0xff);
            }
        }

        int value = cellGroup & 1;
        cellGroup >>= 1;
        cellIndex++;
        first = false;
        return value;
    }

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
