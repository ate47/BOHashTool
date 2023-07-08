package fr.atesab.bo4hash;

import fr.atesab.bo4hash.utils.ExpandTool;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class LargeSearchTool {
    private final Set<String> pattern = new HashSet<>();
    private final Set<String> search = new HashSet<>();

    public void load(String cfg) {
        pattern.clear();
        search.clear();
        pattern.add("%se");
        for (String l : cfg.split("\n")) {
            if (l.startsWith("p\"")) {
                pattern.add(l.substring(2).toLowerCase());
            } else {
                search.add(l.toLowerCase());
            }
        }
    }

    public Stream<String> expand() {
        return pattern.stream()
                .flatMap(pattern -> ExpandTool.expand(search.stream())
                        .map(s -> pattern.replace("%se", s)));
    }
}