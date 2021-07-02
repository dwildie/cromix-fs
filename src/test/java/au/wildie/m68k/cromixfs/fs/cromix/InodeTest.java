package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.disk.floppy.cromix.CromixIMDFloppyDisk;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;


public class InodeTest {

    @Test
    public void addBlock() {
        DiskInterface disk = CromixIMDFloppyDisk.create("CLDSDD", System.out);
        assertThat(disk, notNullValue());

        SuperBlock superBlock = SuperBlock.initialise("CLDSDD");
        FreeBlockList freeBlockList = FreeBlockList.create(superBlock, disk);

        Inode inode = new Inode(1);
        assertThat(inode.getUsedBlockCount(), is(0));
        List<Integer> dataBlocks = inode.getDataBlocks(disk);
        assertThat(dataBlocks, hasSize(0));

        for (int i = 0; i < 2000; i++) {
            int blockNumber = freeBlockList.getAvailableBlock();
            inode.addBlock(blockNumber, disk, freeBlockList);
            assertThat(inode.isDirty(), is(true));
            assertThat(inode.getUsedBlockCount(), is(i + 1));
            dataBlocks = inode.getDataBlocks(disk);
            assertThat(dataBlocks, hasSize(i + 1));
            assertThat(dataBlocks, hasItem(blockNumber));
        }

        int blockNumber = freeBlockList.getAvailableBlock();
        inode.addBlock(blockNumber, disk, freeBlockList);
        assertThat(inode.isDirty(), is(true));
        dataBlocks = inode.getDataBlocks(disk);

    }
}