package fr.atesab.bo4hash;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class MainReader {
    public static void main(String[] args) throws IOException {
        Properties prop = Main.readLastLoad();
        Searcher searcher = new Searcher();
        Pattern idfs = Pattern.compile("([^0-9A-Za-z_])([a-zA-Z_][a-zA-Z_0-9/\\\\]+)");

        String[] ignored = new String[] {
                "func_",
                "function_",
                "namespace_",
                "var_",
                "hash_",
                "script_"
        };

        Path dir = Path.of(prop.getProperty(Main.CFG_PATH));
        Set<String> words = new HashSet<>();
        try (Stream<Path> list = Files.walk(dir)) {
            list.parallel().forEach(p -> {
                if (Files.isDirectory(p)) {
                    return; // ignore dir
                }
                String name = dir.relativize(p).getFileName().toString();
                System.out.println("loading " + name);
                if (!(name.endsWith(".gsc") || name.endsWith(".csc"))) {
                    return;
                }
                String s;
                try {
                    s = Files.readString(p, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("can't load " + p + ": " + e.getMessage(), e);
                }
                Matcher matcher = idfs.matcher(s);
                matcherName:
                while (matcher.find()) {
                    String find = matcher.group(2);
                    for (String ignore : ignored) {
                        if (find.startsWith(ignore)) {
                            // setp this idf
                            continue matcherName;
                        }
                    }
                    words.addAll(Arrays.asList(find.toLowerCase().split("[\\\\_]")));
                }
            });
        }
        words.remove("");
        try (BufferedWriter w = Files.newBufferedWriter(Path.of("words.txt"))) {
            for (String word : words) {
                w.append(word).append("\n");
                w.flush();
            }
        }
        String[] arr = words.toArray(new String[0]);
        System.out.println("start search with " + arr.length + " elements");

        Object b = new Object() {
        };
        String[] splits = {"\\", "_", ""};
        try (BufferedWriter w = Files.newBufferedWriter(Path.of("brute.txt"))) {
            long sum = (long) arr.length * arr.length * arr.length * arr.length * splits.length;
            System.out.println((sum) + "iterations");
            AtomicLong countSum = new AtomicLong();
            AtomicLong current = new AtomicLong();
            LongStream.range(7_000_000_000L, sum / splits.length)
                    .parallel()
                    .forEach(wid -> {
                        final long count = current.incrementAndGet();
                        for (String split : splits) {
                            long id = count;
                            StringBuilder buffer = new StringBuilder();
                            buffer.append(arr[((int) (id % arr.length))]);
                            id /= arr.length;

                            while (id > 0) {
                                buffer.append(split).append(arr[((int) (id % arr.length))]);
                                id /= arr.length;
                            }

                            String k = buffer.toString();
                            List<Searcher.Obj> objs = searcher.search(k);
                            if (!objs.isEmpty()) {
                                synchronized (b) {
                                    try {
                                        for (Searcher.Obj obj : objs) {
                                            w.append(String.valueOf(count)).append(" - ").append(k).append(",").append(obj.element()).append("\n").flush();
                                            System.out.println("find : " + k + "," + obj.element());
                                        }
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                            long c = countSum.incrementAndGet();
                            if (c % 1_000_000 == 0) {
                                System.out.println(c  + "/" + sum + " (" + (c * 100 / sum) + "%) last: " + k);
                            }
                        }
                    });
        }
        System.out.println("done");
    }
}
