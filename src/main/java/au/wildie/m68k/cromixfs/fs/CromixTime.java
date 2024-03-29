package au.wildie.m68k.cromixfs.fs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CromixTime {
    public static final int TIME_SIZE = 6;
    private static final int YEAR = 0;
    private static final int MONTH = 1;
    private static final int DAY = 2;
    private static final int HOUR = 3;
    private static final int MINUTE = 4;
    private static final int SECOND = 5;


    private byte year;           // year 0 .. 99
    private byte month;          // month 1 .. 12
    private byte day;            // day 1 .. 31
    private byte hour;           // hour 0 .. 23
    private byte minute;         // minute 0 .. 59
    private byte second;         // second 0 .. 59

    private final static byte[] tab1 = {31,30,31,30,31,31,30,31,30,31,31,29};
    private final static byte[] tab2 = { 1, 2, 4, 5, 7, 8, 9,11,12,14,15,16};

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final CromixTime unixEpoch = new CromixTime((byte)70,(byte)01,(byte)1,(byte)0,(byte)0,(byte)0);
    public static final CromixTime epoch = new CromixTime((byte)60,(byte)03,(byte)1,(byte)0,(byte)0,(byte)0);

    private final static String[] MONTHS =
            {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    /*
        Convert CROMIX time to UNIX like time.
        UNIX like time is measured in (unsigned)
        seconds from 00:00:00 on March 1, 1960.
        No errors.
    */
    public long utime() {
        long y, m, d;

        y = year - 60;
        if ((m = month) < 3) {
            m += 9;
            --y;
        } else {
            m -= 3;
        }
        d = 365 * y + (y >> 2) + (m << 5) + day - tab2[(int) m];
        return(d * 86400 + second + 60 * (minute + 60 * hour));
    }

    /*
        Convert UNIX like time expressed as (unsigned)
        number of second from 00:00:00 on March 1, 1960
        to CROMIX time of the form (y,m,d,h,m,s).
        No errors.
    */
    public static CromixTime from(long t) {
        long d;
        int m;

        CromixTime ct = new CromixTime();

        d = t / 86400;
        t = t % 86400;
        ct.setSecond((byte)(t % 60));
        t /= 60;
        ct.setMinute((byte) (t % 60));
        ct.setHour((byte) (t / 60));

        t = d / (4 * 365 + 1) << 2;  d %= 4 * 365 + 1;
        if (d == 4 * 365) {
            t += 3;
            d = 365;
        } else {
            t += d / 365;
            d %= 365;
        }

        m = (int) (d >> 5);
        d &= 31;
        d += tab2[m];
        if (d > tab1[m]) {
            d -= tab1[m++];
        }
        ct.setDay((byte) d);
        if ((m += 3) > 12) {
            m -= 12;
            ++t;
        }
        ct.setMonth((byte) m);
        ct.setYear((byte) (t + 60));

        return ct;
    }

    public static CromixTime now() {
        return from(Instant.now());
    }

    public static CromixTime from(Date date) {
        return from(date.toInstant());
    }

    public static CromixTime from(Instant instant) {
        long mtime = instant.getEpochSecond()
                + ZoneId.systemDefault().getRules().getOffset(instant).getTotalSeconds();
        return from(mtime + unixEpoch.utime() - epoch.utime());
    }

    public static CromixTime from(byte[] raw) {
        CromixTime ct = new CromixTime();
        ct.year = raw[YEAR];
        ct.month = raw[MONTH];
        ct.day = raw[DAY];
        ct.hour = raw[HOUR];
        ct.minute = raw[MINUTE];
        ct.second = raw[SECOND];
        return ct;
    }

    public byte[] toBytes() {
        byte[] data = new byte[6];
        data[YEAR] = year;
        data[MONTH] = month;
        data[DAY] = day;
        data[HOUR] = hour;
        data[MINUTE] = minute;
        data[SECOND] = second;
        return data;
    }

    public Date toDate() throws ParseException {
        return formatter.parse(toString());
    }

    @Override
    public String toString() {
        return String.format("%d-%02d-%02d %02d:%02d:%02d",
                1900 + (year & 0xFF),
                month & 0xFF,
                day & 0xFF,
                hour & 0xFF,
                minute & 0xFF,
                second & 0xFF);
    }

    public String toFtarString() {
        return String.format("%s %02d, %4d %02d:%02d", MONTHS[month], day, 1900 + year, hour, minute);
    }
}
