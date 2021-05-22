package au.wildie.m68k.cromixfs.disk.imd;

import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class IMDImage {
    private String header = "";
    private final List<Track> tracks = new ArrayList<>();

    private static final int SECTOR_ENCODING_UNAVAILABLE = 0;
    private static final int SECTOR_ENCODING_NORMAL = 1;
    private static final int SECTOR_ENCODING_COMPRESSED = 2;
    private static final int SECTOR_ENCODING_DELETED = 3;
    private static final int SECTOR_ENCODING_DELETED_COMPRESSED = 4;
    private static final int SECTOR_ENCODING_ERROR = 5;
    private static final int SECTOR_ENCODING_ERROR_COMPRESSED = 6;
    private static final int SECTOR_ENCODING_DELETED_ERROR = 7;
    private static final int SECTOR_ENCODING_DELETED_ERROR_COMPRESSED = 8;

    private static final Set<Integer> SECTOR_ENCODING_VALID = new HashSet<>(Arrays.asList(
            SECTOR_ENCODING_NORMAL,
            SECTOR_ENCODING_COMPRESSED,
            SECTOR_ENCODING_DELETED,
            SECTOR_ENCODING_DELETED_COMPRESSED));

    public IMDImage(int driveId, String filePath, PrintStream out) {
        out.printf("Reading IMD file %s%n", filePath);

        File imdFile = new File(filePath);
        if (!imdFile.exists()) {
            out.printf("Drive %d: IMD file %s does not exist%n", driveId, imdFile.getPath());
            return;
        }

        byte[] raw;
        try (InputStream src = new FileInputStream(imdFile)) {
            raw = new byte[(int) imdFile.length()];
            src.read(raw);
        } catch (IOException e) {
            throw new ImageException(String.format("Drive %d: error reading IMD file %s", driveId, imdFile.getPath()), e);
        }

        int index = 0;

        // Read the header
        while (raw[index] != 0x1a) {
            header = header + (char) raw[index++];
        }
        index++; // Skip EOF

        int offset = 0;

        while (index < raw.length) {
            Track track = new Track();

            // Track header
            track.setMode(raw[index++]);
            track.setCylinder(raw[index++]);
            track.setHead(raw[index++]);
            track.setSectorCount(raw[index++]);
            track.setSectorSize(decodeSectorSize(raw[index++]));
            track.setOffset(offset);

            //out.printf("C=%d, H=%d, Sectors=%d, SectorSize=%d\n", track.getCylinder(), track.getHead(), track.getSectorCount(), track.getSectorSize());

            // Sector number map
            int[] sectorMap = new int[track.getSectorCount()];
            for (int i = 0; i < sectorMap.length; i++) {
                sectorMap[i] = raw[index++];
            }
            track.setSectorMap(sectorMap);

            // Read each sector
            for (int i = 0; i< sectorMap.length; i++) {
                Sector sector = new Sector(track.getSectorSize());

                sector.setNumber(sectorMap[i]);
                sector.setEncoding(raw[index++]);
                sector.setOffset(offset);

                if (sector.getEncoding() == SECTOR_ENCODING_UNAVAILABLE) {
                    out.printf("Sector could not be read: cylinder %d, head %d, sector %d\n", track.getCylinder(), track.getHead(), sector.getNumber());
                } else if (sector.getEncoding() == SECTOR_ENCODING_NORMAL) {
                    // Normal data
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = raw[index++];
                    }
                } else if (sector.getEncoding() == SECTOR_ENCODING_COMPRESSED) {
                    // Compressed data
                    byte value = raw[index++];
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = value;
                    }
                } else if (sector.getEncoding() == SECTOR_ENCODING_DELETED) {
                    // Deleted data
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = raw[index++];
                    }
                } else if (sector.getEncoding() == SECTOR_ENCODING_DELETED_COMPRESSED) {
                    // Deleted compressed data
                    byte value = raw[index++];
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = value;
                    }
                } else if (sector.getEncoding() == SECTOR_ENCODING_ERROR) {
                    // Data with read errors
                    out.printf("Sector errors: cylinder %d, head %d, sector %d\n", track.getCylinder(), track.getHead(), sector.getNumber());
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = raw[index++];
                    }
                } else if (sector.getEncoding() == SECTOR_ENCODING_ERROR_COMPRESSED) {
                    // Compressed data with read errors
                    byte value = raw[index++];
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = value;
                    }
                } else if (sector.getEncoding() == SECTOR_ENCODING_DELETED_ERROR) {
                    // Deleted data with read errors
                    out.printf("Deleted sector errors: cylinder %d, head %d, sector %d\n", track.getCylinder(), track.getHead(), sector.getNumber());
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = raw[index++];
                    }
                } else if (sector.getEncoding() == SECTOR_ENCODING_DELETED_ERROR_COMPRESSED) {
                    // Compressed deleted data with read errors
                    byte value = raw[index++];
                    for (int j = 0; j < track.getSectorSize(); j++) {
                        sector.getData()[j] = value;
                    }
                } else {
                    out.printf("Unexpected sector encoding: cylinder %d, head %d, sector %d, encoding%d\n", track.getCylinder(), track.getHead(), sector.getNumber(), sector.getEncoding());
                }
                track.getSectors().add(sector);

                offset += sector.getData().length;
            }

            tracks.add(track);
        }
        out.println(header);
        out.printf("Read %d tracks%n", tracks.size());
    }


    public void persist(File imdFile) {
        if (imdFile.exists()) {
            imdFile.delete();

        }

        try (OutputStream out = new FileOutputStream(imdFile)) {
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
                    throw new ImageException("Can't write track data");
                }

                Arrays.stream(track.getSectorMap()).forEach(num -> {
                    try {
                        out.write(num);
                    } catch (IOException e) {
                        throw new ImageException("Can't write sector map number");
                    }
                });

                track.getSectors().forEach(sector -> {
                    try {
                        out.write(sector.getEncoding());
                        out.write(sector.getData());
                    } catch (IOException e) {
                        throw new ImageException("Can't write sector data");
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
            for (Track track : tracks) {
                for (Sector sector : track.getSectors()) {
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
        Track track = tracks.stream()
                .filter(t -> t.getCylinder() == cylinderNumber && t.getHead() == headNumber)
                .findFirst()
                .orElseGet(() -> {
                    Track t = new Track();
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
            Sector sector = new Sector(track.getSectorSize());
            sector.setNumber(sectorMap[i]);
            sector.setEncoding(1);
            track.getSectors().add(sector);
        }
    }

    public Track getTrack(int cylinderNumber, int headNumber) {
        return tracks.stream()
                .filter(track -> track.getCylinder() == cylinderNumber && track.getHead() == headNumber)
                .findFirst()
                .orElseThrow(() -> new ImageException(String .format("Cannot find cylinder %d, head %d", cylinderNumber, headNumber)));
    }

    public Sector getSector(int cylinderNumber, int headNumber, int sectorNumber) {
        return tracks.stream()
                .filter(track -> track.getCylinder() == cylinderNumber && track.getHead() == headNumber)
                .flatMap(track -> track.getSectors().stream())
                .filter(sector -> sector.getNumber() == sectorNumber)
                .findFirst()
                .orElseThrow(() -> new ImageException(String .format("Cannot find cylinder %d, head %d, sector %d.", cylinderNumber, headNumber, sectorNumber)));
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public int getTrackCount() {
        return tracks.size();
    }

    public Integer getTrackCount(int head) {
        return new Long(tracks.stream()
                .filter(track -> track.getHead() == head)
                .count()).intValue();
    }

    public Integer getSectorErrorCount() {
        return (int)tracks.stream()
                .flatMap(track -> track.getSectors().stream())
                .filter(sector -> !SECTOR_ENCODING_VALID.contains(sector.getEncoding()))
                .count();
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

        throw new ImageException(String.format("Unexpected sector size value %d", value));
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

        throw new ImageException(String.format("Unexpected sector size value %d", value));
    }


}
