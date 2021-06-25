package au.wildie.m68k.cromixfs.fs;

public interface FreeBlockVisitor {
    void visit(int blockNumber);
}
