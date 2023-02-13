package fr.atesab.bo4hash;

import fr.atesab.bo4hash.utils.HashUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class Searcher {
    public interface IndexListener {
        static IndexListener ofNullable(IndexListener listener) {
            return listener == null ? empty() : listener;
        }
        static IndexListener empty() {
            return (s) -> {};
        }
        static IndexListener sout() {
            return System.out::println;
        }
        void notification(String text);
    }
    public record Obj(String hash, String element) {
    }
    public record Found(String key, Searcher.Obj hash) {
    }

    private final Map<String, Set<Obj>> idfs;
    private final Map<String, Obj> strings;
    private final Map<String, Obj> files;

    public Searcher() {
        idfs = Collections.synchronizedMap(new HashMap<>());
        strings = Collections.synchronizedMap(new HashMap<>());
        files = Collections.synchronizedMap(new HashMap<>());
    }

    public String load(String dirPath) {
        return load(dirPath, IndexListener.empty());
    }
    public String load(String dirPath, IndexListener listener) {
        try {
            Path dir = Path.of(dirPath);

            if (!Files.exists(dir)) {
                return "file doesn't exists!";
            }
            idfs.clear();
            strings.clear();
            files.clear();

            Pattern hashPattern = Pattern.compile("(hash|script)_([0-9a-fA-F]+)");
            Pattern compPattern = Pattern.compile("(function|namespace|var|class)_([0-9a-fA-F]+)");

            try (Stream<Path> list = Files.walk(dir)) {
                list.parallel().forEach(p -> {
                    if (Files.isDirectory(p)) {
                        return; // ignore dir
                    }
                    String name = dir.relativize(p).getFileName().toString();
                    if (!(name.endsWith(".gsc") || name.endsWith(".csc"))) {
                        return;
                    }
                    listener.notification("loading " + name);
                    if (name.startsWith("script_")) {
                        Obj obj = new Obj(name.substring("script_".length(), name.length() - 4).toLowerCase().substring(1), name.substring(0, name.length() - 4));
                        files.put(obj.hash(), obj);
                    }
                    String s;
                    try {
                        s = Files.readString(p, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new RuntimeException("can't load " + p + ": " + e.getMessage(), e);
                    }
                    Matcher hashMatch = hashPattern.matcher(s);
                    while (hashMatch.find()) {
                        Obj obj = new Obj(hashMatch.group(2).toLowerCase().substring(1), hashMatch.group());
                        Obj old = strings.put(obj.hash(), obj);
                        if (old != null && !obj.element().equals(old.element())) {
                            listener.notification("Warning collision! " + old.element() + "/" + obj.element());
                        }
                    }
                    Matcher compMatch = compPattern.matcher(s);
                    while (compMatch.find()) {
                        Obj obj = new Obj(compMatch.group(2).toLowerCase(), compMatch.group());
                        idfs.computeIfAbsent(obj.hash(), hash -> Collections.synchronizedSet(new HashSet<>())).add(obj);
                    }
                });
            }

            Path hashes = Path.of("hashes.txt");
            listener.notification("Write hashes in " + hashes.toAbsolutePath());
            try (BufferedWriter w = Files.newBufferedWriter(hashes)) {
                for (Obj hash : strings.values()) {
                    w.append(hash.hash()).append(",").append(hash.element()).append("\n");
                    w.flush();
                }
                for (Obj hash : files.values()) {
                    w.append(hash.hash()).append(",").append(hash.element()).append("\n");
                    w.flush();
                }
            }
            Path comp = Path.of("comp.txt");
            listener.notification("Write identifiers in " + comp.toAbsolutePath());
            try (BufferedWriter w = Files.newBufferedWriter(comp)) {
                for (Set<Obj> cc : idfs.values()) {
                    for (var c : cc) {
                        w.append(c.hash()).append(",").append(c.element()).append("\n");
                    }
                    w.flush();
                }
            }
            String output = "loaded " + (strings.size() + files.size() + idfs.size()) + " hash(es) | " + strings.size() + " strings, " + files.size() + " files, " + idfs.size() + " idfs";
            listener.notification(output);
            return output;
        } catch (Throwable t) {
            t.printStackTrace();
            return t.getMessage();
        }
    }


    public Obj searchString(String text) {
        String hashString = Long.toUnsignedString(HashUtils.hashRes(text), 16).toLowerCase();
        return strings.get(hashString);
    }

    public List<Obj> search(String text) {
        String hashObjectValue;
        if (text.indexOf('/') != -1 || text.indexOf('\\') != -1) {
            hashObjectValue = "";
        } else {
            hashObjectValue = Long.toUnsignedString(HashUtils.hashComp(text), 16).toLowerCase();
        }
        String hashStringValue = Long.toUnsignedString(HashUtils.hashRes(text), 16).toLowerCase();

        return search(hashStringValue, hashObjectValue);
    }

    public Stream<Found> bruteForceAsync(String prefix, String suffix, int maxSize, String dict) {
        return bruteForceAsync(prefix, suffix, maxSize, dict, 0);
    }
    public Stream<Found> bruteForceAsync(String prefix, String suffix, int maxSize, String dict, long shift) {
        long maxId = 1;
        for (int i = 0; i < maxSize; i++) {
            maxId = Math.multiplyExact(maxId, dict.length());
        }
        final long fmaxId = maxId;
        AtomicLong count = new AtomicLong(shift);
        return LongStream.range(shift, maxId).mapToObj(ignored -> {
            long id = count.getAndIncrement();
            long loc = id;

            StringBuilder b = new StringBuilder();

            do {
                b.append(dict.charAt((int)(loc % dict.length())));
                loc /= dict.length();
            } while (loc > 0);

            String match = prefix + b.reverse() + suffix;
            String hashStringValue = Long.toUnsignedString(HashUtils.hashRes(match), 16).toLowerCase();
            // Obj obj = files.get(hashStringValue);
            Obj obj = strings.get(hashStringValue);

            if (id % 10_000_000 == 1) {
                System.out.println("tried " + (id - 1) + "/" + fmaxId + " elements: " + match);
            }

            if (obj != null) {
                return new Searcher.Found(match, obj);
            } else {
                return null;
            }
        }).filter(Objects::nonNull).parallel();
    }

    public List<Obj> search(String hashString, String hashObject) {
        List<Searcher.Obj> objs = new ArrayList<>();

        Searcher.Obj stringsObj = strings.get(hashString);

        if (stringsObj != null) {
            objs.add(stringsObj);
        }

        if (!hashObject.isEmpty()) {
            Set<Obj> lstObj = idfs.get(hashObject);

            if (lstObj != null) {
                objs.addAll(lstObj);
            }
        }

        Searcher.Obj filesObj = files.get(hashString);

        if (filesObj != null) {
            objs.add(filesObj);
        }

        return objs;
    }
}
