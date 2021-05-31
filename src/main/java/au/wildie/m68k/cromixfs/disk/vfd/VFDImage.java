package au.wildie.m68k.cromixfs.disk.vfd;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Comparator;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.disk.imd.ImageException;
import au.wildie.m68k.cromixfs.disk.imd.Sector;
import au.wildie.m68k.cromixfs.disk.imd.Track;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

@Getter
@RequiredArgsConstructor
public class VFDImage {
    public final ImageInfo info;
    public final byte[] data;

    public static VFDImage fromFile(int driveId, String filePath, PrintStream out) throws IOException, InvalidVFDImageException {
        return fromFile(driveId, new File(filePath), out);
    }

    public static VFDImage fromFile(int driveId, File imdFile, PrintStream out) throws IOException, InvalidVFDImageException {
        out.printf("Reading VFD file %s%n", imdFile.getPath());

        if (!imdFile.exists()) {
            out.printf("Drive %d: VFD file %s does not exist%n", driveId, imdFile.getPath());
            throw new ImageException(String.format("Drive %d: IMD file %s does not exist%n", driveId, imdFile.getPath()));
        }

        try (InputStream src = new FileInputStream(imdFile)) {
            return fromStream(src);
        }
    }

    public static VFDImage fromStream(InputStream imdStream) throws IOException, InvalidVFDImageException {
        byte[] raw = IOUtils.toByteArray(imdStream);
        ImageInfo info = ImageInfo.fromBytes(raw);
        byte[] data = new byte[raw.length - ImageInfo.SIZE];
        System.arraycopy(raw, ImageInfo.SIZE, data, 0, data.length);
        return new VFDImage(info, data);
    }

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
        Pair<TrackInfo, Integer> trackAndOffset = getTrackAndOffset(cylinder, head, sector - 1);
//        System.out.printf("cyl[%2d], head[%d], sector[%2d]: offset=%d (0x%x)\n", cylinder, head, sector, trackAndOffset.getRight(), trackAndOffset.getRight());
        byte[] readData = new byte[trackAndOffset.getLeft().getSectorBytes()];
        System.arraycopy(toBytes(), trackAndOffset.getRight(), readData, 0, trackAndOffset.getLeft().getSectorBytes());
        return readData;
    }

    public Pair<TrackInfo, Integer> getTrackAndOffset(int cylinder, int head, int sector) {
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
        return new ImmutablePair<>(track, offset);
    }
}
