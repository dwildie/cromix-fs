package au.wildie.m68k.cromixfs.disk.floppy.vfd;

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
    public static final int SIZE = 12;

    private final int sectors;
    private final int sectorBytes;
    private final int offset;

    public int size() {
        return sectors * sectorBytes;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(Int68000.toBytes(sectors));
        bytes.write(Int68000.toBytes(sectorBytes));
        bytes.write(Int68000.toBytes(offset));
        return bytes.toByteArray();
    }
}
