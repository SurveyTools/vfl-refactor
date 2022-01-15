package org.surveytools.flightlogger.util;

public class KMPMatch {
	/**
     * Knuth-Morris-Pratt Algorithm
     * Pattern match that is based on the idea that when a mismatch occurs, the pattern itself embodies 
     * sufficient information to determine where the next match could begin.
     */
    public static int indexOf(byte[] data, byte[] matchPattern) {
        int[] failure = computeFailure(matchPattern);

        int j = 0;

        for (int i = 0; i < data.length; i++) {
            while (j > 0 && matchPattern[j] != data[i]) {
                j = failure[j - 1];
            }
            if (matchPattern[j] == data[i]) { 
                j++; 
            }
            if (j == matchPattern.length) {
                return i - matchPattern.length + 1;
            }
        }
        return -1;
    }

    private static int[] computeFailure(byte[] pattern) {
        int[] failure = new int[pattern.length];

        int j = 0;
        for (int i = 1; i < pattern.length; i++) {
            while (j>0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }

        return failure;
    }

}
