package au.wildie.m68k.cromixfs.disk.floppy.vfd;

import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.disk.imd.Sector;
import au.wildie.m68k.cromixfs.disk.imd.Track;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;

@Getter
@RequiredArgsConstructor
public class VFDImage {
    public final ImageInfo info;
    public final byte[] data;

    public static VFDImage from(IMDImage imd) {
        ImageInfo info = new ImageInfo(imd.getCylinders(), imd.getHeads());

        ByteArrayOutputStream data = new ByteArrayOutputStream();

        imd.getTracks().stream()
                .sorted(Comparator.comparing(Track::getCylinder).thenComparing(Track::getHead))
                .peek(track -> {
                    if (track.getCylinder() == 0 && track.getHead() == 0) {
                        info.setFirst(new TrackInfo(track.getSectorCount(), track.getSectorSize(), ImageInfo.SIZE));
                    } else if (info.getRest() == null) {
                        info.setRest(new TrackInfo(track.getSectorCount(), track.getSectorSize(), ImageInfo.SIZE + info.getFirst().size()));
                    }
                })
                .forEach(track -> track.getSectors().stream()
                        .sorted(Comparator.comparing(Sector::getNumber))
                        .forEach(sector -> {
                            try {
                                data.write(sector.getData());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }));

        if (imd.size() != data.size()) {
            System.err.printf("Error: Expected %d data bytes, have %d\n", imd.size(), data.size());
        }

        return new VFDImage(info, data.toByteArray());
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream image = new ByteArrayOutputStream();
        image.write(info.toBytes());
        image.write(data);
        return image.toByteArray();
    }

    public byte[] read(int cylinder, int head, int sector) throws IOException {
        TrackInfo track;
        int offset;
        if (cylinder == 0 && head == 0) {
            // First track
            track = info.getFirst();
            offset = track.getOffset() + sector * track.getSectorBytes();
        } else {
            // Remaining tracks
            track = info.getRest();
            offset = track.getOffset() + (cylinder * info.getHeads() + head - 1) * (track.getSectors() * track.getSectorBytes()) + sector * track.getSectorBytes();
        }
        System.out.printf("cyl[%2d], head[%d], sector[%2d]: offset=%d (0x%x)\n", cylinder, head, sector, offset, offset);
        byte[] readData = new byte[track.getSectorBytes()];
        System.arraycopy(toBytes(), offset, readData, 0, track.getSectorBytes());
        return readData;
    }
}
