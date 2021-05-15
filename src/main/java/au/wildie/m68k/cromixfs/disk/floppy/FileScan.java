package au.wildie.m68k.cromixfs.disk.floppy;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.fs.FileSystem;
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
                .forEach(entry -> System.out.printf("\"%s\" %s\n", entry.getError(), entry.getRelativePath()));

        System.out.print("         total  tracks sector  image\n");
        System.out.print("label   tracks  / side errors  file\n");
        info.stream()
                .filter(entry -> entry.getError() == null)
                .sorted(Comparator.comparing(Info::getRelativePath))
                .forEach(entry -> System.out.printf("%-7s    %3d  %-7s %5d  %s\n",
                        entry.getFormatLabel().replaceAll("\\P{InBasic_Latin}", ""),
                        entry.getTracks(),
                        headTracks(entry),
                        entry.getSectorErrors(),
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
        Info info = new Info(getRelativePath(parent, file.getName()));

        try {
            FileSystem fs = FileSystems.getFloppyFileSystem(file.getAbsolutePath(), out);
            DiskInterface disk = fs.getDisk();
            info.setFormatLabel(disk.getFormatLabel());
            info.setTracks(disk.getTrackCount());
            info.setHead0Tracks(disk.getTrackCount(0));
            info.setHead1Tracks(disk.getTrackCount(1));
            info.setSectorErrors(disk.getSectorErrorCount());
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
    }
}
