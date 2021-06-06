package au.wildie.m68k.cromixfs.disk.vfd;

import au.wildie.m68k.cromixfs.disk.imd.IMDImage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class VFDConverter {
    public static void imdToVfd(File imd, File vfd, boolean strict) throws IOException, VFDErrorsException {
        IMDImage imdImage = IMDImage.fromFile(0, imd, System.out);
        if (strict) {
            imdImage.verify(System.out);
        }

        Integer errors = imdImage.getSectorErrorCount();
        if (!strict) {
            System.out.printf("%d errors, ignoring\n", errors);
        } else {
            if (errors == 0) {
                System.out.println("Strict mode, zero errors");
            } else {
                throw new VFDErrorsException(String.format("IMD file %s has %d sector errors, VFD file was not created.", imd.getPath(), errors), errors);
            }
        }
        VFDImage vfdImage = VFDImage.from(imdImage);
        if (vfd.exists()) {
            vfd.delete();
        }
        FileUtils.writeByteArrayToFile(vfd, vfdImage.toBytes());
    }

    public static byte[] imdToVfd(InputStream imd, boolean strict) throws IOException, VFDErrorsException {
        IMDImage imdImage = IMDImage.fromStream(imd, System.out);
        if (strict) {
            imdImage.verify(System.out);
        }

        Integer errors = imdImage.getSectorErrorCount();
        if (strict && errors != 0) {
            throw new VFDErrorsException("IMD file has sector errors", errors);
        }
        VFDImage vfdImage = VFDImage.from(imdImage);
        return vfdImage.toBytes();
    }

    public static void vfdToImd(File vfd, File imd) {
        // TODO
    }
}
