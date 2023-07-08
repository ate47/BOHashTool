package fr.atesab.bo4hash;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Translation utility
 */
public class I18n {
    public static final String DEFAULT_LANG = "en";
    public static final String[] LANGUAGES = {DEFAULT_LANG, "fr"};
    private static Map<String, String> translation = new HashMap<>();
    private static final Map<String, Map<String, String>> NODES = new HashMap<>();

    static {
        for (String language : LANGUAGES) {
            NODES.put(language, loadLangFile(language));
        }
    }

    private static void checkHotLoad() {
        // hot load the default language if nothing was loaded
        if (NODES.isEmpty()) {
            loadLang(DEFAULT_LANG);
        }
    }

    public static void loadLang(String lang) {
        Map<String, String> trans = NODES.get(lang);
        if (trans == null) {
            trans = NODES.get(DEFAULT_LANG);
            if (trans == null) {
                throw new Error("Can't find lang");
            }
        }
        translation = trans;
    }

    private static Map<String, String> loadLangFile(String lang) {
        Map<String, String> trans = new HashMap<>();
        try {
            // search the language or the default language
            InputStream langFile = ClassLoader.getSystemClassLoader().getResourceAsStream("lang/" + lang + ".lg");
            if (langFile == null) {
                langFile = ClassLoader.getSystemClassLoader().getResourceAsStream("lang/" + DEFAULT_LANG + ".lg");
                if (langFile == null) {
                    throw new Error("Can't load I18n");
                }
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(langFile, StandardCharsets.UTF_8))) {
                String line;
                int lineIndex = 0;

                readLoop:
                while ((line = reader.readLine()) != null) {
                    lineIndex++;
                    int start = -1;
                    do {
                        start++;
                        if (start == line.length() || line.charAt(start) == '#') {
                            continue readLoop; // comment or empty line
                        }
                    } while (Character.isWhitespace(line.charAt(start)));

                    int shiftIndex = line.indexOf('=', start);

                    if (shiftIndex == -1) {
                        System.err.printf("Bad lang line at %d\n", lineIndex);
                        continue; // bad line
                    }

                    // add the translation to the pool
                    String key = line.substring(start, shiftIndex).toLowerCase();
                    String value = line.substring(shiftIndex + 1);

                    String old;
                    if (value.equals("@\"")) {
                        // multi-line component

                        String l2;
                        StringBuilder bld = new StringBuilder();

                        while ((l2 = reader.readLine()) != null) {
                            if (l2.equals("\"@")) {
                                break;
                            }
                            bld.append('\n').append(l2);
                        }
                        value = bld.toString();

                        if (value.isEmpty()) {
                            old = trans.put(key, value);
                        } else {
                            old = trans.put(key, value.substring(1));
                        }
                    } else {
                        old = trans.put(key, value);
                    }
                    if (old != null) {
                        System.err.printf("Translation collision for %s/%s\n", lang, key);
                    }
                }
            }
        } catch (IOException e) {
            throw new Error("Can't load i18n", e);
        }
        return trans;
    }

    public static Map<String, String> getTranslation() {
        return Collections.unmodifiableMap(translation);
    }

    public static Map<String, String> getTranslation(String lang) {
        return Collections.unmodifiableMap(NODES.get(lang));
    }

    public static Set<String> getLanguages() {
        return Collections.unmodifiableSet(NODES.keySet());
    }

    /**
     * translate a key
     *
     * @param key    translation key
     * @param format translation format
     * @return string
     */
    public static String get(String key, Object... format) {
        checkHotLoad();
        String s = translation.get(key.toLowerCase());
        if (s == null) {
            System.err.printf("Can't find translation for %s\n", key);
            return key;
        }
        return String.format(s, format);
    }

    /**
     * translate a key
     *
     * @param key    translation key
     * @param format translation format
     * @return string
     */
    public static String getOrNull(String key, Object... format) {
        checkHotLoad();
        String s = translation.get(key.toLowerCase());
        if (s == null) {
            System.err.printf("Can't find translation for %s\n", key);
            return null;
        }
        return String.format(s, format);
    }

    public static int getInt(String key, int defaultValue) {
        checkHotLoad();
        String s = translation.get(key.toLowerCase());
        if (s == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getLang() {
        return get("lang");
    }

    public static String getLangName() {
        return get("lang.name");
    }

    public static String getLangAuthor() {
        return get("lang.author");
    }


    public static String getForLang(String lang, String key, Object... format) {
        checkHotLoad();
        String s = getTranslation(lang).get(key.toLowerCase());
        if (s == null) {
            System.err.printf("Can't find translation for %s\n", key);
            return key;
        }
        return String.format(s, format);
    }

    public static String getOrNullForLang(String lang, String key, Object... format) {
        checkHotLoad();
        String s = getTranslation(lang).get(key.toLowerCase());
        if (s == null) {
            System.err.printf("Can't find translation for %s\n", key);
            return null;
        }
        return String.format(s, format);
    }

    public static int getIntForLang(String lang, String key, int defaultValue) {
        checkHotLoad();
        String s = getTranslation(lang).get(key.toLowerCase());
        if (s == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getLangForLang(String lang) {
        return getForLang(lang, "lang");
    }

    public static String getLangNameForLang(String lang) {
        return getForLang(lang, "lang.name");
    }

    public static String getLangAuthorForLang(String lang) {
        return getForLang(lang, "lang.author");
    }
}
