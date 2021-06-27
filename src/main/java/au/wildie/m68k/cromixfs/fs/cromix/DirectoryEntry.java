package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

import static au.wildie.m68k.cromixfs.fs.cromix.DirectoryEntryStatus.NOT_ALLOCATED;
import static au.wildie.m68k.cromixfs.fs.cromix.DirectoryEntryStatus.ALLOCATED;
import static au.wildie.m68k.cromixfs.utils.BinUtils.*;

@Getter
@Setter
public class DirectoryEntry {
    public static final int DIRECTORY_ENTRY_LENGTH = 0x20;

    private static final int NAME_LENGTH = 0x18;
    private static final int OFFSET_STATUS = 0x1c;
    private static final int OFFSET_INODE = 0x1e;
    private static final int FLAG_ALLOCATED = 0x8000;

    private DirectoryEntryStatus type;
    private String name;
    private int inodeNumber;
    private boolean dirty;
    private final DirectoryBlock directoryBlock;

    public static DirectoryEntry from(byte[] data, int offset, DirectoryBlock directoryBlock) {
        int status = readWord(data, offset + OFFSET_STATUS);
        if (status == FLAG_ALLOCATED) {
            DirectoryEntry entry = new DirectoryEntry(ALLOCATED, directoryBlock);
            entry.name = readString(data, offset, NAME_LENGTH);
            entry.inodeNumber = readWord(data, offset + OFFSET_INODE);
            return entry;
        } else {
            return new DirectoryEntry(NOT_ALLOCATED, directoryBlock);
        }
    }

    public DirectoryEntry(DirectoryEntryStatus type, DirectoryBlock directoryBlock) {
        this.type = type;
        this.directoryBlock = directoryBlock;
    }

    public void flush(DiskInterface disk) {
        directoryBlock.flush(disk);
    }

    public void toBytes(byte[] data, int offset) {
        if (name != null) {
            writeString(name, data, offset, NAME_LENGTH);
            writeWord(type == ALLOCATED ? FLAG_ALLOCATED : 0, data, offset + OFFSET_STATUS);
            writeWord(inodeNumber, data, offset + OFFSET_INODE);
        } else {
            Arrays.fill(data, offset, offset + DIRECTORY_ENTRY_LENGTH, (byte)0);
        }
    }
}
