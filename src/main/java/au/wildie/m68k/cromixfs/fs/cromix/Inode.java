package au.wildie.m68k.cromixfs.fs.cromix;

import static au.wildie.m68k.cromixfs.fs.CromixTime.TIME_SIZE;
import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.*;
import static au.wildie.m68k.cromixfs.fs.cromix.PointerBlock.BLOCK_POINTER_COUNT;
import static au.wildie.m68k.cromixfs.utils.BinUtils.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.fs.CromixTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Inode {
    public static final int ACCESS_READ   = 0x01;            /* read access                  */
    public static final int ACCESS_EXEC   = 0x02;            /* execute access               */
    public static final int ACCESS_WRITE  = 0x04;            /* write access                 */
    public static final int ACCESS_APPEND = 0x08;            /* append access                */

    public static final int INODE_LENGTH = 0x80;
    public static final int INODES_PER_BLOCK = 4;

    public static final int INODE_OWNER_OFFSET            = 0x00;
    public static final int INODE_GROUP_OFFSET            = 0x02;
    public static final int INODE_PERMISSION_OWNER_OFFSET = 0x04;
    public static final int INODE_PERMISSION_GROUP_OFFSET = 0x05;
    public static final int INODE_PERMISSION_OTHER_OFFSET = 0x06;
    public static final int INODE_TYPE_OFFSET             = 0x07;
    public static final int INODE_LINKS_OFFSET            = 0x08;
    public static final int INODE_FILLER_OFFSET           = 0x09;
    public static final int INODE_FILE_SIZE_OFFSET        = 0x0A;
    public static final int INODE_NUMBER_OFFSET           = 0x0E;
    public static final int INODE_PARENT_OFFSET           = 0x10;
    public static final int INODE_DIR_ENTRY_COUNT_OFFSET  = 0x12;
    public static final int INODE_DEV_MAJOR_OFFSET        = 0x12;
    public static final int INODE_DEV_MINOR_OFFSET        = 0x13;
    public static final int INODE_USED_BLOCKS_OFFSET      = 0x14;
    public static final int INODE_CREATED_OFFSET          = 0x18;
    public static final int INODE_MODIFIED_OFFSET         = 0x1E;
    public static final int INODE_ACCESSED_OFFSET         = 0x24;
    public static final int INODE_DUMPED_OFFSET           = 0x2A;
    public static final int INODE_POINTERS_OFFSET         = 0x30;

    public static final int INODE_BLOCKS = 20;

    public static final int INDIRECT_1_BLOCK = 0x10;
    public static final int INDIRECT_2_BLOCK = 0x11;
    public static final int INDIRECT_3_BLOCK = 0x12;

    public static final Set<InodeType> ALLOCATED = new HashSet<>(Arrays.asList(FILE, DIRECTORY, CHARACTER_DEVICE, BLOCK_DEVICE, PIPE, SHARED_TEXT));

    private int number;
    private int parent;
    private InodeType type;
    private int owner;
    private int group;
    private int directoryEntryCount;
    private int fileSize;
    private int usedBlockCount;
    private int major;
    private int minor;
    private int ownerPermission;
    private int groupPermission;
    private int otherPermission;
    private int links;
    private CromixTime created;
    private CromixTime modified;
    private CromixTime accessed;
    private CromixTime dumped;
    private int[] blocks = new int[INODE_BLOCKS];

    private boolean dirty = true;

    public static Inode from(byte[] raw) {
        Inode inode = new Inode(0xFFFF & readWord(raw, INODE_NUMBER_OFFSET));
        inode.parent = 0xFFFF & readWord(raw, INODE_PARENT_OFFSET);
        inode.type = InodeType.from(0xFF & raw[INODE_TYPE_OFFSET]);
        inode.owner = 0xFFFF & readWord(raw, INODE_OWNER_OFFSET);
        inode.group = 0xFFFF & readWord(raw, INODE_GROUP_OFFSET);
        if (inode.type == CHARACTER_DEVICE || inode.type == BLOCK_DEVICE) {
            inode.major = 0xFF & raw[INODE_DEV_MAJOR_OFFSET];
            inode.minor = 0xFF & raw[INODE_DEV_MINOR_OFFSET];
        } else {
            inode.directoryEntryCount = 0xFFFF & readWord(raw, INODE_DIR_ENTRY_COUNT_OFFSET);
        }
        inode.fileSize = readDWord(raw, INODE_FILE_SIZE_OFFSET);
        inode.usedBlockCount = readDWord(raw, INODE_USED_BLOCKS_OFFSET);
        inode.ownerPermission = 0xFF & raw[INODE_PERMISSION_OWNER_OFFSET];
        inode.groupPermission = 0xFF & raw[INODE_PERMISSION_GROUP_OFFSET];
        inode.otherPermission = 0xFF & raw[INODE_PERMISSION_OTHER_OFFSET];
        inode.links = 0xFF & raw[INODE_LINKS_OFFSET];
        inode.created = CromixTime.from(Arrays.copyOfRange(raw, INODE_CREATED_OFFSET, INODE_CREATED_OFFSET + TIME_SIZE));
        inode.modified = CromixTime.from(Arrays.copyOfRange(raw, INODE_MODIFIED_OFFSET, INODE_MODIFIED_OFFSET + TIME_SIZE));
        inode.accessed = CromixTime.from(Arrays.copyOfRange(raw, INODE_ACCESSED_OFFSET, INODE_ACCESSED_OFFSET + TIME_SIZE));
        inode.dumped = CromixTime.from(Arrays.copyOfRange(raw, INODE_DUMPED_OFFSET, INODE_DUMPED_OFFSET + TIME_SIZE));

        for (int i = 0; i < INODE_BLOCKS; i++) {
            inode.blocks[i] = readDWord(raw, INODE_POINTERS_OFFSET + i * 4);
        }

//        if (inode.type != UNUSED && inode.type != UNKNOWN) {
//            inode.dump(raw, INODE_CREATED_OFFSET);
//        }
        return inode;
    }

    public Inode(int number) {
        this.number = number;
        this.type = UNUSED;
    }

    public void toBytes(byte[] data, int offset) {
        writeWord(number, data, offset + INODE_NUMBER_OFFSET);
        writeWord(parent, data, offset + INODE_PARENT_OFFSET);
        data[offset + INODE_TYPE_OFFSET] = (byte)type.to();
        data[offset + INODE_PERMISSION_OWNER_OFFSET] = (byte)ownerPermission;
        data[offset + INODE_PERMISSION_GROUP_OFFSET] = (byte)groupPermission;
        data[offset + INODE_PERMISSION_OTHER_OFFSET] = (byte)otherPermission;
        writeWord(owner, data, offset + INODE_OWNER_OFFSET);
        writeWord(group, data, offset + INODE_GROUP_OFFSET);
        if (type == CHARACTER_DEVICE || type == BLOCK_DEVICE) {
            data[offset + INODE_DEV_MAJOR_OFFSET] = (byte)(0xFF & major);
            data[offset + INODE_DEV_MINOR_OFFSET] = (byte)(0xFF & minor);
        } else {
            writeWord(directoryEntryCount, data, offset + INODE_DIR_ENTRY_COUNT_OFFSET);
        }
        writeDWord(fileSize, data, offset + INODE_FILE_SIZE_OFFSET);
        writeDWord(usedBlockCount, data, offset + INODE_USED_BLOCKS_OFFSET);
        data[offset + INODE_LINKS_OFFSET] = (byte)(0xFF & links);
        data[offset + INODE_FILLER_OFFSET] = (byte)0;
        timeToBytes(created, data, offset + INODE_CREATED_OFFSET);
        timeToBytes(modified, data, offset + INODE_MODIFIED_OFFSET);
        timeToBytes(accessed, data, offset + INODE_ACCESSED_OFFSET);
        timeToBytes(dumped, data, offset + INODE_DUMPED_OFFSET);

        for (int i = 0; i < INODE_BLOCKS; i++) {
            writeDWord(blocks[i], data, offset + INODE_POINTERS_OFFSET + i * 4);
        }
    }

    public int countUsedBlocks(DiskInterface disk) {
        AtomicInteger used = new AtomicInteger();
        for (int i = 0; i < INDIRECT_1_BLOCK; i++) {
            if (blocks[i] != 0) {
                used.getAndIncrement();
            }
        }

        if (blocks[INDIRECT_1_BLOCK] != 0) {
            used.addAndGet((int) PointerBlock.from(blocks[INDIRECT_1_BLOCK], disk).getPointerList().stream()
                    .filter(blockNumber -> blockNumber != 0)
                    .count() + 1);
        }

        if (blocks[INDIRECT_2_BLOCK] != 0) {
            used.getAndIncrement();
            PointerBlock.from(blocks[INDIRECT_2_BLOCK], disk).getPointerList().stream()
                    .filter(blockNumber -> blockNumber != 0)
                    .peek(blockNumber -> used.getAndIncrement())
                    .map(blockNumber -> PointerBlock.from(blockNumber, disk))
                    .flatMap(pointerBlock -> pointerBlock.getPointerList().stream())
                    .filter(blockNumber -> blockNumber != 0)
                    .forEach(blockNumber -> used.getAndIncrement());
        }

        if (blocks[INDIRECT_3_BLOCK] != 0) {
            used.getAndIncrement();
            PointerBlock.from(blocks[INDIRECT_3_BLOCK], disk).getPointerList().stream()
                    .filter(blockNumber -> blockNumber != 0)
                    .peek(blockNumber -> used.getAndIncrement())
                    .map(blockNumber -> PointerBlock.from(blockNumber, disk))
                    .flatMap(pointerBlock -> pointerBlock.getPointerList().stream())
                    .filter(blockNumber -> blockNumber != 0)
                    .peek(blockNumber -> used.getAndIncrement())
                    .map(blockNumber -> PointerBlock.from(blockNumber, disk))
                    .flatMap(pointerBlock -> pointerBlock.getPointerList().stream())
                    .filter(blockNumber -> blockNumber != 0)
                    .forEach(blockNumber -> used.getAndIncrement());
        }

        return used.get();
    }

    public List<Integer> getDataBlocks(DiskInterface disk) {
        List<Integer> dataBlocks = new ArrayList<>();
        for (int i = 0; i < INDIRECT_1_BLOCK; i++) {
            if (blocks[i] != 0) {
                dataBlocks.add(blocks[i]);
            }
        }

        if (blocks[INDIRECT_1_BLOCK] != 0) {
            dataBlocks.addAll(PointerBlock.from(blocks[INDIRECT_1_BLOCK], disk).getPointerList().stream()
                    .filter(blockNumber -> blockNumber != 0)
                    .collect(Collectors.toList()));
        }

        if (blocks[INDIRECT_2_BLOCK] != 0) {
            dataBlocks.addAll(PointerBlock.from(blocks[INDIRECT_2_BLOCK], disk).getPointerList().stream()
                    .filter(blockNumber -> blockNumber != 0)
                    .map(blockNumber -> PointerBlock.from(blockNumber, disk))
                    .flatMap(pointerBlock -> pointerBlock.getPointerList().stream())
                    .filter(blockNumber -> blockNumber != 0)
                    .collect(Collectors.toList()));
        }

        if (blocks[INDIRECT_3_BLOCK] != 0) {
            dataBlocks.addAll(PointerBlock.from(blocks[INDIRECT_3_BLOCK], disk).getPointerList().stream()
                    .filter(blockNumber -> blockNumber != 0)
                    .map(blockNumber -> PointerBlock.from(blockNumber, disk))
                    .flatMap(pointerBlock -> pointerBlock.getPointerList().stream())
                    .filter(blockNumber -> blockNumber != 0)
                    .map(blockNumber -> PointerBlock.from(blockNumber, disk))
                    .flatMap(pointerBlock -> pointerBlock.getPointerList().stream())
                    .filter(blockNumber -> blockNumber != 0)
                    .collect(Collectors.toList()));
        }

        return dataBlocks;
    }

    public void addBlock(int blockNumber, DiskInterface disk, FreeBlockList freeBlockList) {
        for (int i = 0; i < INDIRECT_1_BLOCK; i++) {
            if (blocks[i] == 0) {
                blocks[i] = blockNumber;
                usedBlockCount++;
                dirty = true;
                return;
            }
        }

        PointerBlock pointerBlock;
        if (blocks[INDIRECT_1_BLOCK] == 0) {
            // Need to add the indirect block
            blocks[INDIRECT_1_BLOCK] = freeBlockList.getAvailableBlock();
            pointerBlock = new PointerBlock(blocks[INDIRECT_1_BLOCK]);
        } else {
            pointerBlock = PointerBlock.from(blocks[INDIRECT_1_BLOCK], disk);
        }
        if (pointerBlock.addPointer(blockNumber)) {
            pointerBlock.flush(disk);
            usedBlockCount++;
            dirty = true;
            return;
        }

        if (blocks[INDIRECT_2_BLOCK] == 0) {
            // Need to add the indirect block of indirect blocks
            blocks[INDIRECT_2_BLOCK] = freeBlockList.getAvailableBlock();
            pointerBlock = new PointerBlock(blocks[INDIRECT_2_BLOCK]);
        } else {
            pointerBlock = PointerBlock.from(blocks[INDIRECT_2_BLOCK], disk);
        }
        // Try to add it to an existing indirect block
        for (int i = 0; i < BLOCK_POINTER_COUNT; i++) {
            if (pointerBlock.getPointers()[i] != 0) {
                PointerBlock pointer2Block = PointerBlock.from(pointerBlock.getPointers()[i], disk);
                if (pointer2Block.addPointer(blockNumber)) {
                    pointer2Block.flush(disk);
                    usedBlockCount++;
                    dirty = true;
                    return;
                }
            }
        }
        // Need to add a new indirect block
        for (int i = 0; i < BLOCK_POINTER_COUNT; i++) {
            if (pointerBlock.getPointers()[i] == 0) {
                PointerBlock pointer2Block = new PointerBlock(freeBlockList.getAvailableBlock());
                pointerBlock.getPointers()[i] = pointer2Block.getBlockNumber();
                pointerBlock.flush(disk);
                if (pointer2Block.addPointer(blockNumber)) {
                    pointer2Block.flush(disk);
                    usedBlockCount++;
                    dirty = true;
                    return;
                }
            }
        }

        if (blocks[INDIRECT_3_BLOCK] == 0) {
            // Need to add the indirect block of indirect blocks of indirect blocks
            blocks[INDIRECT_3_BLOCK] = freeBlockList.getAvailableBlock();
            pointerBlock = new PointerBlock(blocks[INDIRECT_3_BLOCK]);
        } else {
            pointerBlock = PointerBlock.from(blocks[INDIRECT_3_BLOCK], disk);
        }
        // Try to add it to an existing indirect block
        for (int i = 0; i < BLOCK_POINTER_COUNT; i++) {
            if (pointerBlock.getPointers()[i] != 0) {
                PointerBlock pointer2Block = PointerBlock.from(pointerBlock.getPointers()[i], disk);
                for (int j = 0; j < BLOCK_POINTER_COUNT; j++) {
                    if (pointer2Block.getPointers()[j] != 0) {
                        PointerBlock pointer3Block = PointerBlock.from(pointer2Block.getPointers()[j], disk);
                        if (pointer3Block.addPointer(blockNumber)) {
                            pointer3Block.flush(disk);
                            usedBlockCount++;
                            dirty = true;
                            return;
                        }
                    }
                }
                for (int j = 0; j < BLOCK_POINTER_COUNT; j++) {
                    if (pointer2Block.getPointers()[j] == 0) {
                        PointerBlock pointer3Block = new PointerBlock(freeBlockList.getAvailableBlock());
                        pointer2Block.getPointers()[i] = pointer3Block.getBlockNumber();
                        pointer2Block.flush(disk);
                        if (pointer3Block.addPointer(blockNumber)) {
                            pointer3Block.flush(disk);
                            usedBlockCount++;
                            dirty = true;
                            return;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < BLOCK_POINTER_COUNT; i++) {
            if (pointerBlock.getPointers()[i] == 0) {
                PointerBlock pointer2Block = new PointerBlock(freeBlockList.getAvailableBlock());
                pointerBlock.getPointers()[i] = pointer2Block.getBlockNumber();
                pointerBlock.flush(disk);

                PointerBlock pointer3Block = new PointerBlock(freeBlockList.getAvailableBlock());
                pointer2Block.getPointers()[0] = pointer3Block.getBlockNumber();
                pointer2Block.flush(disk);

                pointer3Block.getPointers()[0] = blockNumber;
                pointer3Block.flush(disk);
                usedBlockCount++;
                dirty = true;
                return;
            }
        }
        throw new InodeException(String.format("No slot for block %d", blockNumber));
    }

    protected void timeToBytes(CromixTime time, byte[] data, int offset) {
        if (time != null) {
            System.arraycopy(time.toBytes(), 0, data, offset, TIME_SIZE);
        } else {
            Arrays.fill(data, offset, offset + TIME_SIZE, (byte)0);
        }
    }

    public void incrementDirectoryEntryCount() {
        directoryEntryCount++;
    }

    public void incrementUsedBlockCount() {
        usedBlockCount++;
    }

    public int getBlockNumber(int blockIndex) {
        return blocks[blockIndex];
    }

    public String getTypeChar() {
        switch (type) {
            case FILE:
                return "F";

            case DIRECTORY:
                return "D";

            case CHARACTER_DEVICE:
                return "C";

            case BLOCK_DEVICE:
                return "B";

            case PIPE:
                return "P";

            case SHARED_TEXT:
                return "S";

            default:
                return "U";
        }
    }

    public void dump(byte[] data, int length) {
        int dumped = 0;

        System.out.printf("\nInode: %d\n", number);
        System.out.print("      00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10 11 12 13 14 15 16 17 18");
        while (dumped < length) {
            if (dumped % 32 == 0) {
                System.out.printf("\n %02x :", dumped);
            }
            System.out.printf(" %02x", data[dumped]);
            dumped++;
        }
        System.out.println("");
    }

    public String toString() {
        return "\n" +
               String.format("Inode Number           %d\n", number) +
               String.format("  Parent               %d\n", parent) +
               String.format("  links                %d\n", links) +
               String.format("  Type                 %s\n", type) +
               String.format("  Owner                %d\n", owner) +
               String.format("  Group                %d\n", group) +
               String.format("  Entries              %d\n", directoryEntryCount) +
               String.format("  File size            %d\n", fileSize) +
               String.format("  Used blocks          %d\n", usedBlockCount) +
               String.format("  User permission      %d\n", ownerPermission) +
               String.format("  Group permission     %d\n", groupPermission) +
               String.format("  Other permission     %d\n", otherPermission);
    }

}
