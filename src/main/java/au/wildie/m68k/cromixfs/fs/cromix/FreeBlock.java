package au.wildie.m68k.cromixfs.fs.cromix;

import static au.wildie.m68k.cromixfs.fs.cromix.SuperBlock.FREE_BLOCK_LIST_SIZE;
import static au.wildie.m68k.cromixfs.utils.BinUtils.*;
import static java.lang.Integer.min;

import java.io.IOException;
import java.util.Arrays;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FreeBlock {
    private int blockNumber = 0;
    private int count = 0;
    private int[] list = new int[FREE_BLOCK_LIST_SIZE];
    private FreeBlock next = null;
    private boolean dirty = false;

    public static FreeBlock create(SuperBlock superBlock) {
        FreeBlock freeBlock = create(0, superBlock);
        // Update super block
        superBlock.setFreeBlockCount(freeBlock.getCount());
        for (int i = 0; i < FREE_BLOCK_LIST_SIZE; i++) {
            superBlock.getFreeBlockList()[i] = freeBlock.list[i];
        }
        superBlock.setDirty(true);
        return freeBlock;
    }

    protected static FreeBlock create(int blockIndex, SuperBlock superBlock) {
        FreeBlock freeBlock = new FreeBlock();
        if (blockIndex != 0) {
            freeBlock.blockNumber = superBlock.getFirstDataBlock() + blockIndex++;
        }

        int remainingBlocks = superBlock.getDataBlockCount() - blockIndex;
        if (remainingBlocks < FREE_BLOCK_LIST_SIZE) {
            // Last block, partially filled
            for (int i = 0; i < remainingBlocks; i++) {
                if (blockIndex < superBlock.getDataBlockCount()) {
                    freeBlock.list[remainingBlocks - i] = superBlock.getFirstDataBlock() + blockIndex++;
                    freeBlock.count++;
                }
            }
            freeBlock.count++;  // Count the empty next block
        } else {
            for (int i = 1; i < FREE_BLOCK_LIST_SIZE; i++) {
                if (blockIndex < superBlock.getDataBlockCount()) {
                    freeBlock.list[FREE_BLOCK_LIST_SIZE - i] = superBlock.getFirstDataBlock() + blockIndex++;
                    freeBlock.count++;
                }
            }
        }

        freeBlock.dirty = true;
        if (blockIndex < superBlock.getDataBlockCount()) {
            // Add the next block
            freeBlock.next = create(blockIndex, superBlock);
            freeBlock.list[0] = freeBlock.next.blockNumber;
            freeBlock.count++;
        }
        return freeBlock;
    }


    public static FreeBlock from(SuperBlock superBlock) {
        FreeBlock freeBlock = new FreeBlock();
        freeBlock.blockNumber = 0;
        freeBlock.count = superBlock.getFreeBlockCount();
        freeBlock.list = Arrays.copyOf(superBlock.getFreeBlockList(), superBlock.getFreeBlockList().length);
        return freeBlock;
    }

    public static FreeBlock from(int blockNumber, byte[] data) {
        FreeBlock freeBlock = new FreeBlock();
        freeBlock.blockNumber = blockNumber;
        freeBlock.count = readWord(data, 0);
        freeBlock.list = new int[FREE_BLOCK_LIST_SIZE];
        for (int i = 0; i < FREE_BLOCK_LIST_SIZE; i++) {
            freeBlock.list[i] = readDWord(data, WORD_SIZE + i * DWORD_SIZE);
        }
        return freeBlock;
    }

    public void flush(DiskInterface disk) throws IOException {
        if (blockNumber != 0) {
            byte[] data = disk.getBlock(blockNumber);
            writeWord(count, data, 0);
            for (int i = 0; i < FREE_BLOCK_LIST_SIZE; i++) {
                writeDWord(list[i], data, WORD_SIZE + i * DWORD_SIZE);
            }
        }
        if (next != null) {
            next.flush(disk);
        }
    }

    public int getTotalFreeBlockCount() {
        return count + (next != null ? next.getFreeBlockCount() : 0) - 1;
    }

    private int getFreeBlockCount() {
        return count + (next != null ? next.getFreeBlockCount() : 0);
    }

    public void visit(FreeBlockNumberVisitor visitor) {
        for (int i = 0; i < count; i++) {
            if (list[i] != 0) {
                visitor.visit(list[i]);
            }
        }
        if (next != null) {
            next.visit(visitor);
        }
    }

}
