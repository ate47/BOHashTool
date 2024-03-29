package fr.atesab.bo4hash;

import fr.atesab.bo4hash.ui.HashSearcherFrame;
import fr.atesab.bo4hash.ui.LoadingFrame;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Main {
    public static Properties readLastLoad() {
        Properties prop = new Properties();
        try (BufferedReader r = Files.newBufferedReader(Path.of("bohash.cfg"))) {
            prop.load(r);
            return prop;
        } catch (IOException e) {
            return new Properties();
        }
    }

    public static void saveLoad(Properties prop) {
        try (BufferedWriter r = Files.newBufferedWriter(Path.of("bohash.cfg"))) {
            prop.store(r, "bohash tool cfg");
        } catch (IOException ignore) {
        }
    }

    public static final String CFG_PATH = "cfg.path";

    public static final String CFG_LOOKUP_PATH = "cfg.lookupPath";

    public static final String CFG_EXTRACT_PATH = "cfg.extractPath";
    public static final String CFG_LARGE_SEARCH_PATTERN = "cfg.search.large.pattern";
    public static final String CFG_LARGE_SEARCH_EXPAND = "cfg.search.large.expand";
    public static final String CFG_LANGUAGE = "cfg.language";

    public static void main(String[] args) {
        Properties prop = readLastLoad();
        try {
            I18n.loadLang(prop.getProperty(Main.CFG_LANGUAGE));
            LoadingFrame.ResultSearcher searcher = LoadingFrame.loadSearcher(prop);
            new HashSearcherFrame(prop, searcher.searcher(), searcher.text());
        } catch (Throwable t) {
            StringWriter writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer));
            new HashSearcherFrame(new Properties(), new Searcher(), writer.toString());
        }
    }


}
// bf29ce484222325