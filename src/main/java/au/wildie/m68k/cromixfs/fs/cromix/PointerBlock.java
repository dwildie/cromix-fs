package au.wildie.m68k.cromixfs.fs.cromix;

import static au.wildie.m68k.cromixfs.utils.BinUtils.readDWord;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;

@Getter
public class PointerBlock {
    public static final int BLOCK_POINTER_COUNT = 0x80;

    private final Integer[] pointers = new Integer[BLOCK_POINTER_COUNT];

    public static PointerBlock from(byte[] data) {
        PointerBlock block = new PointerBlock();
        for (int i = 0; i < BLOCK_POINTER_COUNT ; i++) {
            block.pointers[i] = readDWord(data, i * 4);
        }
        return block;
    }

    public List<Integer> getPointerList() {
        return Arrays.asList(pointers);
    }

    public int getPointer(int pointerIndex) {
        return pointers[pointerIndex];
    }
}
