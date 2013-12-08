package com.ensifera.animosity.craftirc;

/**
 * Utility class
 */
public final class Util {
    /**
     * Combine string array with delimiter
     * 
     * @param initialPos
     * @param parts
     * @param delimiter
     * @return
     * @throws ArrayIndexOutOfBoundsException
     */
    public static String combineSplit(int initialPos, String[] parts, String delimiter) throws ArrayIndexOutOfBoundsException {
        if (initialPos >= parts.length) {
            return "";
        }
        final StringBuilder result = new StringBuilder();
        result.append(parts[initialPos]);
        for (int i = initialPos + 1; i < parts.length; i++) {
            result.append(delimiter + parts[i]);
        }
        return result.toString();
    }
}
