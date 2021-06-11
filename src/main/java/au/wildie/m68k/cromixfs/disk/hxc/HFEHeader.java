package au.wildie.m68k.cromixfs.disk.hxc;

import lombok.Getter;
import lombok.Setter;

import static au.wildie.m68k.cromixfs.utils.Int68000.from2BytesUnsigned;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class HFEHeader {
    public static final int SIG_LENGTH = 8;

    public static final int OFFSET_REVISION        = 0x08;
    public static final int OFFSET_TRACK_COUNT     = 0x09;
    public static final int OFFSET_HEAD_COUNT      = 0x0a;
    public static final int OFFSET_TRACK_ENCODING  = 0x0b;
    public static final int OFFSET_BIT_RATE        = 0x0c; // 2 bytes
    public static final int OFFSET_FLOPPY_RPM      = 0x0e; // 2 bytes
    public static final int OFFSET_INTERFACE_MODE  = 0x10;
    public static final int OFFSET_NOT_USED        = 0x11;
    public static final int OFFSET_TRACK_LIST      = 0x12; // 2 bytes
    public static final int OFFSET_CAN_WRITE       = 0x14;
    public static final int OFFSET_SINGLE_STEP     = 0x15;
    public static final int OFFSET_TRACK0_ALT_ENC  = 0x16;
    public static final int OFFSET_TRACK0_ENCODE   = 0x17;
    public static final int OFFSET_TRACK1_ALT_ENC  = 0x18;
    public static final int OFFSET_TRACK1_ENCODE   = 0x19;

    public final static int ISOIBM_MFM_ENCODING = 0x00;
    public final static int AMIGA_MFM_ENCODING = 0x01;
    public final static int ISOIBM_FM_ENCODING = 0x02;
    public final static int EMU_FM_ENCODING = 0x03;
    public final static int UNKNOWN_ENCODING = 0x04;

    private static String[] modeNames = {
            "IBMPC DD", "IBMPC HD", "ATARIST DD", "ATARIST HD",
            "AMIGA DD", "AMIGA HD", "CPC DD", "GENERIC SHUGART DD",
            "IBMPC ED", "MSX2 DD", "C64 DD", "EMU SHUGART",
            "S950 DD", "S950 HD"
    };


    private static String[] encodingNames = {
            "ISOIBM MFM", "AMIGA MFM", "ISOIBM FM", "EMU FM"
    };

    private static final String SIGNATURE = "HXCPICFE";

    private String signature;
    private int revision;
    private int cylinders;
    private int heads;
    private int trackEncoding;
    private int bitRate;
    private int floppyRPM;
    private int mode;
    private int trackListOffset;
    private boolean writeable;
    private boolean singleStep;
    private boolean track0AltEncoded;
    private int track0Encoding;
    private boolean track1AltEncoded;
    private int track1Encoding;

    public static HFEHeader from(byte[] content) {
        HFEHeader header = new HFEHeader();
        header.signature = new String(content, 0, SIG_LENGTH);
        header.revision = content[OFFSET_REVISION] & 0xff;
        header.cylinders = content[OFFSET_TRACK_COUNT] & 0xff;
        header.heads = content[OFFSET_HEAD_COUNT] & 0xff;
        header.trackListOffset = from2BytesUnsigned(content, OFFSET_TRACK_LIST);
        header.trackEncoding = (content[OFFSET_TRACK_ENCODING] & 0xff);
        header.bitRate = from2BytesUnsigned(content, OFFSET_BIT_RATE);
        header.floppyRPM = from2BytesUnsigned(content, OFFSET_FLOPPY_RPM);
        header.mode = (content[OFFSET_INTERFACE_MODE] & 0xff);
        header.writeable = (content[OFFSET_CAN_WRITE] & 0xff) != 0;
        header.singleStep = (content[OFFSET_SINGLE_STEP] & 0xff) != 0;
        header.track0AltEncoded = (content[OFFSET_TRACK0_ALT_ENC] & 0xff) == (byte)0;
        header.track0Encoding = (content[OFFSET_TRACK0_ENCODE] & 0xff);
        header.track1AltEncoded = (content[OFFSET_TRACK1_ALT_ENC] & 0xff) == (byte)0;
        header.track1Encoding = (content[OFFSET_TRACK1_ENCODE] & 0xff);
        return header;
    }

    public int getTrackEncoding(int cylinder, int head) {
        if (cylinder == 0 && head == 0 && track0AltEncoded) {
            return track0Encoding;
        }
        if (cylinder == 0 && head == 1 && track1AltEncoded) {
            return track1Encoding;
        }
        return trackEncoding;
    }

    @Override
    public String toString() {
        List<String> list = new ArrayList<>();

        list.add(String.format("Signature:        %s", getSignature()));
        list.add(String.format("Cylinders:        %d", getCylinders()));
        list.add(String.format("Heads:            %d", getHeads()));
        list.add(String.format("Track encoding:   %d - %s", getTrackEncoding(), getTrackEncodingName()));
        list.add(String.format("Bit rate:         %d kbps", getBitRate()));
        list.add(String.format("Floppy RPM:       %d", getFloppyRPM()));
        list.add(String.format("Interface mode:   %d - %s", getMode(), getModeName()));
        list.add(String.format("Writable:         %b", isWriteable()));
        list.add(String.format("Single step:      %b", isSingleStep()));
        list.add(String.format("Track 0 alt enc:  %b", isTrack0AltEncoded()));
        if (isTrack0AltEncoded()) {
            list.add(String.format("Track 0 encoding: %d - %s", getTrack0Encoding(), getTrack0EncodingName()));
        }
        list.add(String.format("Track 1 alt enc:  %b", isTrack1AltEncoded()));
        if (isTrack1AltEncoded()) {
            list.add(String.format("Track 1 encoding: %d - %s", getTrack1Encoding(), getTrack1EncodingName()));
        }
        return String.join("\n", list);
    }

    public String getModeName() {
        return mode < modeNames.length ? modeNames[mode]: "Unknown";
    }

    public String getTrackEncodingName() {
        return trackEncoding < encodingNames.length ? encodingNames[trackEncoding] : "Unknown";
    }

    public String getTrack0EncodingName() {
        return track0Encoding < encodingNames.length ? encodingNames[track0Encoding] : "Unknown";
    }

    public String getTrack1EncodingName() {
        return track1Encoding < encodingNames.length ? encodingNames[track1Encoding] : "Unknown";
    }
}
