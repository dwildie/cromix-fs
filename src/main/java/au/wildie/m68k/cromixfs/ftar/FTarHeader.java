package au.wildie.m68k.cromixfs.ftar;


import static au.wildie.m68k.cromixfs.ftar.Tbuf.*;
import static au.wildie.m68k.cromixfs.utils.BinUtils.*;

import au.wildie.m68k.cromixfs.fs.CromixTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FTarHeader {

    public static final int TYPE_ORDINARY = 0;
    public static final int TYPE_DIRECTORY = 1;
    public static final int TYPE_CHARACTER = 2;
    public static final int TYPE_BLOCK = 3;

//    time    basetime = {69,12,31,16,0,0};   /* Dec 31, 1968, 16:00:00       */
    private static final CromixTime baseTime = new CromixTime((byte)69,(byte)12,(byte)31,(byte)16,(byte)0,(byte)0);

    private int flag;                           /* Record type                  */
    private int stext;                          /* Shared text                  */
    private int access;                         /* unix access */
    private int owner;                          /* Owner access                 */
    private int group;                          /* Group access                 */
    private int other;                          /* Other access                 */
    private int uid;                            /* User ID                      */
    private int gid;                            /* User ID                      */
    private long size;                          /* File size                    */
    private long mtime;                         /* Modify time                  */
    private long chksum;                        /* Checksum                     */
    private String name;                        /* File path name               */
    private String link;                        /* Link name                    */
    private int devno;                          /* Device number                */
    private int linkFlag;

    public static FTarHeader from(byte[] buf) throws HeaderChecksumException {
        FTarHeader header = new FTarHeader();
        header.setName(readString(buf, TBUF_PATH_NAME_OFFSET));
        header.setAccess(asciiOctalToInt(buf, TBUF_ACCESS_OFFSET, TBUF_ACCESS_SIZE));
        header.setUid(asciiOctalToInt(buf, TBUF_USER_ID_OFFSET, TBUF_USER_ID_SIZE));
        header.setGid(asciiOctalToInt(buf, TBUF_GROUP_ID_OFFSET, TBUF_GROUP_ID_SIZE));
        header.setSize(asciiOctalToLong(buf, TBUF_FILE_SIZE_OFFSET, TBUF_FILE_SIZE_SIZE));
        header.setMtime(asciiOctalToLong(buf, TBUF_TIME_MODIFIED_OFFSET, TBUF_TIME_MODIFIED_SIZE) + baseTime.utime());
        header.setChksum(asciiOctalToLong(buf, TBUF_CHECKSUM_OFFSET, TBUF_CHECKSUM_SIZE));
        if (buf[TBUF_LINK_FLAG_OFFSET] >= 0x30) {
            header.setLinkFlag(buf[TBUF_LINK_FLAG_OFFSET] - 0x30);
            header.setLink(readString(buf, TBUF_LINK_NAME_OFFSET));
        }
        header.setFlag(buf[TBUF_FILE_TYPE_OFFSET] - 0x30);

        int chksum = checksum(buf);
        if (chksum != header.getChksum()) {
            throw new HeaderChecksumException(String.format("Expected checksum %x, calculated %x", header.getChksum(), chksum), header);
        }
        return header;
    }

    private static Integer checksum(byte[] buf) {
        for (int i = 0; i < TBUF_CHECKSUM_SIZE; i++) {
            buf[TBUF_CHECKSUM_OFFSET + i] = ' ';
        }

        int chksum = 0;
        for (int i = 0; i < TBUF_SIZE; i++) {
            chksum += buf[i];
        }
        return chksum;
    }

    public byte[] write() {
        byte[] block = new byte[512];
        block[TBUF_FILE_TYPE_OFFSET] = (byte)(getFlag() + 0x30);
        writeString(getName(), block, TBUF_PATH_NAME_OFFSET, TBUF_NAME_SIZE);
        asciiIntToOctal(getAccess(), block, TBUF_ACCESS_OFFSET, TBUF_ACCESS_SIZE);
        asciiIntToOctal(getUid(), block, TBUF_USER_ID_OFFSET, TBUF_USER_ID_SIZE);
        asciiIntToOctal(getGid(), block, TBUF_GROUP_ID_OFFSET, TBUF_GROUP_ID_SIZE);
        asciiLongToOctal(getSize(), block, TBUF_FILE_SIZE_OFFSET, TBUF_FILE_SIZE_SIZE);
        asciiLongToOctal(getMtime(), block, TBUF_TIME_MODIFIED_OFFSET, TBUF_TIME_MODIFIED_SIZE);
        asciiIntToOctal(checksum(block), block, TBUF_CHECKSUM_OFFSET, TBUF_CHECKSUM_SIZE);
        return block;
    }
}
