package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.disk.DiskInterface;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static au.wildie.m68k.cromixfs.fs.cromix.DirectoryEntry.DIRECTORY_ENTRY_LENGTH;
import static au.wildie.m68k.cromixfs.fs.cromix.Inode.ALLOCATED;
import static au.wildie.m68k.cromixfs.fs.cromix.Inode.INDIRECT_1_BLOCK;
import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.*;

public class Check {
    public static final int FLAG_DIRECTORY         = 0x01;            /* It is directory               */
    public static final int FLAG_WRONG_PARENT      = 0x02;            /* Wrong parent                  */
    public static final int FLAG_CHILD             = 0x04;            /* Child must be processed first */
    public static final int FLAG_BAD_COUNT         = 0x08;            /* Bad directory count           */
    public static final int FLAG_MULTIPLE          = 0x10;            /* Used in multiple directories  */
    public static final int FLAG_BAD_INODE_NUMBER  = 0x20;            /* Bad inode number in inode     */
    public static final int FLAG_GREATER_255_LINKS = 0x40;            /* More than 255 links           */
    public static final int FLAG_ALLOCATED         = 0x80;            /* Inode allocated               */

    private final List<Itable> itables = new ArrayList<>();
    private final SuperBlock superBlock;
    private final InodeManager inodeManager;
    private final DiskInterface disk;

    public Check(SuperBlock superBlock, InodeManager inodeManager, DiskInterface disk) {
        this.superBlock = superBlock;
        this.inodeManager = inodeManager;
        this.disk = disk;
    }

    public void passOne() {
        for (int i = 0; i < inodeManager.getAllInodes().size(); i++) {
            itables.add(new Itable());
        }

        itables.get(0).cntlinks = 1;
        itables.get(0).parent = 1;

        for (int i = 0; i < inodeManager.getAllInodes().size(); i++) {
            Inode inode = inodeManager.getAllInodes().get(i);
            Itable itable = itables.get(i);

            if (itable.parent != 0) {
                if (itable.parent != inode.getParent()) {
                    itable.flags |= FLAG_WRONG_PARENT;
                }
            } else {
                itable.parent = inode.getParent();
                itable.flags |= FLAG_CHILD;
            }

            itable.nlinks = inode.getLinks();

            /* Verify the inode number      */
            /* recorded in the inode        */
            if (inode.getNumber() != (i + 1)) {
                itable.flags |= FLAG_BAD_INODE_NUMBER;
            }

            /* If inode is not allocated    */
            /* that is it, else say it is   */
            /* allocated.                   */
            if (!ALLOCATED.contains(inode.getType())) {
                continue;
            }
            itable.flags |= FLAG_ALLOCATED;

            /* If it is not directory that  */
            /* is it.                       */
            if (inode.getType() != DIRECTORY) {
                continue;
            }

            /* Say it is directoy inode     */
            itable.flags |= FLAG_DIRECTORY;

            /* Read all directory entries   */
            /* and do further check for     */
            /* each allocated entry.        */
            for (int j = 0; j < INDIRECT_1_BLOCK; j++) {
                if (inode.getBlocks()[j] == 0) {
                    continue;
                }
                DirectoryBlock directoryBlock = DirectoryBlock.from(disk, inode.getBlocks()[j]);
                for (DirectoryEntry entry : directoryBlock.getEntries()) {
                    int dirExtent = j * superBlock.getBlockSize() + entry.getIndex() * DIRECTORY_ENTRY_LENGTH;
                    if (dirExtent < inode.getFileSize()) {
                        if (entry.getStatus() == DirectoryEntryStatus.ALLOCATED) {
                            if (entry.getInodeNumber() == 0 || entry.getInodeNumber() > inodeManager.getLastInodeNumber()) {
                                throw new CheckException(String.format("Directory %s, inode %d is out of bounds\n", entry.getName(), entry.getInodeNumber()));
                            }
                            itable.entries++;
                            Itable itabc = itables.get(entry.getInodeNumber() - 1);
                            itabc.cntlinks++;
                            if ((itabc.flags & FLAG_CHILD) != 0) {
                                itabc.flags &= ~FLAG_CHILD;
                                if (itabc.parent != inode.getNumber()) {
                                    itabc.flags |= FLAG_WRONG_PARENT;
                                    itabc.parent = inode.getNumber();
                                }
                            } else if (itabc.parent == 0) {
                                itabc.parent = inode.getNumber();
                            } else if (itabc.parent != inode.getNumber()) {
                                itabc.flags |= FLAG_MULTIPLE;
                            }
                        }
                    }
                }
            }
            if (inode.getDirectoryEntryCount() != itable.entries) {
                itable.flags |= FLAG_BAD_COUNT;
            }
        }
    }

    public int passTwo(PrintStream out) {
        int errors = 0;

        for (int i = 0; i < inodeManager.getAllInodes().size(); i++) {
            Inode inode = inodeManager.getAllInodes().get(i);
            Itable itable = itables.get(i);

            if ((itable.flags & FLAG_BAD_INODE_NUMBER) != 0) {
                if ((itable.flags & FLAG_ALLOCATED) != 0 || itable.cntlinks != 0 || itable.nlinks != 0) {
                    out.printf("Inode %6d, bad inode number in inode\n", inode.getNumber());
                    errors++;
                }
            }
            if ((itable.flags & FLAG_ALLOCATED) != 0) {
                if (itable.cntlinks == 0) {
                    out.printf("Inode %6d, allocated inode with 0 links\n", inode.getNumber());
                    errors++;
                }

                if ((itable.flags & FLAG_DIRECTORY) != 0) {
                    if ((itable.flags & FLAG_BAD_COUNT) != 0) {
                        out.printf("Inode %6d, bad directory entry count, expected %d, actual %d\n", inode.getNumber(), itable.entries, inode.getDirectoryEntryCount());
                        errors++;
                    }
                    if ((itable.flags & FLAG_MULTIPLE) != 0) {
                        out.printf("Inode %6d, directory with more than one parent\n", inode.getNumber());
                        errors++;
                    }
                    if ((itable.flags & FLAG_WRONG_PARENT) != 0) {
                        out.printf("Inode %6d, directory with wrong parent\n", inode.getNumber());
                        errors++;
                    }
                }

                if (itable.nlinks != itable.cntlinks) {
                    out.printf("Inode %6d, bad link count %d, should be %d\n", inode.getNumber(), itable.nlinks, itable.cntlinks);
                    errors++;
                }
                if ((itable.flags & FLAG_GREATER_255_LINKS) != 0) {
                    out.printf("Inode %6d, more than 255 links\n", inode.getNumber());
                    errors++;
                }
            } else if (itable.cntlinks != 0) {
                out.printf("Inode %6d, unallocated inode with %d links\n", inode.getNumber(), itable.cntlinks);
                errors++;
            }
        }
        return errors;
    }

    public int fileCheck(PrintStream out) {
        int errors = 0;
        for (Inode inode : inodeManager.getAllInodes()) {
            if (inode.getType() == FILE || inode.getType() == SHARED_TEXT) {
                int expectedBlocks = inode.getFileSize() / superBlock.getBlockSize()
                                   + ((inode.getFileSize() % superBlock.getBlockSize()) != 0 ? 1 : 0);

                List<Integer> dataBlocks = inode.getDataBlocks(disk);
                if (dataBlocks.size() > expectedBlocks) {
                    out.printf("Inode %6d, too many data blacks, counted %d, should be %d\n", inode.getNumber(), dataBlocks.size(), expectedBlocks);
                    errors++;
                }
                if (dataBlocks.size() < expectedBlocks) {
                    out.printf("Inode %6d, missing data blacks, counted %d, should be %d\n", inode.getNumber(), dataBlocks.size(), expectedBlocks);
                    errors++;
                }

                int countedUsedBlocks = inode.countUsedBlocks(disk);
                if (countedUsedBlocks != inode.getUsedBlockCount()) {
                    out.printf("Inode %6d, used block count mismatch, counted %d, should be %d\n", inode.getNumber(), countedUsedBlocks, inode.getUsedBlockCount());
                    errors++;
                }
            }
        }
        return errors;
    }

    public static class Itable {
        int cntlinks;
        int parent;
        int flags;
        int nlinks;
        int entries;
    }
}
