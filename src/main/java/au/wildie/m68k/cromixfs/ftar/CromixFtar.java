package au.wildie.m68k.cromixfs.ftar;

import au.wildie.m68k.cromixfs.disk.DiskInfo;
import au.wildie.m68k.cromixfs.disk.SectorInvalidException;
import au.wildie.m68k.cromixfs.disk.imd.ImageException;
import au.wildie.m68k.cromixfs.fs.FileSystemOps;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;

import static au.wildie.m68k.cromixfs.disk.imd.ImageException.CODE_END_OF_DISK;
import static au.wildie.m68k.cromixfs.ftar.Thead.*;

public class CromixFtar implements FileSystemOps {
    private final FTarDisk disk;
    private boolean reSync;

    public CromixFtar(FTarDisk disk, PrintStream out) {
        this.disk = disk;
    }

    @Override
    public String getName() {
        return "Cromix ftar";
    }

    @Override
    public DiskInfo getDisk() {
        return disk;
    }

    public boolean isValid() {
        // Look for a valid ftar header block
        while (true) {
            byte[] buf;
            try {
                buf = disk.getNextBlock();
                if (buf[0] != 0 && buf[0] != (byte)0xE5) {
                    Thead.from(buf);
                    return true;
                }
            } catch (ImageException e) {
                if (e.getCode().equals(CODE_END_OF_DISK)) {
                    break;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    @Override
    public void list(PrintStream out) {
        disk.reset();
        while (true) {
            byte[] buf = new byte[0];
            try {
                buf = disk.getNextBlock();
            } catch (SectorInvalidException e) {
                out.printf("Sector error %s, skipping block %d\n", e.getMessage(), disk.getCurrentBlockNumber());
                continue;
            } catch (ImageException e) {
                if (e.getCode().equals(CODE_END_OF_DISK)) {
                    break;
                }
                out.printf("Disk error: %s\n", e.getMessage());
                out.printf("Skipping block %d\n", disk.getCurrentBlockNumber());
            }

            if (buf[0] == 0 || buf[0] == (byte)0xE5) {
                continue;
            }

            Thead thead;
            try {
                thead = Thead.from(buf);
            } catch (HeaderChecksumException e) {
                out.printf("%s %s Header checksum error\n", toType(e.getThead().getFlag()), e.getThead().getName());
                out.printf("Skipping block %d\n", disk.getCurrentBlockNumber());
                continue;
            } catch (Exception e) {
                if (!reSync) {
                    out.printf("Invalid block %d, searching for next ftar header \n", disk.getCurrentBlockNumber());
                    reSync = true;
                }
                continue;
            }
            if (reSync) {
                out.printf("Found ftar header at block %d\n", disk.getCurrentBlockNumber());
                reSync = false;
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
            byte[] buf = new byte[0];
            try {
                buf = disk.getNextBlock();
            } catch (SectorInvalidException e) {
                out.printf("Skipping block %d\n", disk.getCurrentBlockNumber());
            } catch (ImageException e) {
                if (e.getCode().equals(CODE_END_OF_DISK)) {
                    break;
                }
                out.printf("Disk error: %s\n", e.getMessage());
                out.printf("Skipping block %d\n", disk.getCurrentBlockNumber());
            }

            if (buf[0] == 0 || buf[0] == (byte)0xE5) {
                continue;
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
                            byte[] data = new byte[0];
                            try {
                                data = disk.getNextBlock();
                            } catch (SectorInvalidException e) {
                                out.printf("Data errors at: %s\n", e.getMessage());
                                data = new byte[disk.getBlockSize()];
                            }

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
