package au.wildie.m68k.cromixfs.disk.hxc;

import lombok.Getter;
import lombok.Setter;

import static au.wildie.m68k.cromixfs.utils.Int68000.from2BytesUnsigned;

@Getter
@Setter
public class HFEHeader {
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

    private static String[] modeNames = {
            "IBMPC DD", "IBMPC HD", "ATARIST DD", "ATARIST HD",
            "AMIGA DD", "AMIGA HD", "CPC DD", "GENERIC SHUGART DD",
            "IBMPC ED", "MSX2 DD", "C64 DD", "EMU SHUGART",
            "S950 DD", "S950 HD"
    };


    private static String[] encodingNames = {
            "ISOIBM MFM", "AMIGA MFM", "ISOIBM FM", "EMU FM"
    };

    private int revision;
    private int trackCount;
    private int headCount;
    private int encoding;
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
        header.revision = content[OFFSET_REVISION] & 0xff;
        header.trackCount = content[OFFSET_TRACK_COUNT] & 0xff;
        header.headCount = content[OFFSET_HEAD_COUNT] & 0xff;
        header.trackListOffset = from2BytesUnsigned(content, OFFSET_TRACK_LIST);
        header.encoding = (content[OFFSET_TRACK_ENCODING] & 0xff);
        header.bitRate = from2BytesUnsigned(content, OFFSET_BIT_RATE);
        header.bitRate = from2BytesUnsigned(content, OFFSET_FLOPPY_RPM);
        header.mode = (content[OFFSET_INTERFACE_MODE] & 0xff);
        header.writeable = (content[OFFSET_CAN_WRITE] & 0xff) != 0;
        header.singleStep = (content[OFFSET_SINGLE_STEP] & 0xff) != 0;
        header.track0AltEncoded = (content[OFFSET_TRACK0_ALT_ENC] & 0xff) == (byte)0;
        header.track0Encoding = (content[OFFSET_TRACK0_ENCODE] & 0xff);
        header.track1AltEncoded = (content[OFFSET_TRACK1_ALT_ENC] & 0xff) == (byte)0;
        header.track1Encoding = (content[OFFSET_TRACK1_ENCODE] & 0xff);
        return header;
    }

    public String getModeName() {
        return mode < modeNames.length ? modeNames[mode]: "Unknown";
    }

    public String getEncodingName() {
        return encoding < encodingNames.length ? encodingNames[encoding] : "Unknown";
    }

    public String getTrack0EncodingName() {
        return track0Encoding < encodingNames.length ? encodingNames[track0Encoding] : "Unknown";
    }

    public String getTrack1EncodingName() {
        return track1Encoding < encodingNames.length ? encodingNames[track1Encoding] : "Unknown";
    }}
