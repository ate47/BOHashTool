package fr.atesab.bo4hash;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

public class LargeMain {
    public static void main(String[] args) throws IOException {
        Properties prop = Main.readLastLoad();
        Searcher searcher = new Searcher();
        String pathCfg = prop.getProperty(Main.CFG_PATH);
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
            String[] prefixes = {
                    "scripts/core_common/vehicles/"
            };

            String[] mid = {
            };

            searcher.bruteForceAsync(prefixes, mid, ".gsc", 10, dict, 0).forEach(next -> {
                        System.out.println(next.hash().element() + "," + next.key());
                        try {
                            w.append(next.hash().element()).append(",").append(next.key()).append("\n").flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
