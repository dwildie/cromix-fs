package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.disk.DiskInterface;

import java.io.IOException;

import static au.wildie.m68k.cromixfs.fs.cromix.SuperBlock.FREE_BLOCK_LIST_SIZE;

public class FreeBlockList {
    private final SuperBlock superBlock;
    private FreeBlock freeBlock;

    public static FreeBlockList create(SuperBlock superBlock) {
        FreeBlockList freeBlockList = new FreeBlockList(superBlock);
        freeBlockList.freeBlock = FreeBlock.create(0, superBlock);

        // Update super block
        superBlock.setFreeBlockCount(freeBlockList.freeBlock.getCount());
        for (int i = 0; i < FREE_BLOCK_LIST_SIZE; i++) {
            superBlock.getFreeBlockList()[i] = freeBlockList.freeBlock.getList()[i];
        }
        superBlock.setDirty(true);
        return freeBlockList;
    }

    public static FreeBlockList readFreeBlockList(SuperBlock superBlock, DiskInterface disk) {
        FreeBlockList freeBlockList = new FreeBlockList(superBlock);
        freeBlockList.freeBlock = FreeBlock.from(superBlock, disk);
        return freeBlockList;
    }

    public FreeBlockList(SuperBlock superBlock) {
        this.superBlock = superBlock;
    }

    public void flush(DiskInterface disk) throws IOException {
        freeBlock.flush(superBlock, disk);
    }

    public void visit(FreeBlockNumberVisitor visitor) {
        freeBlock.visit(visitor);
    }

    public int getFreeBlockCount() {
        return freeBlock.getFreeBlockCount()  - 1;
    }

    public int getAvailableBlock() {
        int blockNumber = freeBlock.takeNextFreeBlockNumber();
        freeBlock.flush(superBlock);
        return blockNumber;
    }

}
