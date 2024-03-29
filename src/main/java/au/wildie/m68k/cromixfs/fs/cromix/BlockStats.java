package au.wildie.m68k.cromixfs.fs.cromix;

import java.io.PrintStream;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockStats {
    private int fileBlocks;
    private int directoryBlocks;
    private int orphanedBlocks;
    private int duplicateBlocks;
    private int onFreeList;
    private int freeListBlocks;
    private final int availableBlocks;
    private int files;
    private int devices;
    private int directories;

    public BlockStats(SuperBlock superBlock) {
        this(superBlock.getDataBlockCount());
    }

    public BlockStats(int availableBlocks) {
        this.availableBlocks = availableBlocks;
    }

    public void print(PrintStream out) {
        out.print("\nBlocks:\n");
        out.printf("  Directory:       %5d\n", directoryBlocks);
        out.printf("  File:            %5d\n", fileBlocks);
        out.printf("  On free list:    %5d\n", onFreeList);
        out.printf("  Orphaned:        %5d\n", orphanedBlocks);
        out.printf("  Total:           %5d\n", getTotalBlocks());
        out.printf("  Available:       %5d\n", availableBlocks);
        out.printf("  Duplicate:       %5d\n", duplicateBlocks);

        out.print("\nFree Block list:\n");
        out.printf("  Total:           %5d\n", freeListBlocks);

        out.print("\n");
        out.printf("Files:             %5d\n", files);
        out.printf("Directories:       %5d\n", directories);
        out.printf("Devices:           %5d\n", devices);
        out.print("\n");
    }

    public int getTotalBlocks() {
        return directoryBlocks + fileBlocks + onFreeList + orphanedBlocks;
    }
}
