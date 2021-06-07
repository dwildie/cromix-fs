
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
public class ImageInfo {
    public static String SIG = "VFD";
    public static final int SIZE
            = 4 // "VFD" + null
            + 4 // Major & minor version
            + 4 // Cylinders & heads
            + 2 * TrackInfo.SIZE;
    public final int MAJOR_VERSION = 0;
    public final int MINOR_VERSION = 1;
    private final int cylinders;
    private final int heads;
    private TrackInfo first;
    private TrackInfo rest;

    public static ImageInfo fromBytes(byte[] data) throws InvalidVFDImageException {
        if (data[0] != 'V' || data[1] != 'F' || data[2] != 'D' || data[3] != 0) {
            throw new InvalidVFDImageException("Invalid signature");
        }
//        int majorVersion = Int68000.from2Bytes(data, 4);
//        int minorVersion = Int68000.from2Bytes(data, 6);

        int cylinders = Int68000.from2Bytes(data, 8);
        int heads = Int68000.from2Bytes(data, 10);
        ImageInfo info = new ImageInfo(cylinders, heads);
        info.setFirst(TrackInfo.fromBytes(data, 12));
        info.setRest(TrackInfo.fromBytes(data, 12 + TrackInfo.SIZE));

        return info;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(SIG.getBytes());
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
