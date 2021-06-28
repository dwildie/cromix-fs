package au.wildie.m68k.cromixfs.fs.cromix;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class FreeBlockListTest {
    @Test
    public void getAvailableBlock() {

        SuperBlock superBlock = SuperBlock.initialise("CLDSDD");

        FreeBlockList freeBlockList = FreeBlockList.create(superBlock);
        int freeBlockCount = freeBlockList.getFreeBlockCount();

        for (int i = 0; i < freeBlockCount; i++) {
            int blockNumber = freeBlockList.getAvailableBlock();
            assertThat(blockNumber, is(superBlock.getFirstDataBlock() + i));
            assertThat(freeBlockList.getFreeBlockCount(), is(freeBlockCount - (i + 1)));
        }

        FreeBlockListException e = assertThrows(FreeBlockListException.class, freeBlockList::getAvailableBlock);
        assertThat(e.getMessage(), containsString("No more blocks"));
    }
}