package au.wildie.m68k.cromixfs.fs.cromix;

import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.UNUSED;
import static au.wildie.m68k.cromixfs.fs.cromix.SuperBlock.FREE_INODE_LIST_SIZE;
import java.io.PrintStream;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InodeStats {
    private final int expectedInodes;
    private final int freeListSize;

    private int freeInodes;
    private int fileInodes;
    private int directoryInodes;
    private int deviceInodes;
    private int pipeInodes;
    private int sharedTextInodes;
    private int errorInodes;
    private int freeInodeListUsed;
    private int freeInodeListAvailable;

    public InodeStats(SuperBlock superBlock) {
        this(superBlock.getInodeCount());
    }

    public InodeStats(int expectedInodes) {
        this.expectedInodes = expectedInodes;
        freeListSize = FREE_INODE_LIST_SIZE;
    }

    public void countFreeList(Inode inode) {
        if (inode.getType() == UNUSED) {
            freeInodeListAvailable++;
        } else {
            freeInodeListUsed++;
        }
    }

    public void countUsage(Inode inode) {
        switch (inode.getType()) {
            case UNUSED:
                freeInodes++;
                break;
            case FILE:
                fileInodes++;
                break;
            case DIRECTORY:
                directoryInodes++;
                break;
            case CHARACTER_DEVICE:
            case BLOCK_DEVICE:
                deviceInodes++;
                break;
            case PIPE:
                pipeInodes++;
                break;
            case SHARED_TEXT:
                sharedTextInodes++;
                break;
            default:
                errorInodes++;
        }
    }

    public void print(PrintStream out) {
        out.print("\nInodes:\n");
        out.printf("  Directory:       %5d\n", directoryInodes);
        out.printf("  File:            %5d\n", fileInodes);
        out.printf("  Device:          %5d\n", deviceInodes);
        out.printf("  Pipe:            %5d\n", pipeInodes);
        out.printf("  Shared text:     %5d\n", sharedTextInodes);
        out.printf("  Used:            %5d\n", getUsedInodes());
        out.printf("  Error:           %5d\n", errorInodes);
        out.printf("  Free:            %5d\n", freeInodes);
        out.printf("  Total:           %5d\n", getTotalInodes());
        out.printf("  Expected:        %5d\n", expectedInodes);
        out.print("\nFree Inode list:\n");
        out.printf("  Used:            %5d\n", freeInodeListUsed);
        out.printf("  Available:       %5d\n", freeInodeListAvailable);
        out.printf("  Total:           %5d\n", freeInodeListUsed + freeInodeListAvailable);
        out.printf("  Expected:        %5d\n", freeListSize);
    }

    public int getUsedInodes() {
        return fileInodes + directoryInodes + deviceInodes + pipeInodes + sharedTextInodes;
    }

    public int getTotalInodes() {
        return freeInodes + fileInodes + directoryInodes + deviceInodes + pipeInodes + sharedTextInodes + errorInodes;
    }
}
