package au.wildie.m68k.cromixfs.fs;

import java.io.PrintStream;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CromixBlockStats {
    private int fileBlocks;
    private int directoryBlocks;
    private int orphanedBlock;
    private int duplicateBlocks;
    private int onFreeList;
    private int freeListBlocks;
    private final int availableBlocks;
    private int files;
    private int devices;
    private int directories;

    public CromixBlockStats(SuperBlock superBlock) {
        availableBlocks = superBlock.getDataBlockCount();
    }

    public void print(PrintStream out) {
        out.print("\nBlocks:\n");
        out.printf("  Directory:       %5d\n", directoryBlocks);
        out.printf("  File:            %5d\n", fileBlocks);
        out.printf("  On free list:    %5d\n", onFreeList);
        out.printf("  Orphaned:        %5d\n", orphanedBlock);
        out.printf("  Total:           %5d\n", directoryBlocks + fileBlocks + onFreeList + orphanedBlock);
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
}
