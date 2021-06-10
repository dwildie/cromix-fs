package au.wildie.m68k.cromixfs.ftar;

import static au.wildie.m68k.cromixfs.ftar.Thead.TYPE_BLOCK;
import static au.wildie.m68k.cromixfs.ftar.Thead.TYPE_CHARACTER;
import static au.wildie.m68k.cromixfs.ftar.Thead.TYPE_DIRECTORY;
import static au.wildie.m68k.cromixfs.ftar.Thead.TYPE_ORDINARY;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;

import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import au.wildie.m68k.cromixfs.fs.FileSystemOps;

public class CromixFtar implements FileSystemOps {
    private final FtarDisk disk;

    public CromixFtar(IMDImage image) {
        this.disk = new FtarDisk(image);
    }

    @Override
    public void list(PrintStream out) {
        while (true) {
            byte[] buf = disk.getNextBlock();
            if (buf[0] == 0) {
                break;
            }

            Thead thead;
            try {
                thead = Thead.from(buf);
            } catch (HeaderChecksumException e) {
                out.printf("%s %s Header checksum error\n", toType(e.getThead().getFlag()), e.getThead().getName());
                continue;
            }

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

    @Override
    public void extract(String path, PrintStream out) throws IOException {
        while (true) {
            byte[] buf = disk.getNextBlock();
            if (buf[0] == 0) {
                break;
            }

            Thead thead;
            try {
                thead = Thead.from(buf);
            } catch (HeaderChecksumException e) {
                out.printf("%s %s Header checksum error\n", toType(e.getThead().getFlag()), e.getThead().getName());
                continue;
            }

            out.printf("%s %s %12d %s\n", toType(thead.getFlag()), toLinks(thead.getFlag()), thead.getSize(), thead.getName());
            switch (thead.getFlag()) {
                case TYPE_ORDINARY:
                    if (thead.getLinkFlag() == 0) {
                        int fileSize = (int)thead.getSize();
                        int fileBlocks = fileSize / disk.getBlockSize();
                        if (fileSize % disk.getBlockSize() > 0) {
                            fileBlocks++;
                        }

                        File outFile = Paths.get(path, thead.getName()).toFile();
                        if (outFile.exists()) {
                            outFile.delete();
                        }
                        FileOutputStream fo = new FileOutputStream(outFile);
                        for (int i = 0; i < fileBlocks; i++) {
                            byte[] data = disk.getNextBlock();
                            if (fileSize < data.length) {
                                fo.write(data, 0, fileSize);
                                fileSize -= fileSize;
                            } else {
                                fo.write(data);
                                fileSize -= data.length;
                            }
                        }
                        fo.close();
                    }
                    break;

                case TYPE_DIRECTORY:
                    File newDir = Paths.get(path, thead.getName()).toFile();
                    if (!newDir.exists()) {
                        newDir.mkdirs();
                    }
                    break;
            }
        }
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
