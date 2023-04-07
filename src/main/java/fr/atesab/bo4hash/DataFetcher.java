package fr.atesab.bo4hash;

import fr.atesab.bo4hash.utils.HashUtils;

import java.io.BufferedWriter;
import java.io.IOException;
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

	public static void main(String[] args) throws IOException {
		Path dir = Path.of("C:\\Users\\wilat\\workspace\\t8-src");

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
		Path objects = Path.of("objects");
		Files.createDirectories(objects);

		try (BufferedWriter writer = Files.newBufferedWriter(objects.resolve("namespaces.txt"))) {
			for (String namespace : namespaces) {
				writer.append(namespace).write("\n");
			}
		}
		try (BufferedWriter writer = Files.newBufferedWriter(objects.resolve("classes.txt"))) {
			for (String cls : classes) {
				writer.append(cls).write("\n");
			}
		}
		try (BufferedWriter writer = Files.newBufferedWriter(objects.resolve("functions.txt"))) {
			for (String function : functions) {
				writer.append(function).write("\n");
			}
		}
		try (BufferedWriter writer = Files.newBufferedWriter(objects.resolve("strings.txt"))) {
			for (String string : strings) {
				writer.append(string).write("\n");
			}
		}
		try (BufferedWriter writer = Files.newBufferedWriter(objects.resolve("scripts.txt"))) {
			for (String script : scripts) {
				writer.append(Long.toString(HashUtils.hashRes(script), 16)).append(",").append(script).write("\n");
			}
		}
	}
}
