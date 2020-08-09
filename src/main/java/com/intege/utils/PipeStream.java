package com.intege.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PipeStream extends Thread {

    InputStream is;
    OutputStream os;

    public PipeStream(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = this.is.read(buffer)) >= 0) {
                this.os.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
