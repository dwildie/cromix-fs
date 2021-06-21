package au.wildie.m68k.cromixfs.disk.hxc;

import static au.wildie.m68k.cromixfs.disk.imd.ImageException.CODE_END_OF_DISK;
import static au.wildie.m68k.cromixfs.utils.Int68000.from2BytesUnsigned;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import au.wildie.m68k.cromixfs.disk.DiskImage;
import au.wildie.m68k.cromixfs.disk.imd.ImageException;
import lombok.Getter;
import org.apache.commons.io.IOUtils;

@Getter
public class HFEImage extends DiskImage  {
    public static final int SIZE_BLOCK = 0x200;

    private final byte[] content;
    private final HFEHeader header;
    private final TrackEntry[] trackList;
    private HFETrack currentTrack;

    public static HFEImage from(byte[] content) throws IOException {
        return new HFEImage(content);
    }

    public static HFEImage from(InputStream src) throws IOException {
        return new HFEImage(IOUtils.toByteArray(src));
    }

    public HFEImage(byte[] content) {
        this.content = content;
        this.header = HFEHeader.from(content);
        this.trackList = new TrackEntry[header.getCylinders()];;

        System.out.println(header);

        for (int i = 0; i < trackList.length; i++) {
            trackList[i] = new TrackEntry(
                    from2BytesUnsigned(content, header.getTrackListOffset() * SIZE_BLOCK + i * 4),
                    from2BytesUnsigned(content, header.getTrackListOffset() * SIZE_BLOCK + i * 4 + 2));
        }
    }

    public byte[] read(int cylinder, int head, int sector) {
        return getSector(cylinder, head, sector).getData();
    }

    @Override
    public int getCylinders() {
        return trackList.length;
    }

    @Override
    public int getHeads() {
        return 2;
    }

    public HFESector getSector(int cylinder, int head, int sectorNumber) {
        if (cylinder >= trackList.length) {
            throw new ImageException(CODE_END_OF_DISK, String.format("Cylinder %d does not exist", cylinder));
        }
        return getTrack(cylinder, head).getSectors().get(sectorNumber - 1);
    }

    @Override
    public void persist(OutputStream archive) {
        // TODO
    }

    public HFETrack getTrack(int cylinder, int head) {
        if (currentTrack == null || currentTrack.getCylinder() != cylinder || currentTrack.getHead() != head) {
            if (currentTrack != null && currentTrack.isModified()) {
                currentTrack.persist();
            }
            currentTrack = new HFETrack(cylinder, head, header.getTrackEncoding(cylinder, head), header.getBitRate(), trackList[cylinder], content);
            currentTrack.read();
        }
        return currentTrack;
    }

    public void write(int cylinder, int head, int sector, byte[] data) {
        getSector(cylinder, head, sector).write(data);
    }

    public byte[] toBytes() {
        return content;
    }
}
