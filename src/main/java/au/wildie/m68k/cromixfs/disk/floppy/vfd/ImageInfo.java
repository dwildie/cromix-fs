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
public class ImageInfo {
    public static final int SIZE = 8 + 2 * TrackInfo.SIZE;

    private final int cylinders;
    private final int heads;
    private TrackInfo first;
    private TrackInfo rest;

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(Int68000.toBytes(cylinders));
        bytes.write(Int68000.toBytes(heads));
        bytes.write(first.toBytes());
        bytes.write(rest.toBytes());
        return bytes.toByteArray();
    }
}
