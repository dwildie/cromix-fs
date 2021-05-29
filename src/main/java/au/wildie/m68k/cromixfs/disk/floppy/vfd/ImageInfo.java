
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
    public static final int SIZE = 4 + 4 + 4 + 2 * TrackInfo.SIZE;
    public final int MAJOR_VERSION = 0;
    public final int MINOR_VERSION = 1;
    private final int cylinders;
    private final int heads;
    private TrackInfo first;
    private TrackInfo rest;

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write("VFD".getBytes());
        bytes.write(new byte[]{0});
        bytes.write(Int68000.to2Bytes(MAJOR_VERSION));
        bytes.write(Int68000.to2Bytes(MINOR_VERSION));
        bytes.write(Int68000.to2Bytes(cylinders));
        bytes.write(Int68000.to2Bytes(heads));
        bytes.write(first.toBytes());
        bytes.write(rest.toBytes());
        return bytes.toByteArray();
    }
}
