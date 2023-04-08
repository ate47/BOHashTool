package fr.atesab.bo4hash;

import fr.atesab.bo4hash.utils.HashUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class DataFetcher {
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("#namespace ([a-zA-Z_0-9]*);");
    private static final Pattern CLASS_PATTERN = Pattern.compile("class ([a-zA-Z_0-9]*)");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("function( (autoexec|private))? ([a-zA-Z_0-9]*)");
    private static final Pattern STRING_PATTERN = Pattern.compile("\"(([^\"]|(\\\\\"))*)\"");

    public static String fetch(String location, String outputFile) {
        if (location == null || location.isEmpty()) {
            return "empty location";
        }
        if (outputFile == null || outputFile.isEmpty()) {
            return "empty outputFile";
        }
        String log = "";
        try {
            System.out.println("location " + location);
            System.out.println("outputFile " + outputFile);
            Path dir = Path.of(location);
            Path outputFilePath = Path.of(outputFile);

            Set<String> namespaces = new TreeSet<>();
            Set<String> classes = new TreeSet<>();
            Set<String> functions = new TreeSet<>();
            Set<String> strings = new TreeSet<>();
            Set<String> scripts = new TreeSet<>();

            try (Stream<Path> walk = Files.walk(dir)) {
                walk.forEach(p -> {
                    if (Files.isDirectory(p)) {
                        return; // ignore dir
                    }
                    Path rp = dir.relativize(p);
                    String name = rp.getFileName().toString();
                    if (!(name.endsWith(".gsc") || name.endsWith(".csc"))) {
                        return;
                    }


                    if (name.startsWith("script_")) {
                        scripts.add(name.substring(0, name.length() - ".gsc".length()));
                    } else {
                        scripts.add(rp.toString().replace('\\', '/'));
                    }


                    try {
                        String content = Files.readString(p);

                        Matcher nsm = NAMESPACE_PATTERN.matcher(content);

                        while (nsm.find()) {
                            namespaces.add(nsm.group(1));
                        }

                        Matcher cm = CLASS_PATTERN.matcher(content);

                        while (cm.find()) {
                            classes.add(cm.group(1));
                        }

                        Matcher fm = FUNCTION_PATTERN.matcher(content);

                        while (fm.find()) {
                            functions.add(fm.group(fm.groupCount()));
                        }


                        Matcher sm = STRING_PATTERN.matcher(content);

                        while (sm.find()) {
                            strings.add(sm.group(1));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            Files.createDirectories(outputFilePath);

            try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath.resolve("namespaces.txt"))) {
                for (String namespace : namespaces) {
                    writer.append(namespace).write("\n");
                }
            }
            try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath.resolve("classes.txt"))) {
                for (String cls : classes) {
                    writer.append(cls).write("\n");
                }
            }
            try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath.resolve("functions.txt"))) {
                for (String function : functions) {
                    writer.append(function).write("\n");
                }
            }
            try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath.resolve("strings.txt"))) {
                for (String string : strings) {
                    writer.append(string).write("\n");
                }
            }
            try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath.resolve("scripts.txt"))) {
                for (String script : scripts) {
                    writer.append(Long.toString(HashUtils.hashFNV(script), 16)).append(",").append(script).write("\n");
                }
            }
            long total = 0;
            try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath.resolve("dataset.csv"))) {
                long scriptCount = dumpDataset(writer, "script", scripts);
                total += scriptCount;
                log += "write " + scriptCount + " script(s)\n";
                long hashCount = dumpDataset(writer, "hash", strings);
                total += hashCount;
                log += "write " + hashCount + " hash(es)\n";
                long classCount = dumpDataset(writer, "class", classes);
                total += classCount;
                log += "write " + classCount + " class(es)\n";
                long functionCount = dumpDataset(writer, "function", functions);
                total += functionCount;
                log += "write " + functionCount + " function(s)\n";
                long namespaceCount = dumpDataset(writer, "namespace", namespaces);
                total += namespaceCount;
                log += "write " + namespaceCount + " namespace(s)\n";

            }
            log += "write " + total + " element(s)\n";
        } catch (Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            return writer.toString();
        }
        return log + "write to " + outputFile;
    }

    private static long dumpDataset(BufferedWriter writer, String type, Set<String> dataset) throws IOException {
        boolean fnv = HashUtils.isFNV(type);
        String prefix = type + "_";
        long count = 0;
        for (String entry : dataset) {
            if (!entry.startsWith(prefix)) {
                if (fnv) {
                    writer.write(prefix + Long.toUnsignedString(HashUtils.hashFNV(entry), 16) + "," + entry + "\n");
                } else {
                    writer.write(prefix + Long.toUnsignedString(HashUtils.hashIDF(entry), 16) + "," + entry + "\n");
                }
                count++;
            }
        }
        writer.flush();
        return count;
    }
}
