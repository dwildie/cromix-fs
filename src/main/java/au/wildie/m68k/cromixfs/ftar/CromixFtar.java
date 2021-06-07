package au.wildie.m68k.cromixfs.ftar;

import static au.wildie.m68k.cromixfs.ftar.Thead.TYPE_BLOCK;
import static au.wildie.m68k.cromixfs.ftar.Thead.TYPE_CHARACTER;
import static au.wildie.m68k.cromixfs.ftar.Thead.TYPE_DIRECTORY;
import static au.wildie.m68k.cromixfs.ftar.Thead.TYPE_ORDINARY;
import java.io.IOException;
import java.io.PrintStream;
import au.wildie.m68k.cromixfs.disk.imd.IMDImage;

public class CromixFtar {
    private final FtarDisk disk;

    public CromixFtar(IMDImage image) {
        this.disk = new FtarDisk(image);
    }

    public void list(PrintStream out) throws IOException {
        while (true) {
            byte[] buf = disk.getNextBlock();
            if (buf[0] == 0) {
                break;
            }

            Thead thead = Thead.from(buf);

            out.printf("%s %s %12d %s\n", toType(thead.getFlag()), toLinks(thead.getFlag()), thead.getSize(), thead.getName());
            if (thead.getFlag() == TYPE_ORDINARY && thead.getLinkFlag() == 0) {
                int fileBlocks = (int) (thead.getSize() / disk.getBlockSize());
                if (thead.getSize() % disk.getBlockSize() > 0) {
                    fileBlocks++;
                }
                disk.skipBlocks(fileBlocks);
            }
        }
    }


    public void extract(String path, PrintStream out) throws IOException {

    }

    public String toLinks(int val) {
        return String.format("%d", val);
    }

    public String toType(int val) {
        switch (val) {
            case TYPE_ORDINARY:
                return "F";
            case TYPE_DIRECTORY:
                return "D";
            case TYPE_CHARACTER:
                return "C";
            case TYPE_BLOCK:
                return "B";
            default:
                return String.format("%d", val);
        }
    }
}
