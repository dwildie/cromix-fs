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
    private int lastDataBit;
    private boolean first = true;

    protected void read() {
        codeRate = bitRate / (isFMEncoding() ? 125 : 250);
        cellCount = entry.getLength() * 4; // A cell is 2 bits
        cellIndex = 0;

        int initialCRC;
        byte[] header = new byte[4];

        while (cellIndex < cellCount) {
            if (advanceToNextIDAccessMark()) {
                if (isMFMEncoding()) {
                    initialCRC = 0xb230;
                    readByte();
                } else {
                    initialCRC = 0xef21;
                }

                header[ADDRESS_CYLINDER] = readByte();  // Cylinder
                header[ADDRESS_HEAD] = readByte();  // Head
                header[ADDRESS_SECTOR] = readByte();  // Sector
                header[ADDRESS_SIZE] = readByte();  // Sector size, 0=128, 1=256, 2=512, 3=1024, etc.

                int expectedCRC = readTwoBytes();
                int calculatedCrc = CheckSum.calculateCRC(header, 0, header.length, initialCRC);
                if (calculatedCrc != expectedCRC) {
                    System.out.printf("Sector %d Header CRC error, expected 0x%04x, calculated 0x%04x\n", header[ADDRESS_SECTOR], expectedCRC, calculatedCrc);
                }

                if (advanceToNextDataAccessMark()) {
                    int accessMark = readValue & 0xFF;

                    if (isMFMEncoding()) {
                        initialCRC = (accessMark == MARK_DELETED) ? 0xe295 : 0xd2f6;
                    } else {
                        initialCRC = (accessMark == MARK_DELETED) ? 0xbf84 : 0x8fe7;
                    }

                    HFESector sector = new HFESector(header[ADDRESS_SECTOR], cellIndex, sectorSize(header[ADDRESS_SIZE]), initialCRC, accessMark);
                    for (int i = 0; i < sector.getSize(); i++) {
                        sector.getData()[i] = readByte();
                    }
                    sectors.add(sector);

                    expectedCRC = readTwoBytes();
                    calculatedCrc = sector.calculateChecksum();
                    if (calculatedCrc != expectedCRC) {
                        System.out.printf("Sector %d Data CRC error, expected 0x%04x, calculated 0x%04x\n", header[ADDRESS_SECTOR], expectedCRC, calculatedCrc);
                    }
                }
            }
        }
    }

    protected boolean isModified() {
        return getSectors().stream().anyMatch(HFESector::isModified);
    }

    protected void persist() {
        getSectors().stream()
                .filter(HFESector::isModified)
                .forEach(hfeSector -> {
                    lastDataBit = (hfeSector.getAccessMark()) & 1;
                    setCellPosition(hfeSector.getCellIndex());
                    for (int j = 0; j < hfeSector.getSize(); j++) {
                        writeBits(hfeSector.getData()[j], 8);
                    }
                    writeBits(hfeSector.calculateChecksum(), 16);
                });
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

    private void writeBits(int value, int number) {
        int mask = 1 << (number - 1);
        for (int i = 0; i < number; i++) {
            writeNextBit((value & mask)!=0);
            mask >>= 1;
        }
    }

    private void writeNextBit(boolean bit) {
        boolean clock = isFMEncoding() || (lastDataBit ==0 && !bit);

        writeNextCell(clock);
        if (codeRate == 4) {
            writeNextCell(false);  // ignore the next cell
        }
        writeNextCell(bit);   // Data bit
        lastDataBit = bit? 1 : 0;
        if (codeRate == 4) {
            writeNextCell(false);  // ignore the next cell
        }
    }

     private int readTwoBytes() {
        return readBits(16);
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
        if ((cellIndex % 8) == 0) {
            int trackOffset = getTrackOffset(cellIndex / 8);
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

    private void writeNextCell(boolean set) {
        int position = cellIndex / 8;
        if (position >= entry.getLength()) {
            position =  entry.getLength() - 1;
        }
        int trackOffset = getTrackOffset(position);

        int bit = 1 << (cellIndex % 8);

        if (set) {
            content[entry.getOffset() * SIZE_BLOCK + trackOffset] |= bit;
        } else {
            content[entry.getOffset() * SIZE_BLOCK + trackOffset] &= ~bit;
        }
        cellIndex++;
    }

    private int getTrackOffset(int position) {
        int blockIndex = position / (SIZE_BLOCK / 2);
        int blockOffset = position % (SIZE_BLOCK / 2);
        return blockIndex * SIZE_BLOCK + head * (SIZE_BLOCK / 2) + blockOffset;
    }

    private void setCellPosition(int pos) {
        int trackOffset = getTrackOffset(pos / 8);
        cellGroup = (byte)((content[entry.getOffset() * SIZE_BLOCK + trackOffset] >> (cellIndex % 8)) & 0xff);
        cellIndex = pos;
    }
}
