package au.wildie.m68k.cromixfs.fs.cromix;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@ToString
@RequiredArgsConstructor
public class BlockUsage {
    private List<BlockUsageItem> blocks;
    private int firstDataBlock;

    public BlockUsage(SuperBlock superBlock) {
        blocks = new ArrayList<>(superBlock.getDataBlockCount());
        firstDataBlock = superBlock.getFirstDataBlock();

        for (int i = 0; i < superBlock.getDataBlockCount(); i++) {
            blocks.add(new BlockUsageItem(firstDataBlock + i));
        }
    }

    public void setDirectory(int blockNumber) {
        blocks.get(blockNumber - firstDataBlock).setDirectory();
    }

    public void setFile(int blockNumber) {
        blocks.get(blockNumber - firstDataBlock).setFile();
    }

    public void setOnFreeList(int blockNumber) {
        blocks.get(blockNumber - firstDataBlock).setOnFreeList();
    }

    public List<BlockUsageItem> getOrphanedBlocks() {
        return blocks.stream().filter(BlockUsageItem::isOrphaned).collect(Collectors.toList());
    }

    public int getFileBlockCount() {
        return (int)blocks.stream().filter(BlockUsageItem::isFile).count();
    }

    public int getDirectoryBlockCount() {
        return (int)blocks.stream().filter(BlockUsageItem::isDirectory).count();
    }

    public int getOnFreeListBlockCount() {
        return (int)blocks.stream().filter(BlockUsageItem::isOnFreeList).count();
    }
    public int getOrphanedBlockCount() {
        return (int)blocks.stream().filter(BlockUsageItem::isOrphaned).count();
    }

    public int getDuplicateBlockCount() {
        return (int)blocks.stream().filter(BlockUsageItem::isDuplicate).count();
    }
}
