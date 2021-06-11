package au.wildie.m68k.cromixfs.disk.hxc;

import static au.wildie.m68k.cromixfs.utils.Int68000.from2BytesUnsigned;
import java.io.IOException;
import java.io.InputStream;
import lombok.Getter;
import org.apache.commons.io.IOUtils;

@Getter
public class HFEImage {
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
        if (currentTrack == null || currentTrack.getCylinder() != cylinder || currentTrack.getHead() != head) {
            currentTrack = new HFETrack(cylinder, head, header.getTrackEncoding(cylinder, head), header.getBitRate(), trackList[cylinder], content);
            currentTrack.read();
        }
        return currentTrack.getSectors().get(sector).getData();
    }

    public byte[] toBytes() {
        return content;
    }
}
