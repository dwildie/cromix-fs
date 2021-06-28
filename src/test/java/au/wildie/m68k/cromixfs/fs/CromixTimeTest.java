package au.wildie.m68k.cromixfs.fs;

import org.junit.Test;

import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class CromixTimeTest {

    @Test
    public void now() {
        System.out.printf("UNIX epoch:   %s\n", CromixTime.unixEpoch.toFtarString());
        System.out.printf("Cromix epoch: %s\n", CromixTime.epoch.toFtarString());
        System.out.printf("Now:          %s\n", CromixTime.now().toFtarString());

        CromixTime now = CromixTime.now();
        assertThat(now.getYear(), is((byte)121));
    }

    @Test
    public void from() {
        Date now = new Date();
        System.out.printf("Now:          %s\n", CromixTime.from(now).toFtarString());

        CromixTime nowC = CromixTime.now();
        assertThat(nowC.getYear(), is((byte)121));
    }
}