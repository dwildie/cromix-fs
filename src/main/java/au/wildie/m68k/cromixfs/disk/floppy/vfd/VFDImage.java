package au.wildie.m68k.cromixfs.disk.floppy.vfd;

import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.disk.imd.Sector;
import au.wildie.m68k.cromixfs.disk.imd.Track;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
}
