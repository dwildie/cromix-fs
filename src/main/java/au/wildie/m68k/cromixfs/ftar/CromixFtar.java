package au.wildie.m68k.cromixfs.ftar;

import au.wildie.m68k.cromixfs.disk.DiskInfo;
import au.wildie.m68k.cromixfs.disk.SectorInvalidException;
import au.wildie.m68k.cromixfs.disk.imd.ImageException;
import au.wildie.m68k.cromixfs.fs.CromixTime;
import au.wildie.m68k.cromixfs.fs.FileSystemOps;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;

import static au.wildie.m68k.cromixfs.disk.imd.ImageException.CODE_END_OF_DISK;
import static au.wildie.m68k.cromixfs.ftar.FTarHeader.*;

public class CromixFtar implements FileSystemOps {
    private final FTarDisk disk;
    private boolean reSync;

    public CromixFtar(String formatLabel, PrintStream out) {
        this.disk = FTarIMDDisk.create(formatLabel, out);
    }

    public CromixFtar(FTarDisk disk) {
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
                    FTarHeader.from(buf);
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
        process(false, null, out);
    }

    @Override
    public void extract(String path, PrintStream out) {
        process(true, path, out);
    }

    protected void process(boolean extract, String path, PrintStream out) {
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

            FTarHeader thead;
            try {
                thead = FTarHeader.from(buf);
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

            out.printf("%s %s %12d  %s  %s\n",
                    toType(thead.getFlag()),
                    toLinks(thead.getFlag()),
                    thead.getSize(),
                    CromixTime.from(thead.getMtime()).toString(),
                    thead.getName());

            if (!extract) {
                // Skip any file blocks
                if (thead.getFlag() == TYPE_ORDINARY && thead.getLinkFlag() == 0) {
                    int fileBlocks = (int) (thead.getSize() / disk.getBlockSize());
                    if (thead.getSize() % disk.getBlockSize() > 0) {
                        fileBlocks++;
                    }
                    disk.skipBlocks(fileBlocks);
                }
            } else {
                // Create directories and files
                switch (thead.getFlag()) {
                    case TYPE_ORDINARY:
                        if (thead.getLinkFlag() == 0) {
                            Date modified;
                            try {
                                modified = CromixTime.from(thead.getMtime()).toDate();
                            } catch (ParseException e) {
                                e.printStackTrace();
                                modified = new Date();
                            }

                            try {
                                int fileSize = (int) thead.getSize();
                                int fileBlocks = fileSize / disk.getBlockSize();
                                if (fileSize % disk.getBlockSize() > 0) {
                                    fileBlocks++;
                                }

                                File outFile = Paths.get(path, thead.getName()).toFile();
                                if (!outFile.getParentFile().exists()) {
                                    // In case the directory was not created due to errors
                                    outFile.getParentFile().mkdirs();
                                }
                                if (outFile.exists()) {
                                    outFile.delete();
                                }
                                FileOutputStream fo = new FileOutputStream(outFile);
                                for (int i = 0; i < fileBlocks; i++) {
                                    byte[] data;
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

                                if (modified.before(new Date())) {
                                    try {
                                        outFile.setLastModified(modified.getTime());
                                    } catch (IllegalArgumentException e) {
                                        //
                                    }
                                }
                            } catch (IOException e) {
                                out.printf("Error extracting file %s: %s\n", thead.getName(), e.getMessage());
                            }
                        }
                        break;

                    case TYPE_DIRECTORY:
                        File newDir = Paths.get(path, thead.getName()).toFile();
                        if (!newDir.exists()) {
                            newDir.mkdirs();
                        }

                        Date modified;
                        try {
                            modified = CromixTime.from(thead.getMtime()).toDate();
                        } catch (ParseException e) {
                            e.printStackTrace();
                            modified = new Date();
                        }
                        if (modified.before(new Date())) {
                            try {
                                newDir.setLastModified(modified.getTime());
                            } catch (IllegalArgumentException e) {
                                //
                            }
                        }
                        break;
                }
            }
        }
    }

    public void create(String path, OutputStream archive, PrintStream out) {
        disk.reset();
        File root = new File(path);
        try {
            if (root.isDirectory()) {
                create(root, root, out);
            } else {
                File parent = root.getParentFile();
                create(parent, parent, out);
                create(parent, root, out);
            }
        } catch (ImageException e) {
            out.println(e.getMessage());
            if (!e.getCode().equals(CODE_END_OF_DISK)) {
                e.printStackTrace(out);
            }
        } catch (Exception e) {
            out.println(e.getMessage());
            e.printStackTrace(out);
        }

        // Fill the remainder of the disk with empty (0) blocks
        byte[] empty = new byte[disk.getBlockSize()];
        try {
            while (disk.getBlockCount() - disk.getCurrentBlockNumber() > 0) {
                disk.setNextBlock(empty);
            }
        } catch (ImageException e) {
            if (!e.getCode().equals(CODE_END_OF_DISK)) {
                e.printStackTrace(out);
            }
        } catch (Exception e) {
            out.println(e.getMessage());
            e.printStackTrace(out);
        }

        disk.persist(archive);
    }

    protected void create(File base, File file, PrintStream out) throws SectorInvalidException, IOException {
        BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        long mtime = attrs.lastModifiedTime().toInstant().getEpochSecond()
                + 8 * 3600
                + ZoneId.systemDefault().getRules().getOffset(Instant.now()).getTotalSeconds();

        if (file.isDirectory()) {
            // Add directory header block
            FTarHeader header = new FTarHeader();
            header.setName(getRelativePath(base, file));
            header.setFlag(TYPE_DIRECTORY);
            header.setMtime(mtime);
            header.setLinkFlag(0);
            header.setLink(null);
            header.setSize(0);
            header.setUid(0x7FFF);
            header.setGid(0x7FFF);
            header.setAccess(0xD11);
            disk.setNextBlock(header.write());

            out.printf("Adding directory \"%s\", at block %d\n", header.getName(), disk.currentBlockNumber);

            for (File child : Arrays.stream(Objects.requireNonNull(file.listFiles())).sorted(Comparator.comparing(File::getName)).collect(Collectors.toList())) {
                create(base, child, out);
            }
        } else {
            long fileSize = Files.size(file.toPath());
            int requiredBlocks =
                    (int)(fileSize / disk.getBlockSize())             // Full blocks
                    + ((fileSize % disk.getBlockSize()) > 0 ? 1 : 0)  // Partial block, if required
                    + 1;                                              // header block

            if (disk.getBlockCount() - disk.getCurrentBlockNumber() < requiredBlocks) {
                throw new ImageException(CODE_END_OF_DISK, String.format("Insufficient remaining blocks for file %s (%d blocks)", getRelativePath(base, file), requiredBlocks));
            }

            // Add file header block
            FTarHeader header = new FTarHeader();
            header.setName(getRelativePath(base, file));
            header.setFlag(TYPE_ORDINARY);
            header.setMtime(mtime);
            header.setLinkFlag(0);
            header.setLink(null);
            header.setSize(fileSize);
            header.setUid(0x7FFF);
            header.setGid(0x7FFF);
            header.setAccess(0xD11);
            disk.setNextBlock(header.write());

            out.printf("Adding file      \"%s\" (%d bytes = %d blocks), at block %d\n", header.getName(), fileSize, requiredBlocks, disk.currentBlockNumber);

            FileInputStream in = new FileInputStream(file);
            byte[] data = new byte[disk.getBlockSize()];
            while (fileSize > 0) {
                int read = in.read(data);
                disk.setNextBlock(data);
                fileSize -= read;
            }
        }
    }

    protected String getRelativePath(File base, File file) {
        Path rel = base.toPath().relativize(file.toPath());
        return StringUtils.isBlank(rel.toString()) ? "." : rel.toString();
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
