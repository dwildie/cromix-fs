package au.wildie.m68k.cromixfs.fs.cromix;

import au.wildie.m68k.cromixfs.disk.DiskInterface;

import java.util.ArrayList;
import java.util.List;

import static au.wildie.m68k.cromixfs.fs.cromix.Inode.ALLOCATED;
import static au.wildie.m68k.cromixfs.fs.cromix.Inode.INDIRECT_1_BLOCK;
import static au.wildie.m68k.cromixfs.fs.cromix.InodeType.DIRECTORY;

public class Check {
    public static final int if_direct =       0001;            /* It is directory              */
    public static final int if_wrongp =       0002;            /* Wrong parent                 */
    public static final int if_child =        0004;            /* Child must be processed first*/
    public static final int if_badcount =     0010;            /* Bad directory count          */
    public static final int if_many =         0020;            /* Used in many directories     */
    public static final int if_badinode =     0040;            /* Bad inode number in inode    */
    public static final int if_gt255 =        0100;            /* More than 255 links          */
    public static final int if_alloc =        0200;            /* Inode allocated              */

    private List<Itab> itabs = new ArrayList<>();
    private InodeManager inodeManager;
    private DiskInterface disk;

    public Check(InodeManager inodeManager, DiskInterface disk) {
        this.inodeManager = inodeManager;
        this.disk = disk;
    }

    public void passOne() {
        for (int i = 0; i < inodeManager.getAllInodes().size(); i++) {
            itabs.add(new Itab());
        }

        itabs.get(0).cntlinks = 1;
        itabs.get(0).parent = 1;

        for (int i = 0; i < inodeManager.getAllInodes().size(); i++) {
            Inode inode = inodeManager.getAllInodes().get(i);
            Itab itab = itabs.get(i);

            if (itab.parent != 0) {
                if (itab.parent != inode.getParent()) {
                    itab.flags |= if_wrongp;
                }
            } else {
                itab.parent = inode.getParent();
                itab.flags |= if_child;
            }

            itab.nlinks = inode.getLinks();

            /* Verify the inode number      */
            /* recorded in the inode        */
            if (inode.getNumber() != (i + 1)) {
                itab.flags |= if_badinode;
            }

            /* If inode is not allocated    */
            /* that is it, else say it is   */
            /* allocated.                   */
            if (!ALLOCATED.contains(inode.getType())) {
                continue;
            }
            itab.flags |= if_alloc;

            /* If it is not directory that  */
            /* is it.                       */
            if (inode.getType() != DIRECTORY) {
                continue;
            }

            /* Say it is directoy inode     */
            itab.flags |= if_direct;

            /* Read all directory entries   */
            /* and do further check for     */
            /* each allocated entry.        */
            for (int j = 0; j < INDIRECT_1_BLOCK; j++) {
                if (inode.getBlocks()[j] == 0) {
                    continue;
                }
                DirectoryBlock directoryBlock = DirectoryBlock.from(disk, inode.getBlocks()[j]);
                for (DirectoryEntry entry : directoryBlock.getEntries()) {
                    if (entry.getStatus() == DirectoryEntryStatus.ALLOCATED) {
                        if (entry.getInodeNumber() == 0 || entry.getInodeNumber() > inodeManager.getLastInodeNumber()) {
                            throw new CheckException(String.format("Directory %s, inode %d is out of bounds\n", entry.getName(), entry.getInodeNumber()));
                        }
                        itab.entries++;
                        Itab itabc = itabs.get(entry.getInodeNumber() - 1);
                        itabc.cntlinks++;
                        if ((itabc.flags & if_child) != 0) {
                            itabc.flags &= ~if_child;
                            if (itabc.parent != inode.getNumber()) {
                                itabc.flags |= if_wrongp;
                                itabc.parent = inode.getNumber();
                            }
                        } else if (itabc.parent == 0) {
                            itabc.parent = inode.getNumber();
                        } else if (itabc.parent != inode.getNumber()) {
                            itabc.flags |= if_many;
                        }
                    }
                }
            }
            if (inode.getDirectoryEntryCount() != itab.entries) {
                itab.flags |= if_badcount;
            }
        }
    }

    public void passTwo() {
        for (int i = 0; i < inodeManager.getAllInodes().size(); i++) {
            Inode inode = inodeManager.getAllInodes().get(i);
            Itab itab = itabs.get(i);

            if ((itab.flags & if_badinode) != 0) {
                if ((itab.flags & if_alloc) != 0 || itab.cntlinks != 0 || itab.nlinks != 0) {
                    System.out.printf("Inode %d, bad inode number in inode\n", inode.getNumber());
                }
            }
            if ((itab.flags & if_alloc) != 0) {
                if (itab.cntlinks == 0) {
                    System.out.printf("Inode %d, allocated inode with 0 links\n", inode.getNumber());
                }

                if ((itab.flags & if_direct) != 0) {
                    if ((itab.flags & if_badcount) != 0) {
                        System.out.printf("Inode %d, bad directory entry count\n", inode.getNumber());
                    }
                    if ((itab.flags & if_many) != 0) {
                        System.out.printf("Inode %6d, directory with more than one parent\n", inode.getNumber());
                    }
                    if ((itab.flags & if_wrongp) != 0) {
                        System.out.printf("Inode %d, directory with wrong parent\n", inode.getNumber());
                    }
                }

                if (itab.nlinks != itab.cntlinks) {
                    System.out.printf("Inode %6d, bad link count %d, should be %d\n", inode.getNumber(), itab.nlinks, itab.cntlinks);
                }
                if ((itab.flags & if_gt255) != 0) {
                    System.out.printf("Inode %6d, more than 255 links\n", inode.getNumber());
                }
            } else if (itab.cntlinks != 0) {
                System.out.printf("Inode %d, unallocated inode with %d links\n", inode.getNumber(), itab.cntlinks);
            }

        }
    }
    public static class Itab {
        int cntlinks;
        int parent;
        int flags;
        int nlinks;
        int entries;
    }
}
