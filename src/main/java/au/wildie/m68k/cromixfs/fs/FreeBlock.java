package au.wildie.m68k.cromixfs.fs;

import static au.wildie.m68k.cromixfs.fs.SuperBlock.FREE_BLOCK_LIST_SIZE;
import static au.wildie.m68k.cromixfs.utils.BinUtils.readDWord;
import static au.wildie.m68k.cromixfs.utils.BinUtils.readWord;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FreeBlock {
    private int blockNumber;
    private int freeBlockCount;
    private int[] freeBlockList = new int[FREE_BLOCK_LIST_SIZE];
    private FreeBlock next;
    private boolean dirty = false;

    public static FreeBlock from(SuperBlock superBlock) {
        FreeBlock freeBlock = new FreeBlock();
        freeBlock.blockNumber = 0;
        freeBlock.freeBlockCount = superBlock.getFreeBlockCount();
        freeBlock.freeBlockList = Arrays.copyOf(superBlock.getFreeBlockList(), superBlock.getFreeBlockList().length);
        return freeBlock;
    }

    public static FreeBlock from(int blockNumber, byte[] data) {
        FreeBlock freeBlock = new FreeBlock();
        freeBlock.blockNumber = blockNumber;
        freeBlock.freeBlockCount = readWord(data, 0);
        freeBlock.freeBlockList = new int[FREE_BLOCK_LIST_SIZE];
        for (int i = 0; i < FREE_BLOCK_LIST_SIZE; i++) {
            freeBlock.freeBlockList[i] = readDWord(data, 2 + i * 4);
        }
        return freeBlock;
    }

    public int getTotalFreeBlockCount() {
        return freeBlockCount + (next != null ? next.getFreeBlockCount() : 0) - 1;
    }

    private int getFreeBlockCount() {
        return freeBlockCount + (next != null ? next.getFreeBlockCount() : 0);
    }

    public void visit(FreeBlockVisitor visitor) {
        for (int i = 0; i < freeBlockCount; i++) {
            if (freeBlockList[i] != 0) {
                visitor.visit(freeBlockList[i]);
            }
        }
        if (next != null) {
            next.visit(visitor);
        }
    }
}
