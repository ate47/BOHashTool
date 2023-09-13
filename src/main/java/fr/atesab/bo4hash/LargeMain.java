package fr.atesab.bo4hash;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LargeMain {
    private static final Object syncWrite = new Object() {
    };

    public static void main(String[] args) throws IOException, InterruptedException {
        Properties prop = Main.readLastLoad();
        Searcher searcher = new Searcher();
        //String pathCfg = prop.getProperty(Main.CFG_PATH);
        String pathCfg = "N:\\bo4hash\\t8wip\\gsc\\output_hash";
        System.out.println(pathCfg);
        System.out.println(searcher.load(pathCfg));
        // 1570000001
        try (BufferedWriter w = Files.newBufferedWriter(Path.of("brute.txt"), StandardOpenOption.APPEND)) {
            //scripts/wz_common/gametypes
            // String dict = "abcdefghijklmnopqrstuvwxyz0123456789_";
            String dict = "abcdefghijklmnopqrstuvwxyz_";
            //String dict = "0123456789abcdef";
            // mp 260000000
            // boss_ 820000000
            // all     5400000000
            // all an 60030000000
            // character_ 2690000000
            // scripts/??/mp/??.gsc 255440000000
            String[][] prefixes = {
                    {"", ""},
                    {"specialty_", ""},
                    {"specialty_mod_", ""},
                    {"specialty_immune", ""},
                    {"specialty_showenemy", ""},
                    {"specialty_detect", ""},

            };

            String[] mid = {};


            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < prefixes.length; i++) {
                final int j = i;

                Thread t = new Thread(() -> {
                    System.out.println("Thread " + Thread.currentThread().getName() + " started!");
                    String[] f = {prefixes[j][0]};
                    searcher.bruteForceAsync(f, mid, prefixes[j][1], 10, dict, 0).forEach(next -> {
                        try {
                            synchronized (syncWrite) {
                                System.out.println(next.hash().element() + "," + next.key());
                                w.append(next.hash().element()).append(",").append(next.key()).append("\n").flush();
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }, "Runner#" + i);
                threads.add(t);
                t.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }
        }
    }
}
