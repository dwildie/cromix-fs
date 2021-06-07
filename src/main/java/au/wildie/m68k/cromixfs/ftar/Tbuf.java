package au.wildie.m68k.cromixfs.ftar;

import static au.wildie.m68k.cromixfs.ftar.Thead.NAMSIZ;
import lombok.Getter;

@Getter
public class Tbuf {
    public static final int TBUF_PATH_NAME_OFFSET     = 0x000;
    public static final int TBUF_ACCESS_OFFSET        = 0x064;
    public static final int TBUF_USER_ID_OFFSET       = 0x06c;
    public static final int TBUF_GROUP_ID_OFFSET      = 0x074;
    public static final int TBUF_FILE_SIZE_OFFSET     = 0x07c;
    public static final int TBUF_TIME_MODIFIED_OFFSET = 0x088;
    public static final int TBUF_CHECKSUM_OFFSET      = 0x094;
    public static final int TBUF_LINK_FLAG_OFFSET     = 0x09c;
    public static final int TBUF_LINK_NAME_OFFSET     = 0x09d;
    public static final int TBUF_FILE_TYPE_OFFSET     = 0x101;
    public static final int TBUF_DEVICE_NUMBER_OFFSET = 0x102;

    private char[] name = new char[NAMSIZ];   /*   0 0x000 Path name                            */
    private char[] mode = new char[8];        /* 100 0x064 Unix-like access                     */
    private  char[] uid = new char[8];        /* 108 0x06c User ID                              */
    private char[] gid = new char[8];         /* 116 0x074 Group ID                             */
    private char[] size = new char[12];       /* 124 0x07c File size                            */
    private char[] mtime = new char[12];      /* 136 0x088 Time modified                        */
    private char[] chksum = new char[8];      /* 148 0x094 Checksum                             */
    private char[] link = new char[NAMSIZ+2]; /* 156 0x09c Link flag + Link name + File type    */
    private char[] devno = new char[8];       /* 258 0x0102 Device number for device files       */
}
