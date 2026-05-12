package org.yawlfoundation.yawl.ui.util;

import java.io.Serializable;
import java.util.Comparator;

/**
 *
 * @author Michael Adams
 * @date 10/5/2026
 */
public class CaseIdComparator implements Comparator<String>, Serializable {

    @Override
    public int compare(String s1, String s2) {
        // Split by the boundaries between digits and non-digits
        String[] chunks1 = s1.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        String[] chunks2 = s2.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

        int length = Math.min(chunks1.length, chunks2.length);
        for (int i = 0; i < length; i++) {
            String c1 = chunks1[i];
            String c2 = chunks2[i];

            int result;
            // If both chunks are numeric, compare them as Longs
            if (Character.isDigit(c1.charAt(0)) && Character.isDigit(c2.charAt(0))) {
                result = Long.compare(Long.parseLong(c1), Long.parseLong(c2));
            } else {
                // Otherwise, compare as standard strings
                result = c1.compareTo(c2);
            }

            if (result != 0) return result;
        }

        // If all chunks match so far, the shorter string comes first
        return Integer.compare(chunks1.length, chunks2.length);
    }
}

