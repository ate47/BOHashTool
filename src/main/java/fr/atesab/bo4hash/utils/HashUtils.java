package fr.atesab.bo4hash.utils;

import java.nio.file.Path;
import java.util.Set;

public class HashUtils {
    private static final Set<String> IDF_TYPES = Set.of("var", "function", "class", "namespace", "event");
    private static final Set<String> FNV_TYPES = Set.of("script", "hash");

    private static final long SCRIPT_NAMESPACE = 0x30FCC2BF;
    private static final long NUM_START = Long.parseUnsignedLong("cbf29ce484222325", 16);

    public static boolean isIDF(String type) {
        return IDF_TYPES.contains(type);
    }

    public static boolean isFNV(String type) {
        return FNV_TYPES.contains(type);
    }

    public static boolean isHashFile(String name) {
        return name.endsWith(".gsc") || name.endsWith(".csc") || name.endsWith(".gcsc") || name.endsWith(".csv");
    }

    public static long hashIDF(String input) {
        long hash = 0x4B9ACE2FL;
        input = input.toLowerCase();

        for (int i = 0; i < input.length(); i++) {
            long c = input.charAt(i);
            hash = ((((c + hash) & 0xFFFFFFFFL) ^ (((c + hash) & 0xFFFFFFFFL) << 10)) & 0xFFFFFFFFL) +
                    ((((((c + hash) & 0xFFFFFFFFL) ^ (((c + hash) & 0xFFFFFFFFL) << 10)) & 0xFFFFFFFFL) >>> 6) & 0xFFFFFFFFL);
        }

        return (0x8001 * (((9 * hash) & 0xFFFFFFFFL) ^ (((9 * hash) & 0xFFFFFFFFL) >>> 11))) & 0xFFFFFFFFL;
    }

    public static long hashFNV(String input) {
        long num = NUM_START;
        for (int index = 0; index < input.length(); ++index) {
            num ^= Character.toLowerCase(input.charAt(index));
            num *= 0x100000001b3L;
        }
        return num & 0x7FFFFFFF_FFFFFFFFL;
    }
}
