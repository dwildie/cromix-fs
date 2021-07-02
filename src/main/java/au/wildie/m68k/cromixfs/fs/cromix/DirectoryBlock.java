package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.Arrays;

import static au.wildie.m68k.cromixfs.fs.cromix.DirectoryEntry.DIRECTORY_ENTRY_LENGTH;
import static au.wildie.m68k.cromixfs.fs.cromix.DirectoryEntryStatus.NOT_ALLOCATED;
import static au.wildie.m68k.cromixfs.fs.cromix.DirectoryEntryStatus.ALLOCATED;

@Getter
@Setter
public class DirectoryBlock {
    public static final int DIRECTORY_ENTRIES = 0x10;

    private int blockNumber;
    private DirectoryEntry[] entries = new DirectoryEntry[DIRECTORY_ENTRIES];
    private boolean dirty;

    public static DirectoryBlock from(DiskInterface disk, int blockNumber) {
        try {
            byte[] data = disk.getBlock(blockNumber);
            DirectoryBlock directoryBlock = new DirectoryBlock(blockNumber, false);
            for (int i = 0; i < DIRECTORY_ENTRIES; i++) {
                directoryBlock.entries[i] = DirectoryEntry.from(data, i, directoryBlock);
            }
            return directoryBlock;
        } catch (IOException e) {
            throw new BlockUnavailableException(blockNumber, e);
        }
    }

    public DirectoryBlock(int blockNumber, boolean initialise) {
        this.blockNumber = blockNumber;
        if (initialise) {
            for (int i = 0; i < DIRECTORY_ENTRIES; i++) {
                entries[i] = new DirectoryEntry(i, NOT_ALLOCATED, this);
            }
            dirty = true;
        }
    }

    public DirectoryEntry findEntry(String name) {
        return Arrays.stream(entries).filter(entry -> entry.getStatus() == ALLOCATED && entry.getName().equals(name)).findFirst().orElse(null);
    }

    public DirectoryEntry getFirstUnusedEntry() {
        return Arrays.stream(entries).filter(entry -> entry.getStatus() == NOT_ALLOCATED).findFirst().orElse(null);
    }

    public int getUnusedEntries() {
        return (int)Arrays.stream(entries).filter(entry -> entry.getStatus() == NOT_ALLOCATED).count();
    }

    public void flush(DiskInterface disk) {
//        System.out.printf("Flush directory block %d\n", blockNumber);
        try {
            byte[] data = disk.getBlock(blockNumber);
            for (int i = 0; i < DIRECTORY_ENTRIES; i++) {
                entries[i].toBytes(data, i * DIRECTORY_ENTRY_LENGTH);
            }
        } catch (IOException e) {
            throw new BlockUnavailableException(blockNumber, e);
        }
    }

    public int getExtent() {
        int last = -1;
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].getStatus() == ALLOCATED) {
                last = i;
            }
        }
        return (last + 1) * DIRECTORY_ENTRY_LENGTH;
    }
}
