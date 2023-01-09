package au.wildie.m68k.cromixfs.disk.imd;

import au.wildie.m68k.cromixfs.disk.DiskImage;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.CromixFloppyInfo;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static au.wildie.m68k.cromixfs.disk.imd.ImageException.CODE_END_OF_DISK;
import static au.wildie.m68k.cromixfs.disk.imd.ImageException.CODE_ERROR;
import static java.lang.Integer.max;

@Getter
public class IMDImage extends DiskImage {
    private String header = "";
    private int heads= -1;
    private int cylinders= -1;
    private final List<IMDTrack> tracks = new ArrayList<>();

    public static final int SECTOR_ENCODING_UNAVAILABLE = 0;
    public static final int SECTOR_ENCODING_NORMAL = 1;
    public static final int SECTOR_ENCODING_COMPRESSED = 2;
    public static final int SECTOR_ENCODING_DELETED = 3;
    public static final int SECTOR_ENCODING_DELETED_COMPRESSED = 4;
    public static final int SECTOR_ENCODING_ERROR = 5;
    public static final int SECTOR_ENCODING_ERROR_COMPRESSED = 6;
    public static final int SECTOR_ENCODING_DELETED_ERROR = 7;
    public static final int SECTOR_ENCODING_DELETED_ERROR_COMPRESSED = 8;
    public static final int SECTOR_ENCODING_UNKNOWN = 9;

    private static final Set<Integer> SECTOR_ENCODING_VALID = new HashSet<>(Arrays.asList(
            SECTOR_ENCODING_NORMAL,
            SECTOR_ENCODING_COMPRESSED,
            SECTOR_ENCODING_DELETED,
            SECTOR_ENCODING_DELETED_COMPRESSED));

    public static IMDImage fromFile(int driveId, String filePath, PrintStream out) throws IOException {
        return fromFile(driveId, new File(filePath), out);
    }

    public static IMDImage fromFile(int driveId, File imdFile, PrintStream out) throws IOException {
        out.printf("Reading IMD file %s%n", imdFile.getPath());

        if (!imdFile.exists()) {
            out.printf("Drive %d: IMD file %s does not exist%n", driveId, imdFile.getPath());
            throw new ImageException(CODE_ERROR, String.format("Drive %d: IMD file %s does not exist%n", driveId, imdFile.getPath()));
        }

        try (InputStream src = new FileInputStream(imdFile)) {
            return fromStream(src, out);
        }
    }

    public static IMDImage fromStream(InputStream imdStream, PrintStream out) throws IOException {
        return new IMDImage(IOUtils.toByteArray(imdStream), out);
    }

    public IMDImage (int[] mode, CromixFloppyInfo info) {
        this.header = "IMD 1.17:  4/06/2010 13:59:22\r\n";
        this.cylinders = info.getCylinders();
        this.heads = info.getHeads();
        int offset = 0;

        // First track
        tracks.add(new IMDTrack(mode[0], 0, 0, info.getSectorsFirstTrack(), info.getBytesPerSectorFirstTrack(), offset));
        offset += (info.getSectorsFirstTrack() * info.getBytesPerSectorFirstTrack());

        // Second track
        tracks.add(new IMDTrack(mode[1], 0, 1, info.getSectorsPerTrack(), info.getBytesPerSector(), offset));
        offset += (info.getSectorsPerTrack() * info.getBytesPerSector());

        // All other tracks
        for (int c = 1; c < cylinders; c++) {
            for (int h = 0; h < heads; h++) {
                tracks.add(new IMDTrack(mode[1], c, h, info.getSectorsPerTrack(), info.getBytesPerSector(), offset));
                offset += (info.getSectorsPerTrack() * info.getBytesPerSector());
            }
        }
    }

    public IMDImage(byte[] raw, PrintStream out) {
        int index = 0;

        // Read the header
        while (raw[index] != 0x1a) {
            header = header + (char) raw[index++];
        }
        index++; // Skip EOF

        out.printf("IMD header: %s\n", header);

        int offset = 0;

        while (index < raw.length) {
            IMDTrack track = new IMDTrack();

            // Track header
            track.setMode(raw[index++]);
            track.setCylinder(raw[index++]);
            track.setHead(raw[index++]);
            track.setSectorCount(raw[index++]);
            track.setSectorSize(decodeSectorSize(raw[index++]));
            track.setOffset(offset);

            heads = max(heads, track.getHead());
            cylinders = max(cylinders, track.getCylinder());

            //out.printf("C=%d, H=%d, Sectors=%d, SectorSize=%d\n", track.getCylinder(), track.getHead(), track.getSectorCount(), track.getSectorSize());

            // Sector number map
            int[] sectorMap = new int[track.getSectorCount()];
            for (int i = 0; i < sectorMap.length; i++) {
                sectorMap[i] = raw[index++];
            }
            track.setSectorMap(sectorMap);

            // Read each sector
            for (int i = 0; i< sectorMap.length; i++) {
                IMDSector sector = new IMDSector(track.getSectorSize());

                sector.setNumber(sectorMap[i]);
                sector.setEncoding(raw[index++]);
                sector.setOffset(offset);
                sector.setSrcOffset(index);

                if (sector.getEncoding() == SECTOR_ENCODING_UNAVAILABLE) {
                    sector.setValid(false);
                    out.printf("Sector is \"Unavailable\" in IMD file: cylinder %d, head %d, sector %d\n", track.getCylinder(), track.getHead(), sector.getNumber());
                    // Assume normal data
                } else if (sector.getEncoding() == SECTOR_ENCODING_NORMAL) {
                    // Normal data
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = raw[index++];
                    }
                    sector.setValid(true);
                } else if (sector.getEncoding() == SECTOR_ENCODING_COMPRESSED) {
                    // Compressed data
                    byte value = raw[index++];
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = value;
                    }
                    sector.setValid(true);
                } else if (sector.getEncoding() == SECTOR_ENCODING_DELETED) {
                    // Deleted data
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = raw[index++];
                    }
                    sector.setValid(false);
                } else if (sector.getEncoding() == SECTOR_ENCODING_DELETED_COMPRESSED) {
                    // Deleted compressed data
                    byte value = raw[index++];
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = value;
                    }
                    sector.setValid(false);
                } else if (sector.getEncoding() == SECTOR_ENCODING_ERROR) {
                    // Data with read errors
                    out.printf("Sector errors: cylinder %d, head %d, sector %d\n", track.getCylinder(), track.getHead(), sector.getNumber());
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = raw[index++];
                    }
                    sector.setValid(false);
                } else if (sector.getEncoding() == SECTOR_ENCODING_ERROR_COMPRESSED) {
                    // Compressed data with read errors
                    out.printf("Sector errors: cylinder %d, head %d, sector %d\n", track.getCylinder(), track.getHead(), sector.getNumber());
                    byte value = raw[index++];
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = value;
                    }
                    sector.setValid(false);
                } else if (sector.getEncoding() == SECTOR_ENCODING_DELETED_ERROR) {
                    // Deleted data with read errors
                    out.printf("Deleted sector errors: cylinder %d, head %d, sector %d\n", track.getCylinder(), track.getHead(), sector.getNumber());
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = raw[index++];
                    }
                    sector.setValid(false);
                } else if (sector.getEncoding() == SECTOR_ENCODING_DELETED_ERROR_COMPRESSED) {
                    // Compressed deleted data with read errors
                    out.printf("Deleted sector errors: cylinder %d, head %d, sector %d\n", track.getCylinder(), track.getHead(), sector.getNumber());
                    byte value = raw[index++];
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = value;
                    }
                    sector.setValid(false);
                } else {
                    out.printf("Unexpected sector encoding: cylinder %d, head %d, sector %d, encoding %d\n", track.getCylinder(), track.getHead(), sector.getNumber(), sector.getEncoding());
                    sector.setValid(false);
                }
                track.getSectors().add(sector);

                offset += sector.getData().length;
            }

            tracks.add(track);
        }

        heads++;
        cylinders++;

        out.printf("Read %d tracks%n", tracks.size());
        out.printf("%n%s%n", getSectorErrorSummary());
    }

    public int size() {
        return tracks.stream()
                .mapToInt(track -> track.getSectorCount() * track.getSectorSize())
                .sum();
    }

    public void persist(File imdFile) {
        if (imdFile.exists()) {
            imdFile.delete();
        }

        try (OutputStream out = new FileOutputStream(imdFile)) {
            persist(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void persist(OutputStream out) {
        try {
            out.write(header.getBytes(StandardCharsets.US_ASCII));
            out.write(0x1a);
            tracks.forEach(track -> {
                try {
                    out.write(track.getMode());
                    out.write(track.getCylinder());
                    out.write(track.getHead());
                    out.write(track.getSectorCount());
                    out.write(encodeSectorSize(track.getSectorSize()));
                } catch (IOException e) {
                    throw new ImageException(CODE_ERROR, "Can't write track data");
                }

                Arrays.stream(track.getSectorMap()).forEach(num -> {
                    try {
                        out.write(num);
                    } catch (IOException e) {
                        throw new ImageException(CODE_ERROR, "Can't write sector map number");
                    }
                });

                track.getSectors().forEach(sector -> {
                    try {
                        out.write(sector.getEncoding());
                        if (sector.getEncoding() == SECTOR_ENCODING_NORMAL
                         || sector.getEncoding() == SECTOR_ENCODING_DELETED
                         || sector.getEncoding() == SECTOR_ENCODING_DELETED_ERROR
                         || sector.getEncoding() == SECTOR_ENCODING_ERROR) {
                            // Uncompressed, write all bytes
                            out.write(sector.getData());
                        } else {
                            // Compressed, write one byte, all other bytes are the same as the first byte
                            out.write(sector.getData()[0]);
                        }
                    } catch (IOException e) {
                        throw new ImageException(CODE_ERROR, "Can't write sector data");
                    }
                });
            });
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void persistAsImage(File file) {
        if (file.exists()) {
            file.delete();
        }

        try (OutputStream out = new FileOutputStream(file)) {
            for (IMDTrack track : tracks) {
                for (IMDSector sector : track.getSectors()) {
                    out.write(sector.getData());
                }
            }
//            tracks.stream()
////                    .sorted(Comparator.comparing(Track::getCylinder).thenComparing(Track::getHead))
//                    .forEach(track -> {
//                        track.getSectors().stream()
////                                .sorted(Comparator.comparing(Sector::getNumber))
//                                .forEach(sector -> {
//                                    try {
//                                        out.write(sector.getData());
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                });
//                    });

            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initTrack(int cylinderNumber, int headNumber, Pair<Integer,Integer> sectorInfo) {
        IMDTrack track = tracks.stream()
                .filter(t -> t.getCylinder() == cylinderNumber && t.getHead() == headNumber)
                .findFirst()
                .orElseGet(() -> {
                    IMDTrack t = new IMDTrack();
                    t.setCylinder(cylinderNumber);
                    t.setHead(headNumber);
                    tracks.add(t);
                    return t;
                });

        track.getSectors().clear();
        track.setSectorCount(sectorInfo.getLeft());
        track.setSectorSize(sectorInfo.getRight());

        int[] sectorMap = new int[track.getSectorCount()];
        for (int i = 0; i < sectorMap.length; i++) {
            sectorMap[i] = i+1;
        }
        track.setSectorMap(sectorMap);

        for (int i = 0; i < track.getSectorCount(); i++) {
            IMDSector sector = new IMDSector(track.getSectorSize());
            sector.setNumber(sectorMap[i]);
            sector.setEncoding(1);
            track.getSectors().add(sector);
        }
    }

    public IMDTrack getTrack(int trackIndex) {
        return tracks.get(trackIndex);
    }

    public IMDTrack getTrack(int cylinderNumber, int headNumber) {
        if (cylinderNumber >= this.cylinders) {
            throw new ImageException(CODE_END_OF_DISK, String.format("Cylinder %d does not exist", cylinderNumber));
        }
        return tracks.stream()
                .filter(track -> track.getCylinder() == cylinderNumber && track.getHead() == headNumber)
                .findFirst()
                .orElseThrow(() -> new ImageException(CODE_ERROR, String.format("Cannot find cylinder %d, head %d", cylinderNumber, headNumber)));
    }

    public IMDSector getSector(int cylinderNumber, int headNumber, int sectorNumber) {
        if (cylinderNumber >= this.cylinders) {
            throw new ImageException(CODE_END_OF_DISK, String.format("Cylinder %d does not exist", cylinderNumber));
        }
        return tracks.stream()
                .filter(track -> track.getCylinder() == cylinderNumber && track.getHead() == headNumber)
                .flatMap(track -> track.getSectors().stream())
                .filter(sector -> sector.getNumber() == sectorNumber)
                .findFirst()
                .orElseThrow(() -> new ImageException(CODE_ERROR, String .format("Cannot find cylinder %d, head %d, sector %d.", cylinderNumber, headNumber, sectorNumber)));
    }

    public int getTrackCount() {
        return tracks.size();
    }

    @Override
    public int getCylinders() {
        return cylinders;
    }

    public static boolean isValidEncoding(IMDSector sector) {
        return SECTOR_ENCODING_VALID.contains(sector.getEncoding());
    }

    public Integer getTrackCount(int head) {
        return new Long(tracks.stream()
                .filter(track -> track.getHead() == head)
                .count()).intValue();
    }

    public Integer getSectorErrorCount() {
        return (int)tracks.stream()
                .flatMap(track -> track.getSectors().stream())
                .filter(sector -> !isValidEncoding(sector))
                .count();
    }

    public String getSectorErrorSummary() {
        Integer[] counts = new Integer[]{0,0,0,0,0,0,0,0,0,0};
        tracks.stream()
                .flatMap(track -> track.getSectors().stream())
                .filter(sector -> !isValidEncoding(sector))
                .map(IMDSector::getEncoding)
                .map(encoding -> encoding >= SECTOR_ENCODING_UNKNOWN ? SECTOR_ENCODING_UNKNOWN : encoding)
                .forEach(encoding -> counts[encoding]++);

        StringBuilder summary = new StringBuilder();
        if (counts[SECTOR_ENCODING_UNAVAILABLE] > 0) {
            summary.append(String.format("  Unavailable:    %d\n", counts[SECTOR_ENCODING_UNAVAILABLE]));
        }
        if (counts[SECTOR_ENCODING_ERROR] > 0 || counts[SECTOR_ENCODING_ERROR_COMPRESSED] > 0) {
            summary.append(String.format("  Errors:         %d\n", counts[SECTOR_ENCODING_ERROR] + counts[SECTOR_ENCODING_ERROR_COMPRESSED]));

        }
        if (counts[SECTOR_ENCODING_DELETED_ERROR] > 0 || counts[SECTOR_ENCODING_DELETED_ERROR_COMPRESSED] > 0) {
            summary.append(String.format("  Deleted Errors: %d\n", counts[SECTOR_ENCODING_DELETED_ERROR] + counts[SECTOR_ENCODING_DELETED_ERROR_COMPRESSED]));
        }

        if (summary.length() == 0) {
            return "No sector errors\n";
        }

        summary.insert(0, "Sector errors:\n");
        return summary.toString();
    }

    public void verify(PrintStream out) {

        // Check for missing sectors
        tracks.forEach(track -> {
            if (track.getSectorCount() != 16) {
                out.println("");
            }
            Set<Integer> expectedSectors = IntStream.rangeClosed(1, track.getSectorCount())
                    .boxed().collect(Collectors.toSet());
            expectedSectors.removeAll(track.getSectors().stream()
                                        .map(IMDSector::getNumber)
                                        .collect(Collectors.toSet()));
            if (expectedSectors.size() > 0) {
                out.printf("Cylinder %d, head %d, missing sectors %s", track.getCylinder(), track.getCylinder(),
                   expectedSectors.stream()
                           .sorted()
                           .map(Object::toString)
                           .collect(Collectors.joining(", ")));
            }
        });
    }

    private int decodeSectorSize(int value) {
        if (value == 0) {
            return 128;
        }
        if (value == 1) {
            return 256;
        }
        if (value == 2) {
            return 512;
        }
        if (value == 3) {
            return 1024;
        }
        if (value == 4) {
            return 2048;
        }
        if (value == 5) {
            return 4096;
        }
        if (value == 6) {
            return 8192;
        }

        throw new ImageException(CODE_ERROR, String.format("Unexpected sector size value %d", value));
    }

    private int encodeSectorSize(int value) {
        if (value == 128) {
            return 0;
        }
        if (value == 256) {
            return 1;
        }
        if (value == 512) {
            return 2;
        }
        if (value == 1024) {
            return 3;
        }
        if (value == 2048) {
            return 4;
        }
        if (value == 4096) {
            return 5;
        }
        if (value == 8192) {
            return 6;
        }

        throw new ImageException(CODE_ERROR, String.format("Unexpected sector size value %d", value));
    }


}
