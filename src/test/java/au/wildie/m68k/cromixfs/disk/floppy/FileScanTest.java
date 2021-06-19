package au.wildie.m68k.cromixfs.disk.floppy;

import org.junit.Test;

import java.io.IOException;

public class FileScanTest {

    @Test
    public void scan() throws IOException {
        FileScan scan = new FileScan("/home/dwildie/cromemcos/m/cromemco/code/disks");
        scan.scan();
    }
}