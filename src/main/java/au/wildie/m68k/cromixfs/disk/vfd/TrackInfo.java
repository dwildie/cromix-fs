package au.wildie.m68k.cromixfs.disk.vfd;

import au.wildie.m68k.cromixfs.utils.Int68000;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Setter
@Getter
@RequiredArgsConstructor
public class TrackInfo {
    public static final int SIZE
            = 2  // sectors
            + 2  // sectorBytes
            + 2; // offset

    private final int sectors;
    private final int sectorBytes;
    private final int offset;

    public int size() {
        return sectors * sectorBytes;
    }

    public static TrackInfo fromBytes(byte[] data, int index) {
        int sectors = Int68000.from2Bytes(data, index);
        int sectorBytes = Int68000.from2Bytes(data, index + 2);
        int offset = Int68000.from2Bytes(data, index + 4);
        return new TrackInfo(sectors, sectorBytes, offset);
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(Int68000.to2Bytes(sectors));
        bytes.write(Int68000.to2Bytes(sectorBytes));
        bytes.write(Int68000.to2Bytes(offset));
        return bytes.toByteArray();
    }
}
