package au.wildie.m68k.cromixfs.disk.floppy.vfd;

import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class VFDConverter {
    public static void imdToVfd(File imd, File vfd) throws IOException {
        IMDImage imdImage = IMDImage.fromFile(0, imd, System.out);
        VFDImage vfdImage = VFDImage.from(imdImage);
        if (vfd.exists()) {
            vfd.delete();
        }
        FileUtils.writeByteArrayToFile(vfd, vfdImage.toBytes());
    }

    public static void vfdToImd(File vfd, File imd) {
        // TODO
    }
}
