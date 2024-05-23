package au.wildie.m68k;

import au.wildie.m68k.cromixfs.disk.st.STDiskException;
import au.wildie.m68k.cromixfs.disk.vfd.InvalidVFDImageException;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static au.wildie.m68k.App.main;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void testCreateLarge() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        main(new String[]{"-ml", "/tmp/5l.IMD", "/home/dwildie/Downloads/SVT52.BIN"});
    }

    @Test
    public void testListLarge() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        main(new String[]{"-l", "/tmp/5l.IMD"});
    }

    @Test
    public void testCreateSmallWithFile() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        main(new String[]{"-ms", "/tmp/5s.IMD", "/home/dwildie/jay/SVT52.BIN"});
    }

    @Test
    public void testCreateSmall() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        main(new String[]{"-ms", "/tmp/5se.IMD"});
    }

    @Test
    public void testListMBR() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        main(new String[]{"-l", "-mbr", "2", "/home/dwildie/ide/cromemco-ide-master.img"});
    }

    @Test
    public void testListMBRAndPartition() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        main(new String[]{"-l", "-mbr", "0", "-p", "7", "/home/dwildie/mfm/20220705.D45.C1536.H16.master.ide.img"});
    }

    @Test
    public void testListPartition() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        main(new String[]{"-l", "-p", "7", "/home/dwildie/mfm/20220705.D45.C1536.H16.master.img"});
    }

    @Test
    public void testCheckPartition() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        main(new String[]{"-c", "-p", "0", "/home/dwildie/mfm/20220705.D45.C1536.H16.master.img"});
    }
    @Test
    public void testCheckMBR() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        main(new String[]{"-c", "-mbr", "2", "/home/dwildie/ide/cromemco-ide-master.img"});
    }

    @Test
    public void testListSmall() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        main(new String[]{"-l", "/tmp/5s.IMD"});
    }

    @Test
    public void testList169() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        main(new String[]{"-l", "/tmp/169C3079.IMD"});
    }


    @Test
    public void addSmall() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        main(new String[]{"-ms", "/tmp/5se.IMD"});
        main(new String[]{"-l", "/tmp/5se.IMD"});
        main(new String[]{"-a", "/tmp/5se.IMD", "/home/dwildie/jay/SVT52.BIN"});
//        App.main(new String[]{"-a", "/tmp/5se.IMD", "/home/dwildie/jay/SVT53.BIN"});
        main(new String[]{"-l", "/tmp/5se.IMD"});
    }

    @Test
    public void addSmallDir() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        main(new String[]{"-ms", "/tmp/5se.IMD"});
//        App.main(new String[]{"-l", "/tmp/5se.IMD"});
        main(new String[]{"-a", "/tmp/5se.IMD", "/home/dwildie/jay/adir"});
        main(new String[]{"-l", "/tmp/5se.IMD"});
    }

    @Test
    public void addLarge() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        main(new String[]{"-ml", "/tmp/5le.IMD"});
        main(new String[]{"-l", "/tmp/5le.IMD"});
        main(new String[]{"-a", "/tmp/5le.IMD", "/home/dwildie/jay/SVT52.BIN"});
        main(new String[]{"-a", "/tmp/5le.IMD", "/home/dwildie/jay/SVT53.BIN"});
        main(new String[]{"-l", "/tmp/5le.IMD"});
    }

    @Test
    public void add169() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        final String IMD = "169C3079.IMD";
        FileUtils.copyFileToDirectory(new File("/home/dwildie/jay/" + IMD), new File("/tmp"));
        main(new String[]{"-l", "/tmp/" + IMD});
        main(new String[]{"-a", "/tmp/" + IMD, "/home/dwildie/jay/SVT52.BIN"});
        main(new String[]{"-l", "/tmp/" + IMD});
    }

    @Test
    public void add211() throws InvalidVFDImageException, STDiskException, IOException, ParseException {
        final String IMD = "211C2063.IMD";
        FileUtils.copyFileToDirectory(new File("/home/dwildie/jay/" + IMD), new File("/tmp"));
        main(new String[]{"-l", "/tmp/" + IMD});
        main(new String[]{"-a", "/tmp/" + IMD, "/home/dwildie/jay/SVT52.BIN"});
        main(new String[]{"-l", "/tmp/" + IMD});
    }
}
