package fr.atesab.bo4hash;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Scanner {
    public static void main(String[] args) throws Exception {
        //scanner();
        //dlscanner();
        poly(args);
    }

    public static void scanner() throws IOException {
        Properties prop = Main.readLastLoad();
        String pathCfg = prop.getProperty(Main.CFG_PATH);
        Path dict = Path.of("dictionary.txt");
        Pattern elem = Pattern.compile("[a-z0-9]+");

        Set<String> elements = new HashSet<>();

        try (Stream<Path> files = Files.walk(Path.of(pathCfg));
             BufferedWriter writer = Files.newBufferedWriter(dict)) {

            for (Path f : (Iterable<? extends Path>) files::iterator) {
                String fs = f.toString();
                if (Files.isDirectory(f) || !(fs.endsWith(".gsc") || fs.endsWith(".csc"))) {
                    continue;
                }
                System.out.println(f.toAbsolutePath());
                try (Stream<String> lines = Files.lines(f)) {
                    for (String line : (Iterable<? extends String>) lines::iterator) {
                        Matcher matcher = elem.matcher(line);
                        while (matcher.find()) {
                            String idf = matcher.group();
                            if (elements.add(idf)) {
                                writer.write(idf + "\n");
                                if (elements.size() % 1024 == 0) {
                                    writer.flush();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void dlscanner() throws IOException {
        String[] indexes = {
                "https://github.com/Scobalula/GreyhoundPackageIndex/raw/master/PackageIndexSources/FNV1A/fnv1a_xsounds.csv",
                "https://github.com/Scobalula/GreyhoundPackageIndex/raw/master/PackageIndexSources/FNV1A/fnv1a_xmodels.csv",
                "https://github.com/Scobalula/GreyhoundPackageIndex/raw/master/PackageIndexSources/FNV1A/fnv1a_xmaterials.csv",
                "https://github.com/Scobalula/GreyhoundPackageIndex/raw/master/PackageIndexSources/FNV1A/fnv1a_ximages.csv",
                "https://github.com/Scobalula/GreyhoundPackageIndex/raw/master/PackageIndexSources/FNV1A/fnv1a_xanims.csv",
        };

        Path dldir = Path.of("downloaddir");
        Files.createDirectories(dldir);

        Path dict = Path.of("dictionarybig.txt");
        Pattern elem = Pattern.compile("[a-z0-9]+");

        Set<String> elements = new HashSet<>();

        try (BufferedWriter writer = Files.newBufferedWriter(dict)) {
            for (String index : indexes) {
                URL url = new URL(index);
                int i = index.lastIndexOf('/');
                assert i != -1;
                String fileName = index.substring(i + 1);

                Path fs = dldir.resolve(fileName);

                if (Files.exists(fs)) {
                    System.out.printf("%s found, no dl\n", fs);
                } else {
                    System.out.printf("dl %s\n", fs);
                }

                try (InputStream is = url.openStream()) {
                    Files.copy(is, fs);
                }

                try (Stream<String> lines = Files.lines(fs)) {
                    for (String line : (Iterable<? extends String>) lines::iterator) {
                        int cut = line.indexOf(',');
                        if (cut == -1) {
                            continue;
                        }
                        Matcher matcher = elem.matcher(line.substring(cut));

                        while (matcher.find()) {
                            String idf = matcher.group();
                            if (elements.add(idf)) {
                                writer.write(idf + "\n");
                                if (elements.size() % 1024 == 0) {
                                    writer.flush();
                                }
                            }
                        }

                    }
                }
            }
        }
    }


    private static final Object writer = new Object() {
    };

    public static void poly(String[] args) throws IOException, InterruptedException {
        Properties prop = Main.readLastLoad();
        String pathCfg = prop.getProperty(Main.CFG_PATH);
        Path dict = Path.of("N:/bo4hash/words.txt");
        //Path dict = Path.of("dictionarybig.txt");
        Searcher searcher = new Searcher();
        System.out.println(pathCfg);
        System.out.println(searcher.load(pathCfg));

        String[][] sets = {
                {"", ""},
                {"specialty_", ""},
                {"specialty_mod_", ""},
                {"specialty_detect", ""},
                {"specialty_immune", ""},
                {"specialty_showenemy", ""},
                {"specialty_sprint", ""},
                {"specialty_health", ""},
                {"specialty_fast", ""},

        };

        //final char[] mid = {'_', '/', ' '};
        final char[] mid = {'_', 0};


        List<String> dictionary = Files.readAllLines(dict);
        System.out.println("loaded " + dictionary.size() + " strings");

        try (BufferedWriter w = Files.newBufferedWriter(Path.of("brute.txt"), StandardOpenOption.APPEND)) {
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < sets.length; i++) {
                final int j = i;
                Thread t = new Thread(() -> {
                    System.out.println("Thread " + Thread.currentThread().getName() + " started!");
                    String pref = sets[j][0];
                    String suff = sets[j][1];
                    String[] buff = new String[20];
                    try {
                        long id = 0;

                        for (; ; ) {
                            long cid = id++;

                            int log = 0;
                            int fact = 1;
                            buff[log++] = dictionary.get((int) (cid % dictionary.size()));

                            while ((cid /= dictionary.size()) > 0) {
                                buff[log++] = (dictionary.get((int) (cid % dictionary.size())));
                                fact *= mid.length;
                            }


                            final int logStatic = log;
                            IntStream.range(0, fact).parallel().mapToObj(e -> {
                                StringBuilder bld = new StringBuilder(pref);

                                bld.append(buff[0]);
                                for (int k = 1; k < logStatic; k++) {
                                    char c = mid[e % mid.length];
                                    if (c != 0) {
                                        bld.append(c);
                                    }
                                    e /= mid.length;
                                    bld.append(buff[k]);
                                }
                                bld.append(suff);

                                return bld.toString();
                            }).forEach(ee -> {
                                Searcher.Obj next = searcher.searchString(ee);
                                if (next != null) {
                                    synchronized (writer) {
                                        System.out.println(next.element() + "," + ee);
                                        try {
                                            w.append(next.element()).append(",").append(ee).append("\n").flush();
                                        } catch (IOException ex) {
                                            throw new RuntimeException(ex);
                                        }
                                    }
                                }
                            });
                        }
                    } catch (Throwable t2) {
                        throw new RuntimeException(t2);
                    }
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
