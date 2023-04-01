package fr.atesab.bo4hash.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ExpandTool {
    private static final String[] EXPAND = {"/", "\\", "_", "", " "};
    public static Stream<String> expand(Stream<String> stream) {
        return stream
                .flatMap(key -> {
                    String[] words = Stream.of(key.split("[\\\\/_ ]")).filter(s -> !s.isEmpty()).toArray(String[]::new);
                    if (words.length <= 1 || words.length > 3) {
                        // too big
                        return Stream.of(key);
                    }

                    Stream<List<String>> out = Stream.of(words).map(List::of);

                    for (int i = 1; i < words.length; i++) {
                        out = out.flatMap(k -> Stream.of(words).flatMap(word -> IntStream.range(0, k.size() + 1)
                                .mapToObj(id -> {
                                    List<String> lst = new ArrayList<>(k.size() + 1);

                                    lst.addAll(k.subList(0, id));
                                    lst.add(word);
                                    lst.addAll(k.subList(id, k.size()));

                                    return lst;
                                })));
                    }

                    return Stream.concat(Stream.of(key), out
                            .flatMap(k -> {
                                if (k.isEmpty()) {
                                    return Stream.empty();
                                }
                                List<String> result = new ArrayList<>();

                                result.add(k.get(0));

                                for (int i = 1; i < k.size(); i++) {
                                    ListIterator<String> it = result.listIterator();
                                    while (it.hasNext()) {
                                        String e = it.next();
                                        String nextElement = k.get(i);
                                        it.remove();
                                        for (String s : EXPAND) {
                                            it.add(e + s + nextElement);
                                        }
                                    }
                                }

                                return result.stream();
                            }));
                })
                .map(obj -> obj.replace('\\', '/'))
                .flatMap(obj -> {
                    if (obj.length() <= 4 || obj.charAt(obj.length() - 4) != '.') {
                        return Stream.of(obj);
                    }

                    String s = obj.substring(0, obj.length() - 4);
                    if (obj.endsWith("gsc")) {
                        int pathLast = obj.lastIndexOf('/');
                        if (pathLast != -1) {
                            String namespace = obj.substring(pathLast + 1);
                            return Stream.of(namespace, obj, s + ".csc");
                        } else {
                            return Stream.of(obj, s + ".csc");
                        }
                    }
                    if (obj.endsWith("csc")) {
                        int pathLast = obj.lastIndexOf('/');
                        if (pathLast != -1) {
                            String namespace = obj.substring(pathLast + 1);
                            return Stream.of(namespace, obj, s + ".gsc");
                        } else {
                            return Stream.of(obj, s + ".gsc");
                        }
                    }
                    return Stream.of(obj);
                })
                // reduce size
                .flatMap(obj -> {
                    if (obj.length() < 20) {
                        return IntStream.range(1, obj.length() + 1)
                                .mapToObj(l -> obj.substring(0, l));
                    } else {
                        return Stream.of(obj);
                    }
                });
    }
}
