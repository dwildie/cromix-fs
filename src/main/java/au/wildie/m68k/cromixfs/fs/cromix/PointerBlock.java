package au.wildie.m68k.cromixfs.fs.cromix;

import static au.wildie.m68k.cromixfs.utils.BinUtils.readDWord;
import static au.wildie.m68k.cromixfs.utils.BinUtils.writeDWord;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import lombok.Getter;

@Getter
public class PointerBlock {
    public static final int BLOCK_POINTER_COUNT = 0x80;
    private final int blockNumber;
    private final Integer[] pointers = new Integer[BLOCK_POINTER_COUNT];

    public static PointerBlock from(int blockNumber, DiskInterface disk) {
        try {
            PointerBlock block = new PointerBlock(blockNumber);
            byte[] data = disk.getBlock(blockNumber);
            for (int i = 0; i < BLOCK_POINTER_COUNT; i++) {
                block.pointers[i] = readDWord(data, i * 4);
            }
            return block;
        } catch (IOException e) {
            throw new BlockUnavailableException(blockNumber, e);
        }
    }

    public PointerBlock(int blockNumber) {
        this.blockNumber = blockNumber;
        Arrays.fill(pointers, 0);
    }

    public List<Integer> getPointerList() {
        return Arrays.asList(pointers);
    }

    public void setPointer(int index, int blockNumber) {
        pointers[index] = blockNumber;
    }

    public boolean addPointer(int blockNumber) {
        for (int i = 0; i < pointers.length; i++) {
            if (pointers[i] == 0) {
                pointers[i] = blockNumber;
                return true;
            }
        }
        return false;
    }

    public int getPointer(int pointerIndex) {
        return pointers[pointerIndex];
    }

    public void flush(DiskInterface disk) {
        try {
            byte[] data = disk.getBlock(blockNumber);
            for (int i = 0; i < BLOCK_POINTER_COUNT; i++) {
                writeDWord(pointers[i], data, i * 4);
            }
        } catch (IOException e) {
            throw new BlockUnavailableException(blockNumber, e);
        }
    }
}
