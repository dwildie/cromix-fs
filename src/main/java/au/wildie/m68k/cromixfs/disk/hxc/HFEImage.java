package au.wildie.m68k.cromixfs.disk.hxc;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import static au.wildie.m68k.cromixfs.utils.Int68000.from2BytesUnsigned;

public class HFEImage {
    private final int SIZE_BLOCK = 0x200;

    private byte[] content;
    private HFEHeader header;
    private TrackEntry[] trackList;



    public static HFEImage from(InputStream src) throws IOException {
        return new HFEImage(IOUtils.toByteArray(src));
    }

    public HFEImage(byte[] content) {
        this.content = content;
        this.header = HFEHeader.from(content);
        this.trackList = new TrackEntry[header.getTrackCount()];;

        System.out.printf("Tracks:           %d\n", header.getTrackCount());
        System.out.printf("Heads:            %d\n", header.getHeadCount());
        System.out.printf("Track encoding:   %d - %s\n", header.getEncoding(), header.getEncodingName());
        System.out.printf("Bit rate:         %d kbps\n", header.getBitRate());
        System.out.printf("Floppy RPM:       %d\n", header.getFloppyRPM());
        System.out.printf("Interface mode:   %d - %s\n", header.getMode(), header.getModeName());
        System.out.printf("Writable:         %b\n", header.isWriteable());
        System.out.printf("Single step:      %b\n", header.isSingleStep());
        System.out.printf("Track 0 alt enc:  %b\n", header.isTrack0AltEncoded());
        if (header.isTrack0AltEncoded()) {
            System.out.printf("Track 0 encoding: %d - %s\n", header.getTrack0Encoding(), header.getTrack0EncodingName());
        }
        System.out.printf("Track 1 alt enc:  %b\n", header.isTrack1AltEncoded());
        if (header.isTrack1AltEncoded()) {
            System.out.printf("Track 1 encoding: %d - %s\n", header.getTrack1Encoding(), header.getTrack1EncodingName());
        }

        for (int i = 0; i < trackList.length; i++) {
            trackList[i] = new TrackEntry(
                    from2BytesUnsigned(content, header.getTrackListOffset() * SIZE_BLOCK + i * 4),
                    from2BytesUnsigned(content, header.getTrackListOffset() * SIZE_BLOCK + i * 4 + 2));
        }
    }

    public byte[] getTrack(int cylinder, int head) {
        TrackEntry entry = trackList[cylinder];

        int offset = entry.getOffset() * SIZE_BLOCK;

        byte[][] data = new byte[2][entry.getOffset() / 2];


        return data[head];
    }
}
