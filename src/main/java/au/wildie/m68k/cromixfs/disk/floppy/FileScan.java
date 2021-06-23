package au.wildie.m68k.cromixfs.disk.floppy;

import au.wildie.m68k.cromixfs.disk.DiskInfo;
import au.wildie.m68k.cromixfs.fs.FileSystemOps;
import au.wildie.m68k.cromixfs.fs.FileSystems;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class FileScan {
    private final String basePath;

    public void scan() throws IOException {
        File base = new File(basePath);
        if (!base.exists()) {
            System.err.printf("Specified path %s does not exist\n", basePath);
            return;
        }

        PrintStream out = new PrintStream(File.createTempFile("FileScan", ".out"));
        List<Info> info = new ArrayList<>();

        if (base.isDirectory()) {
            info.addAll(scan(null, base, out));
        }
        if (base.isFile()) {
            info.add(getInfo(null, base, out));
        }

        info.stream()
                .filter(entry -> entry.getError() != null)
                .sorted(Comparator.comparing(Info::getRelativePath))
                .forEach(entry -> System.out.printf("Error: \"%s\" %s\n", entry.getError(), entry.getRelativePath()));

        System.out.print("\n\n");
        System.out.print("         total  tracks  sector\n");
        System.out.print("label   tracks  / side  errors  usage              version  image file\n");
        info.stream()
                .filter(entry -> entry.getError() == null)
                .sorted(Comparator.comparing(Info::getRelativePath))
                .forEach(entry -> System.out.printf("%-7s    %3d  %-7s  %5d   %-17s %7s  %s\n",
                        Optional.ofNullable(entry.getFormatLabel()).map(label -> label.replaceAll("\\P{InBasic_Latin}", " ").trim()).orElse(""),
                        entry.getTracks(),
                        headTracks(entry),
                        entry.getSectorErrors(),
                        entry.getFileSystem(),
                        entry.getVersion(),
                        entry.getRelativePath()));
    }

    protected String headTracks (Info entry) {
        return String.format("[%d,%d]", entry.getHead0Tracks(), entry.getHead1Tracks());
    }

    protected List<Info> scan(String parent, File folder, PrintStream out) {
        List<Info> info = new ArrayList<>();
        String relativePath = getRelativePath(parent, folder.getName());

        for (File entry : folder.listFiles()) {
            if (entry.isDirectory()) {
                info.addAll(scan(relativePath, entry, out));
            } else if (FilenameUtils.getExtension(entry.getName()).equalsIgnoreCase("imd")) {
                info.add(getInfo(relativePath, entry, out));
            }
        }

        return info;
    }

    protected String getRelativePath(String parent, String child) {
        return Optional.ofNullable(parent).map(p -> p + "/" + child).orElse(child);
    }

    protected Info getInfo(String parent, File file, PrintStream out) {
//        System.out.printf("Scanning %s\n", file.getPath());
        Info info = new Info(getRelativePath(parent, file.getName()));

        try {
            FileSystemOps fs = FileSystems.getIMDFloppyFileSystem(file.getAbsolutePath(), out);
            DiskInfo disk = fs.getDisk();
            info.setFormatLabel(disk.getFormatLabel());
            info.setTracks(disk.getTrackCount());
            info.setHead0Tracks(disk.getTrackCount(0));
            info.setHead1Tracks(disk.getTrackCount(1));
            info.setSectorErrors(disk.getSectorErrorCount());
            info.setFileSystem(fs.getName());
            info.setVersion(fs.getVersion());
        } catch (Exception e) {
            info.setError(e.getMessage());
        }
        return info;
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class Info {
        private final String relativePath;
        private String formatLabel;
        private Integer tracks;
        private Integer head0Tracks;
        private Integer head1Tracks;
        private Integer sectorErrors;
        private String error;
        private String fileSystem;
        private String version;
    }
}
