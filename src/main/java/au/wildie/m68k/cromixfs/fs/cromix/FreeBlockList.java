package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.disk.DiskInterface;

import java.io.IOException;

import static au.wildie.m68k.cromixfs.fs.cromix.SuperBlock.FREE_BLOCK_LIST_SIZE;

public class FreeBlockList {
    private final SuperBlock superBlock;
    private final DiskInterface disk;
    private FreeBlock freeBlock;

    public static FreeBlockList create(SuperBlock superBlock, DiskInterface disk) {
        FreeBlockList freeBlockList = new FreeBlockList(superBlock, disk);
        freeBlockList.freeBlock = FreeBlock.create(0, superBlock);

        // Update super block
        superBlock.setFreeBlockCount(freeBlockList.freeBlock.getCount());
        System.arraycopy(freeBlockList.freeBlock.getList(), 0, superBlock.getFreeBlockList(), 0, FREE_BLOCK_LIST_SIZE);
        superBlock.setDirty(true);
        return freeBlockList;
    }

    public static FreeBlockList readFreeBlockList(SuperBlock superBlock, DiskInterface disk) {
        FreeBlockList freeBlockList = new FreeBlockList(superBlock, disk);
        freeBlockList.freeBlock = FreeBlock.from(superBlock, disk);
        return freeBlockList;
    }

    public FreeBlockList(SuperBlock superBlock, DiskInterface disk) {
        this.superBlock = superBlock;
        this.disk = disk;
    }

    public void flush() {
        try {
            freeBlock.flush(superBlock, disk);
        } catch (IOException e) {
             throw new FreeBlockListException("Failed to flush free block list", e);
        }
    }

    public void visit(FreeBlockNumberVisitor visitor) {
        freeBlock.visit(visitor);
    }

    public int getFreeBlockCount() {
        return freeBlock.getFreeBlockCount()  - 1;
    }

    public int getAvailableBlock() {
        int blockNumber = freeBlock.takeNextFreeBlock();
        freeBlock.flush(superBlock);
        return blockNumber;
    }

    public void returnBlock(int block) {
        freeBlock.returnBlock(block);
        freeBlock.flush(superBlock);
    }
}
