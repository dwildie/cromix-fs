package au.wildie.m68k.cromixfs.fs.cromix;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static au.wildie.m68k.cromixfs.fs.cromix.SuperBlock.FREE_BLOCK_LIST_SIZE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThrows;

public class FreeBlockListTest {
    @Test
    public void getAvailableBlock() {

        SuperBlock superBlock = SuperBlock.initialiseLarge("CLDSDD");

        FreeBlockList freeBlockList = FreeBlockList.create(superBlock, null);
        int freeBlockCount = freeBlockList.getFreeBlockCount();

        for (int i = 0; i < freeBlockCount; i++) {
            int blockNumber = freeBlockList.getAvailableBlock();
            assertThat(blockNumber, is(superBlock.getFirstDataBlock() + i));
            assertThat(freeBlockList.getFreeBlockCount(), is(freeBlockCount - (i + 1)));
        }

        FreeBlockListException e = assertThrows(FreeBlockListException.class, freeBlockList::getAvailableBlock);
        assertThat(e.getMessage(), containsString("No more blocks"));
    }

    @Test
    public void returnBlock() {
        SuperBlock superBlock = SuperBlock.initialiseLarge("CLDSDD");

        FreeBlockList freeBlockList = FreeBlockList.create(superBlock, null);
        int initialFreeBlockCount = freeBlockList.getFreeBlockCount();

        List<Integer> freedBlocks = new ArrayList<>();
        for (int i = 0; i < FREE_BLOCK_LIST_SIZE; i++) {
            freedBlocks.add(freeBlockList.getAvailableBlock());
        }

        assertThat(freedBlocks, hasSize(FREE_BLOCK_LIST_SIZE));

        for (int i = 0; i< FREE_BLOCK_LIST_SIZE; i++) {
            freeBlockList.returnBlock(freedBlocks.get(i));
        }

        int finalFreeBlockCount = freeBlockList.getFreeBlockCount();
        assertThat(finalFreeBlockCount, is(initialFreeBlockCount));
    }
}