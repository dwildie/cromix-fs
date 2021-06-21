package au.wildie.m68k.cromixfs.fs;

import lombok.RequiredArgsConstructor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RequiredArgsConstructor
public class CromixTimeUtils {
    private final byte[] raw;
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String toString() {
        return String.format("%d-%02d-%02d %02d:%02d:%02d",
                1900 + (raw[0] & 0xFF),
                raw[1] & 0xFF,
                raw[2] & 0xFF,
                raw[3] & 0xFF,
                raw[4] & 0xFF,
                raw[5] & 0xFF);
    }

    public Date toDate() throws ParseException {
        return formatter.parse(toString());
    }





}
