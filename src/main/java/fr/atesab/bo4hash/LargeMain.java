package fr.atesab.bo4hash;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class LargeMain {
    public static void main(String[] args) throws IOException {
        Properties prop = Main.readLastLoad();
        Searcher searcher = new Searcher();
        String pathCfg = prop.getProperty(Main.CFG_PATH);
        System.out.println(pathCfg);
        System.out.println(searcher.load(pathCfg));
        // 1570000001
        try (BufferedWriter w = Files.newBufferedWriter(Path.of("brute.txt"))) {
            //scripts/wz_common/gametypes
            String dict = "abcdefghijklmnopqrstuvwxyz_";
            long n = dict.length();
            searcher.bruteForceAsync("scripts/zm_common/", "/zm_powerup_nuke.gsc", 10, dict, n)
                    .forEach(next -> {
                        System.out.println(next.key() + "," + next.hash().element());
                        try {
                            w.append(next.key()).append(",").append(next.hash().element()).append("\n").flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
