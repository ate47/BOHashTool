package fr.atesab.bo4hash;

import fr.atesab.bo4hash.utils.HashUtils;
import fr.atesab.bo4hash.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import java.util.concurrent.atomic.AtomicInteger;
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
            return (s) -> {
            };
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

    private final Map<Long, Set<Obj>> idfs;
    private final Map<Long, Obj> strings;
    private final Map<Long, Obj> files;

    public Searcher() {
        idfs = Collections.synchronizedMap(new HashMap<>());
        strings = Collections.synchronizedMap(new HashMap<>());
        files = Collections.synchronizedMap(new HashMap<>());
    }

    public String saveDb(Path dbFile) {
        try (BufferedOutputStream w = new BufferedOutputStream(Files.newOutputStream(dbFile))) {

            IOUtils.write(w, idfs.size());
            for (Map.Entry<Long, Set<Obj>> e : idfs.entrySet()) {
                long key = e.getKey();
                Set<Obj> objs = e.getValue();
                IOUtils.write(w, key);
                IOUtils.write(w, objs.size());
                for (Obj obj : objs) {
                    IOUtils.write(w, obj.element());
                    IOUtils.write(w, obj.hash());
                }
            }

            IOUtils.write(w, strings.size());
            for (Map.Entry<Long, Obj> e : strings.entrySet()) {
                long key = e.getKey();
                Obj obj = e.getValue();
                IOUtils.write(w, key);
                IOUtils.write(w, obj.element());
                IOUtils.write(w, obj.hash());
            }

            IOUtils.write(w, files.size());
            for (Map.Entry<Long, Obj> e : files.entrySet()) {
                long key = e.getKey();
                Obj obj = e.getValue();
                IOUtils.write(w, key);
                IOUtils.write(w, obj.element());
                IOUtils.write(w, obj.hash());
            }

            return "saved";
        } catch (IOException e) {
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            return w.toString();
        }
    }

    public String loadDb(Path dbFile) {
        idfs.clear();
        strings.clear();
        files.clear();

        try (BufferedInputStream r = new BufferedInputStream(Files.newInputStream(dbFile))) {
            int idfsCount = (int) IOUtils.readInt64(r);
            for (int i = 0; i < idfsCount; i++) {
                long key = IOUtils.readInt64(r);
                int objsCount = (int) IOUtils.readInt64(r);
                Set<Obj> set = new HashSet<>();
                for (int j = 0; j < objsCount; j++) {
                    String element = IOUtils.readStr(r);
                    String hash = IOUtils.readStr(r);
                    set.add(new Obj(hash, element));
                }
                idfs.put(key, set);
            }

            int stringsCount = (int) IOUtils.readInt64(r);
            for (int i = 0; i < stringsCount; i++) {
                long key = IOUtils.readInt64(r);
                String element = IOUtils.readStr(r);
                String hash = IOUtils.readStr(r);
                strings.put(key, new Obj(hash, element));
            }
            int filesCount = (int) IOUtils.readInt64(r);
            for (int i = 0; i < filesCount; i++) {
                long key = IOUtils.readInt64(r);
                String element = IOUtils.readStr(r);
                String hash = IOUtils.readStr(r);
                files.put(key, new Obj(hash, element));
            }

            return "loaded";
        } catch (IOException e) {
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            return w.toString();
        }
    }

    public String load(String dirPath) {
        return load(dirPath, IndexListener.empty());
    }

    public String load(String dirPath, IndexListener listener) {
        try {
            Path dir = Path.of(dirPath);

            if (!Files.exists(dir)) {
                return I18n.get("searcher.badDir");
            }
            idfs.clear();
            strings.clear();
            files.clear();

            Pattern hashPattern = Pattern.compile("(hash|script)_([0-9a-fA-F]+)");
            Pattern compPattern = Pattern.compile("(function|namespace|var|class)_([0-9a-fA-F]+)");

            String loadingI18n = I18n.get("loader.loading");

            AtomicInteger count = new AtomicInteger(1);
            try (Stream<Path> list = Files.walk(dir)) {
                list.parallel().forEach(p -> {
                    if (Files.isDirectory(p)) {
                        return; // ignore dir
                    }
                    String name = dir.relativize(p).getFileName().toString();

                    if (name.endsWith(".list")) {
                        try (Stream<String> l = Files.lines(p)) {
                            l.forEach(line -> {
                                if (!line.startsWith("ximage_")) {
                                    return;
                                }

                                int extIndex = line.indexOf('.');

                                String hash;
                                if (extIndex != -1) {
                                    hash = line.substring("ximage_".length(), extIndex);
                                } else {
                                    hash = line.substring("ximage_".length());
                                }

                                strings.put(Long.parseUnsignedLong(hash), new Obj(hash, line));
                            });
                        } catch (IOException e) {
                            throw new RuntimeException("can't load " + p + ": " + e.getMessage(), e);
                        }

                        return;
                    }

                    if (!(name.endsWith(".gsc") || name.endsWith(".csc"))) {
                        return;
                    }
                    listener.notification(loadingI18n + " #" + count.getAndIncrement() + " - " + name);
                    if (name.startsWith("script_")) {
                        Obj obj = new Obj(name.substring("script_".length(), name.length() - 4).toLowerCase(), name.substring(0, name.length() - 4));
                        files.put(Long.parseUnsignedLong(obj.hash(), 16), obj);
                    }
                    String s;
                    try {
                        s = Files.readString(p, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new RuntimeException(I18n.get("searcher.cantLoad", p) + ": " + e.getMessage(), e);
                    }
                    Matcher hashMatch = hashPattern.matcher(s);
                    while (hashMatch.find()) {
                        Obj obj = new Obj(hashMatch.group(2).toLowerCase(), hashMatch.group());
                        if (obj.hash().isEmpty()) {
                            continue;
                        }
                        Obj old = strings.put(Long.parseUnsignedLong(obj.hash(), 16), obj);
                        if (old != null && !obj.element().equals(old.element())) {
                            listener.notification(I18n.get("searcher.collision") + " " + old.element() + "/" + obj.element());
                        }
                    }
                    Matcher compMatch = compPattern.matcher(s);
                    while (compMatch.find()) {
                        Obj obj = new Obj(compMatch.group(2).toLowerCase(), compMatch.group());
                        if (obj.hash().isEmpty()) {
                            continue;
                        }
                        idfs.computeIfAbsent(Long.parseUnsignedLong(obj.hash(), 16), hash -> Collections.synchronizedSet(new HashSet<>())).add(obj);
                    }
                });
            }

            Path hashes = Path.of("hashes.txt");
            listener.notification(I18n.get("searcher.write.hash", hashes.toAbsolutePath()));
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
            listener.notification(I18n.get("searcher.write.idfs", comp.toAbsolutePath()));
            try (BufferedWriter w = Files.newBufferedWriter(comp)) {
                for (Set<Obj> cc : idfs.values()) {
                    for (var c : cc) {
                        w.append(c.hash()).append(",").append(c.element()).append("\n");
                    }
                    w.flush();
                }
            }
            String output = I18n.get("searcher.notif",
                    strings.size() + files.size() + idfs.size(), strings.size(), files.size(), idfs.size());
            listener.notification(output);
            return output;
        } catch (Throwable t) {
            t.printStackTrace();
            return t.getMessage();
        }
    }


    public Obj searchString(String text) {
        long hashString = HashUtils.hashFNV(text);
        return strings.get(hashString);
    }

    public List<Obj> search(String text) {
        long hashObjectValue;
        if (text.indexOf('/') != -1 || text.indexOf('\\') != -1) {
            hashObjectValue = 0;
        } else {
            hashObjectValue = HashUtils.hashIDF(text);
        }
        long hashStringValue = HashUtils.hashFNV(text);
        return search(hashStringValue, hashObjectValue);
    }

    public Stream<Found> bruteForceAsync(String[] prefixes, String[] mid, String suffix, int maxSize, String dict) {
        return bruteForceAsync(prefixes, mid, suffix, maxSize, dict, 0);
    }

    public Stream<Found> bruteForceAsync(String[] prefixes, String[] mid, String suffix, int maxSize, String dict, long shift) {
        long maxId = 1;
        for (int i = 0; i < maxSize; i++) {
            maxId = Math.multiplyExact(maxId, dict.length());
        }
        final long fmaxId = maxId;
        AtomicLong count = new AtomicLong(shift);
        return LongStream.range(shift, maxId).parallel().mapToObj(ignored -> {
                    long id = count.getAndIncrement();
                    long loc = id;

                    StringBuilder b = new StringBuilder();

                    do {
                        b.append(dict.charAt((int) (loc % dict.length())));
                        loc /= dict.length();
                    } while (loc > 0);

                    if (id % 100_000_000 == 1) {
                        System.out.println("tried " + (id - 1) + "/" + fmaxId + " elements: " + b.reverse());
                    }

                    return b.reverse();
                })
                .flatMap(text -> Stream.of(prefixes).map(prefix -> {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append(prefix).append(text);
                    for (String m : mid) {
                        buffer.append(m).append(text);
                    }
                    String match = buffer.append(suffix).toString();
                    long hashStringValue = HashUtils.hashFNV(match);
                    // Obj obj = files.get(hashStringValue);
                    Obj obj = strings.get(hashStringValue);


                    if (obj != null) {
                        return new Found(match, obj);
                    } else {
                        return null;
                    }
                })).filter(Objects::nonNull);
    }

    public Stream<Found> bruteForceAsync2(String prefix, String suffix, int maxSize, String dict, String combiner) {
        return bruteForceAsync2(prefix, suffix, maxSize, dict, 0, combiner);
    }

    public Stream<Found> bruteForceAsync2(String prefix, String suffix, int maxSize, String dict, long shift, String combiner) {
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
                b.append(dict.charAt((int) (loc % dict.length())));
                loc /= dict.length();
            } while (loc > 0);

            String s = b.reverse().toString();
            String match = prefix + s + combiner + s + suffix;
            long hashStringValue = HashUtils.hashFNV(match);
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

    public List<Obj> search(long hashString, long hashObject) {
        List<Searcher.Obj> objs = new ArrayList<>();


        Searcher.Obj stringsObj = strings.get(hashString);

        if (stringsObj != null) {
            objs.add(stringsObj);
        }

        if (hashObject != 0) {
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
