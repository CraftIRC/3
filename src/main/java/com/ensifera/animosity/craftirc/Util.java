package com.ensifera.animosity.craftirc;

/**
 * Utility class
 */
public final class Util {
    /**
     * Combine string array with delimiter
     *
     * @param initialPos initial position
     * @param parts parts to combine
     * @param delimiter delimiter
     * @return combined string
     * @throws ArrayIndexOutOfBoundsException
     */
    public static String combineSplit(int initialPos, String[] parts, String delimiter) throws ArrayIndexOutOfBoundsException {
        if (initialPos >= parts.length) {
            return "";
        }
        final StringBuilder result = new StringBuilder();
        result.append(parts[initialPos]);
        for (int i = initialPos + 1; i < parts.length; i++) {
            result.append(delimiter).append(parts[i]);
        }
        return result.toString();
    }
}
