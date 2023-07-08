package fr.atesab.bo4hash;

import fr.atesab.bo4hash.utils.HashUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Lookup {
    private record LookupElement(String id, String[] data) {
    }

    private final List<LookupElement> HASHMAP = new ArrayList<>();

    public String loadFile(Path path) {
        Map<Long, List<String>> hashmap = new HashMap<>();
        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach(l -> {
                if (l.indexOf(' ') == -1 && l.indexOf('\t') == -1) {
                    hashmap.computeIfAbsent(HashUtils.hashIDF(l), k -> new ArrayList<>()).add(l);
                }
                hashmap.computeIfAbsent(HashUtils.hashFNV(l), k -> new ArrayList<>()).add(l);
            });
        } catch (Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            return writer.toString();
        }
        HASHMAP.clear();
        hashmap.forEach((hash, lst) -> HASHMAP.add(new LookupElement(Long.toUnsignedString(hash, 16).toLowerCase(), lst.toArray(String[]::new))));
        HASHMAP.sort(Comparator.comparing(LookupElement::id));
        return I18n.get("lookup.loaded", HASHMAP.stream().mapToLong(l -> l.data.length).sum());
    }

    public Stream<String> lookup(String hash) {
        if (HASHMAP.isEmpty()) {
            return Stream.of(I18n.get("lookup.empty"));
        }
        int split = hash.indexOf('_');
        int start;
        if (split == -1) {
            start = 0;
        } else {
            String s = hash.substring(0, split);
            if (HashUtils.isFNV(s) || HashUtils.isIDF(s)) {
                start = split + 1; // known hash
            } else {
                start = 0; // unknown
            }
        }


        String id = hash.substring(start).toLowerCase();

        if (id.isEmpty()) {
            return Stream.empty();
        }

        // it's a shitty string search algorithm, but even with 100k strings, my computer is still able to run it
        return HASHMAP.stream()
                .flatMap(le -> {
                    if (le.id.contains(id)) {
                        return Arrays.stream(le.data).map(s -> le.id + "," + s);
                    }
                    return Arrays.stream(le.data).filter(s -> s.contains(id)).map(s -> le.id + "," + s);
                });
    }
}
