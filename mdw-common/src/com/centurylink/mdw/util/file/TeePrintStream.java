/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Tee output to a file as well as another PrintStream.
 */
public class TeePrintStream extends PrintStream {
    private PrintStream printStream;

    public static void main(String[] args) {
        TeePrintStream teePrintStream = null;
        try {
            teePrintStream = new TeePrintStream(System.out, new File("c:/temp/out.log"));
            teePrintStream.println("Output via println()");
            teePrintStream.format("%s: %d", "Output via format()", 12345678);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        finally {
            if (teePrintStream != null)
                teePrintStream.close();
        }
    }

    public TeePrintStream(PrintStream printStream, File file) throws IOException {
        super(new FileOutputStream(file), true);
        this.printStream = printStream;
    }

    public boolean checkError() {
        return printStream.checkError() || super.checkError();
    }

    public void write(int b) {
        printStream.write(b);
        super.write(b);
    }

    public void write(byte[] buf, int off, int len) {
        printStream.write(buf, off, len);
        super.write(buf, off, len);
    }

    public void close() {
        if (printStream != System.out && printStream != System.err)
            printStream.close();
        super.close();
    }

    public void flush() {
        printStream.flush();
        super.flush();
    }
}