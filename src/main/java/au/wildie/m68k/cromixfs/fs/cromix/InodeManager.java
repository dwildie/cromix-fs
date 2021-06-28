package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.disk.DiskInterface;
import au.wildie.m68k.cromixfs.fs.CromixTime;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static au.wildie.m68k.cromixfs.fs.cromix.Inode.*;
import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.DIRECTORY;
import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.UNUSED;
import static au.wildie.m68k.cromixfs.fs.cromix.SuperBlock.FREE_INODE_LIST_SIZE;
import static java.lang.Integer.min;

@Getter
public class InodeManager {
    private final SuperBlock superBlock;
    private final DiskInterface disk;
    private final FreeBlockList freeBlockList;
    private List<Inode> allInodes;

    public static InodeManager read(SuperBlock superBlock, DiskInterface disk, FreeBlockList freeBlockList) {
        InodeManager manager = new InodeManager(superBlock, disk, freeBlockList);
        manager.readAllInodes();
        return manager;
    }

    public static InodeManager create(SuperBlock superBlock, DiskInterface disk, FreeBlockList freeBlockList) {
        InodeManager manager = new InodeManager(superBlock, disk, freeBlockList);
        manager.allInodes = new ArrayList<>();
        for (int i = 0; i < superBlock.getInodeCount(); i++) {
            Inode inode = new Inode(i + 1);
            if (inode.getNumber() == 1) {
                // Create as the root inode
                inode.setType(DIRECTORY);
                inode.setParent(1);
                inode.setLinks(1);
                inode.setFileSize(0);
                inode.setOwnerPermission(ACCESS_READ | ACCESS_EXEC | ACCESS_WRITE | ACCESS_APPEND);
                inode.setGroupPermission(ACCESS_READ | ACCESS_EXEC);
                inode.setOtherPermission(ACCESS_READ | ACCESS_EXEC);
                inode.setCreated(CromixTime.now());
                inode.setModified(CromixTime.now());
                inode.setAccessed(CromixTime.now());
            }
            manager.allInodes.add(inode);
        }

        return manager;
    }

    public InodeManager(SuperBlock superBlock, DiskInterface disk, FreeBlockList freeBlockList) {
        this.superBlock = superBlock;
        this.disk = disk;
        this.freeBlockList = freeBlockList;
    }

    public int getLastInodeNumber() {
        return superBlock.getInodeCount() + 1;
    }

    public void flush(Inode inode) {
//        System.out.printf("Flush inode %d\n", inode.getNumber());
        int blockNumber = superBlock.getFirstInodeBlock() + ((inode.getNumber() - 1) / INODES_PER_BLOCK);
        try {
            byte[] data = disk.getBlock(blockNumber);
            inode.toBytes(data, (superBlock.getBlockSize() / INODES_PER_BLOCK) * ((inode.getNumber() - 1) % INODES_PER_BLOCK));
        } catch (IOException e) {
            throw new BlockUnavailableException(blockNumber, e);
        }
    }

    public void flushAll() {
        for (int i = 0; i < superBlock.getInodeCount() / INODES_PER_BLOCK; i++) {
            int blockNumber = superBlock.getFirstInodeBlock() + i;
            try {
                byte[] block = disk.getBlock(blockNumber);
                for (int j = 0; j < INODES_PER_BLOCK; j++) {
                    Inode inode = allInodes.get(i * INODES_PER_BLOCK + j);
                    inode.toBytes(block, (superBlock.getBlockSize() / INODES_PER_BLOCK) * j);
                }
            } catch (IOException e) {
                throw new BlockUnavailableException(blockNumber, e);
            }
        }
    }

    protected void readAllInodes() {
        allInodes = new ArrayList<>();
        int blocks = superBlock.getInodeCount() / INODES_PER_BLOCK;
        for (int i = 0; i < blocks; i++) {
            int blockNumber = superBlock.getFirstInodeBlock() + i;
            try {
                byte[] block = disk.getBlock(blockNumber);
                for (int j = 0; j < INODES_PER_BLOCK; j++) {
                    allInodes.add(Inode.from(Arrays.copyOfRange(block, j * INODE_LENGTH, j * INODE_LENGTH + INODE_LENGTH)));
                }
            } catch (IOException e) {
                throw new BlockUnavailableException(blockNumber, e);
            }
        }
    }

    public Inode getAvailableInode() {
        if (superBlock.getFreeInodeCount() == 0) {
            List<Inode> free = getAllInodes().stream().filter(inode -> inode.getType() == UNUSED).collect(Collectors.toList());
            int freeInodes = min(FREE_INODE_LIST_SIZE, free.size());
            for (int i = 0; i < freeInodes; i++) {
                superBlock.getFreeInodeList()[FREE_INODE_LIST_SIZE - i - 1] = free.get(i).getNumber();
                superBlock.incrementFreeInodeCount();
            }
        }

        int inodeNumber = superBlock.getFreeInodeList()[superBlock.getFreeInodeCount() - 1];
        superBlock.decrementFreeInodeCount();
        superBlock.flush(disk);
        return getInode(inodeNumber);
    }

    protected Inode getInode(int inodeNumber) {
        if (inodeNumber == 0) {
            throw new InodeException(inodeNumber);
        }
        return getAllInodes().stream()
                .filter(iNode -> iNode.getNumber() == inodeNumber)
                .findFirst()
                .orElseThrow(() -> new InodeException(inodeNumber));
    }

    public void addBlock(Inode entryInode, int blockNumber) {
        entryInode.addBlock(blockNumber, disk, freeBlockList);
    }
}
