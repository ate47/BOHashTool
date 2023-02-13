package fr.atesab.bo4hash.utils;

public class HashUtils {

    private static final long SCRIPT_NAMESPACE = 0x30FCC2BF;
    private static final long NUM_START = Long.parseUnsignedLong("cbf29ce484222325", 16);
    public static long hashComp(String input) {
        long hash = 0x4B9ACE2FL;
        input = input.toLowerCase();

        for (int i = 0; i < input.length(); i++) {
            long c = input.charAt(i);
            hash = ((((c + hash) & 0xFFFFFFFFL) ^ (((c + hash) & 0xFFFFFFFFL) << 10))& 0xFFFFFFFFL) +
                    ((((((c + hash) & 0xFFFFFFFFL) ^ (((c + hash) & 0xFFFFFFFFL) << 10))& 0xFFFFFFFFL) >>> 6)& 0xFFFFFFFFL);
        }

        return (0x8001 * (((9 * hash) & 0xFFFFFFFFL) ^ (((9 * hash) & 0xFFFFFFFFL) >>> 11))) & 0xFFFFFFFFL;
    }

    public static long hashRes(String input) {
        long num = NUM_START;
        for (int index = 0; index < input.length(); ++index) {
            num = ((num ^ (long) input.charAt(index)) * 0x100000001b3L);
        }
        num &= 0xFFFFFFFFFFFFFFFL;
        return num;
    }
}
