package fr.atesab.bo4hash.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class IOUtils {

    public static void write(OutputStream stream, long int64) throws IOException {
        for (int i = 0; i < 8; i++) {
            stream.write((int) ((int64 >>> (i << 3)) & 0xFF));
        }
    }

    public static long readInt64(InputStream stream) throws IOException {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= (stream.read() & 0xFFL) << (i << 3);
        }
        return value;
    }

    public static void write(OutputStream stream, String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        write(stream, bytes.length);
        stream.write(bytes);
    }

    public static String readStr(InputStream stream) throws IOException {
        return new String(readBytes(stream, (int) readInt64(stream)), StandardCharsets.UTF_8);
    }

    public static byte[] readBytes(InputStream stream, int len) throws IOException {
        byte[] bytes = new byte[len];
        int toRead = len;

        while (toRead > 0) {
            int read = stream.read(bytes, 0, len);
            if (read == -1) {
                throw new EOFException();
            }
            toRead -= read;
        }
        return bytes;
    }


    private IOUtils() {
    }
}
