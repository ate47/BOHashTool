package fr.atesab.bo4hash;

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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Searcher {
    public record Obj(String hash, String element) {
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
        try {
            Path dir = Path.of(dirPath);

            if (!Files.exists(dir)) {
                return "file doesn't exists!";
            }
            idfs.clear();
            strings.clear();
            files.clear();

            Pattern hashPattern = Pattern.compile("hash_([0-9a-fA-F]+)");
            Pattern compPattern = Pattern.compile("(function|namespace|var|class)_([0-9a-fA-F]+)");

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
                    if (name.startsWith("script_")) {
                        Obj obj = new Obj(name.substring("script_".length(), name.length() - 4).toLowerCase().substring(1), name);
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
                        Obj obj = new Obj(hashMatch.group(1).toLowerCase().substring(1), hashMatch.group());
                        Obj old = strings.put(obj.hash(), obj);
                        if (old != null && !obj.element().equals(old.element())) {
                            System.err.println("Warning collision! " + old.element() + "/" + obj.element());
                        }
                    }
                    Matcher compMatch = compPattern.matcher(s);
                    while (compMatch.find()) {
                        Obj obj = new Obj(compMatch.group(2).toLowerCase(), compMatch.group());
                        idfs.computeIfAbsent(obj.hash(), hash -> Collections.synchronizedSet(new HashSet<>())).add(obj);
                    }
                });
            }
            try (BufferedWriter w = Files.newBufferedWriter(Path.of("hashes.txt"))) {
                for (Obj hash : strings.values()) {
                    w.append(hash.hash()).append(",").append(hash.element()).append("\n");
                    w.flush();
                }
                for (Obj hash : files.values()) {
                    w.append(hash.hash()).append(",").append(hash.element()).append("\n");
                    w.flush();
                }
            }
            try (BufferedWriter w = Files.newBufferedWriter(Path.of("comp.txt"))) {
                for (Set<Obj> cc : idfs.values()) {
                    for (var c : cc) {
                        w.append(c.hash()).append(",").append(c.element()).append("\n");
                    }
                    w.flush();
                }
            }
            return "loaded " + (strings.size() + files.size() + idfs.size()) + " hash(es) | " + strings.size() + " strings, " + files.size() + " files, " + idfs.size() + " idfs";
        } catch (Throwable t) {
            t.printStackTrace();
            return t.getMessage();
        }
    }


    public Obj searchString(String text) {
        String hashString = Long.toUnsignedString(Hash.hashRes(text), 16).toLowerCase();
        return strings.get(hashString);
    }

    public List<Obj> search(String text) {
        String hashStringValue = Long.toUnsignedString(Hash.hashRes(text), 16).toLowerCase();
        String hashObjectValue = Long.toUnsignedString(Hash.hashComp(text), 16).toLowerCase();

        return search(hashStringValue, hashObjectValue);
    }

    public List<Obj> search(String hashString, String hashObject) {
        List<Searcher.Obj> objs = new ArrayList<>();

        Searcher.Obj stringsObj = strings.get(hashString);

        if (stringsObj != null) {
            objs.add(stringsObj);
        }

        Set<Obj> lstObj = idfs.get(hashObject);

        if (lstObj != null) {
            objs.addAll(lstObj);
        }

        Searcher.Obj filesObj = files.get(hashString);

        if (filesObj != null) {
            objs.add(filesObj);
        }

        return objs;
    }
}
