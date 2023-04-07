package fr.atesab.bo4hash.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.LongStream;

public class StringUtils {
    public static long[] ENCRYPTION_VALUES;
    static {
        try {
            ENCRYPTION_VALUES = readEncryptionValues("encryptionValues.txt");
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private static long[] readEncryptionValues(String resource) throws IOException {
        try (InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Can't find resource \"" + resource + "\"");
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
            return r.lines().flatMapToLong(line -> {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    return LongStream.empty();
                }
                int lastIndex = 0;
                LongStream.Builder bld = LongStream.builder();
                while (lastIndex < line.length()) {
                    if (line.charAt(lastIndex) != '0' || line.charAt(lastIndex + 1) != 'x') {
                        // ignore bad character
                        continue;
                    }
                    int index = line.indexOf(' ', lastIndex);

                    if (index == -1) {
                        bld.accept(Long.parseLong(line, lastIndex + 2, line.length(), 16));
                        break;
                    } else {
                        bld.accept(Long.parseLong(line, lastIndex + 2, index, 16));
                        lastIndex = index + 1;
                    }
                }
                return bld.build();
            }).toArray();
        }
    }

    public static void main(String[] args) {
    }
}
