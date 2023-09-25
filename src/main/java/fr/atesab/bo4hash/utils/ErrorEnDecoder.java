package fr.atesab.bo4hash.utils;

import fr.atesab.bo4hash.ui.InfoTabInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ErrorEnDecoder {
    private static final List<List<String>> WORDS;
    private static final int[] WORDS_LOG_SIZES = {6, 10, 8, 8};

    static {

        List<List<String>> words = new ArrayList<>();

        String text;

        for (int i = 1; i <= 4; i++) {
            String fileName = "stringtables/words_" + i + ".txt";
            try (InputStream is = InfoTabInfo.class.getClassLoader().getResourceAsStream(fileName)) {
                if (is == null) {
                    throw new Error("Can't read " + fileName);
                }

                words.add(new BufferedReader(new InputStreamReader(is)).lines().toList());
            } catch (IOException e) {
                throw new Error(e);
            }
        }

        WORDS = Collections.unmodifiableList(words);
    }


    public static String encode(long code) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < WORDS.size(); i++) {
            int log2Size = WORDS_LOG_SIZES[i];
            int row = (int) (code & ((1 << log2Size) - 1));
            code >>= log2Size;

            b.append(" ").append(WORDS.get(i).get(row));
        }

        return b.substring(1);
    }

    public static long decode(String value) throws IllegalArgumentException {
        String[] split = value.split("\\s");

        if (split.length != WORDS.size()) {
            throw new IllegalArgumentException("An error should contain " + WORDS.size() + " components");
        }

        long code = 0;

        for (int i = split.length - 1; i >= 0; i--) {
            String s = split[i];

            int idx = WORDS.get(i).indexOf(s);

            if (idx == -1) {
                throw new IllegalArgumentException("Can't find component '" + s + "' please check the value/case.");
            }

            code |= idx;

            if (i > 0) {
                code <<= WORDS_LOG_SIZES[i - 1];
            }

        }

        System.out.println();

        return code;

    }

}
