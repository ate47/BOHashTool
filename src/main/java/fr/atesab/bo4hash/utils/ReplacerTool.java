package fr.atesab.bo4hash.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReplacerTool {
    private record ReplaceObject(String hash, String replace) {
    }

    public static String replace(String directory, String keys) throws IOException {
        Path dir = Path.of(directory);
        List<ReplaceObject> replaceObjects = readObjects(keys);
        Map<String, String> hashIndex;
        try {
            hashIndex = replaceObjects.stream()
                    .collect(Collectors.toMap(ReplaceObject::hash, ReplaceObject::replace, (v1, v2) -> {
                        if (!v1.equals(v2)) {
                            throw new RuntimeException("Duplicate key for " + v1 + " and " + v2);
                        }
                        return v1;
                    }));
        } catch (RuntimeException e) {
            return e.getMessage();
        }

        try (Stream<Path> list = Files.walk(dir)) {
            AtomicLong read = new AtomicLong();
            AtomicLong updated = new AtomicLong();
            String output = list.parallel().map(p -> {
                        if (Files.isDirectory(p)) {
                            return null; // ignore dir
                        }
                        String name = dir.relativize(p).getFileName().toString();
                        // ignore non GSC file
                        if (!(name.endsWith(".gsc") || name.endsWith(".csc"))) {
                            return null;
                        }
                        String fileReplacement = hashIndex.get(name.substring(0, name.length() - 4));
                        if (fileReplacement != null) {
                            Path newP = dir.resolve(fileReplacement);
                            try {
                                Files.createDirectories(newP.getParent());
                                Files.move(p, newP);
                                p = newP;
                                updated.incrementAndGet();
                            } catch (IOException e) {
                                return "can't move " + p + " to " + newP + ": " + e.getMessage();
                            }
                        }

                        String s;
                        try {
                            s = Files.readString(p, StandardCharsets.UTF_8);
                            read.incrementAndGet();
                        } catch (IOException e) {
                            return "can't load " + p + ": " + e.getMessage();
                        }
                        String ns = s;
                        // faster because it's most likely smaller as the file's hashes
                        for (ReplaceObject obj : replaceObjects) {
                            ns = ns.replace(obj.hash(), obj.replace());
                        }
                        if (!s.equals(ns)) {
                            try {
                                Files.writeString(p, ns, StandardCharsets.UTF_8);
                                updated.incrementAndGet();
                            } catch (IOException e) {
                                return "can't write " + p + ": " + e.getMessage();
                            }
                        }
                        return null;
                    }).filter(Objects::nonNull)
                    .collect(Collectors.joining("\n"));
            return output + (output.isEmpty() ? "" : "\n") + read.get() + " file(s) read / " + updated.get() + " file(s) updated";
        }
    }

    private static List<ReplaceObject> readObjects(String keys) {
        return Arrays.stream(keys.split("\n"))
                .map(line -> {
                    int idx = line.indexOf(',');

                    if (idx == -1 || line.indexOf(',', idx + 1) != -1) {
                        System.err.println("Can't parse line: " + line);
                        return null;
                    }

                    String hash = line.substring(0, idx);
                    String value = line.substring(idx + 1);

                    if (hash.startsWith("script_")) {
                        value = value.replace('/', '\\');
                    }

                    return new ReplaceObject(hash, value);
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
