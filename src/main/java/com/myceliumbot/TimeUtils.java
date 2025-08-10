package com.myceliumbot;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {

    // Compiling the pattern once for efficiency
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)\\s*(d|h|m|s)", Pattern.CASE_INSENSITIVE);

    /**
     * Parses a duration string (e.g., "1h30m", "10s") into a total number of seconds.
     * @param durationStr The string to parse.
     * @return The total duration in seconds, or 0 if the string is invalid.
     */
    public long parseDuration(String durationStr) {
        long totalSeconds = 0;
        Matcher matcher = DURATION_PATTERN.matcher(durationStr);
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            switch (unit) {
                case "d":
                    totalSeconds += value * 86400;
                    break;
                case "h":
                    totalSeconds += value * 3600;
                    break;
                case "m":
                    totalSeconds += value * 60;
                    break;
                case "s":
                    totalSeconds += value;
                    break;
            }
        }
        return totalSeconds;
    }

    /**
     * Formats a total number of seconds into a human-readable string.
     * @param totalSeconds The duration in seconds.
     * @return A formatted string like "1 day, 2 hours, 30 minutes, and 5 seconds".
     */
    public String formatDuration(long totalSeconds) {
        if (totalSeconds < 0) {
            return "invalid duration";
        }
        if (totalSeconds == 0) {
            return "0 seconds";
        }

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        List<String> parts = new ArrayList<>();
        if (days > 0) {
            parts.add(days + (days == 1 ? " day" : " days"));
        }
        if (hours > 0) {
            parts.add(hours + (hours == 1 ? " hour" : " hours"));
        }
        if (minutes > 0) {
            parts.add(minutes + (minutes == 1 ? " minute" : " minutes"));
        }
        if (seconds > 0) {
            parts.add(seconds + (seconds == 1 ? " second" : " seconds"));
        }

        if (parts.isEmpty()) {
            return "0 seconds";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            sb.append(parts.get(i));
            if (i < parts.size() - 2) {
                sb.append(", ");
            } else if (i == parts.size() - 2) {
                sb.append(", and ");
            }
        }

        return sb.toString();
    }
}
