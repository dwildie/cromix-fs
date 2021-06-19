package au.wildie.m68k.cromixfs;

import java.io.OutputStream;
import java.io.PrintStream;

public abstract class CromemcoTest {

    protected PrintStream getDummyPrintStream() {
        return new PrintStream(new OutputStream(){
            public void write(int b) {
                // NO-OP
            }
        });
    }
}
