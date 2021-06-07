package au.wildie.m68k.cromixfs.ftar;


import static au.wildie.m68k.cromixfs.ftar.Tbuf.TBUF_FILE_SIZE_OFFSET;
import static au.wildie.m68k.cromixfs.ftar.Tbuf.TBUF_FILE_TYPE_OFFSET;
import static au.wildie.m68k.cromixfs.ftar.Tbuf.TBUF_LINK_FLAG_OFFSET;
import static au.wildie.m68k.cromixfs.ftar.Tbuf.TBUF_LINK_NAME_OFFSET;
import static au.wildie.m68k.cromixfs.ftar.Tbuf.TBUF_PATH_NAME_OFFSET;
import static au.wildie.m68k.cromixfs.utils.BinUtils.asciiOctalToLong;
import static au.wildie.m68k.cromixfs.utils.BinUtils.readString;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Thead {
    public static final int NAMSIZ = 100;

    public static final int TYPE_ORDINARY = 0;
    public static final int TYPE_DIRECTORY = 1;
    public static final int TYPE_CHARACTER = 2;
    public static final int TYPE_BLOCK = 3;


    private int flag;                           /* Record type                  */
    private int stext;                          /* Shared text                  */
    private int owner;                          /* Owner access                 */
    private int group;                          /* Group access                 */
    private int other;                          /* Other access                 */
    private int uid;                            /* User ID                      */
    private int gid;                            /* User ID                      */
    private long size;                          /* File size                    */
    private long mtime;                         /* Modify time                  */
    private String name;                        /* File path name               */
    private String link;                        /* Link name                    */
    private int devno;                          /* Device number                */
    private int linkFlag;

    public static Thead from(byte[] buf) {
        Thead thead = new Thead();
        thead.setName(readString(buf, TBUF_PATH_NAME_OFFSET));
        thead.setSize(asciiOctalToLong(buf, TBUF_FILE_SIZE_OFFSET, 12));
        if (buf[TBUF_LINK_FLAG_OFFSET] >= 0x30) {
            thead.setLinkFlag(buf[TBUF_LINK_FLAG_OFFSET] - 0x30);
            thead.setLink(readString(buf, TBUF_LINK_NAME_OFFSET));
        }
        thead.setFlag(buf[TBUF_FILE_TYPE_OFFSET] - 0x30);
        return thead;
    }
}
