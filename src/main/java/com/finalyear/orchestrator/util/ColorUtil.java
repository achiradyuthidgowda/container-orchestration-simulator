package com.finalyear.orchestrator.util;

/**
 * ANSI escape-code constants for coloured terminal output.
 *
 * <p>Usage: {@code ColorUtil.GREEN + "text" + ColorUtil.RESET}
 *
 * <p>On Windows consoles that don't support ANSI (e.g. legacy cmd.exe) the
 * codes are ignored gracefully by Windows Terminal / PowerShell 7+. You can
 * also add the JVM flag {@code -Djansi=true} if using the Jansi library.
 */
public final class ColorUtil {

    // ── Colours ───────────────────────────────────────────────────────────────
    public static final String RESET   = "\u001B[0m";
    public static final String BLACK   = "\u001B[30m";
    public static final String RED     = "\u001B[31m";
    public static final String GREEN   = "\u001B[32m";
    public static final String YELLOW  = "\u001B[33m";
    public static final String BLUE    = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN    = "\u001B[36m";
    public static final String WHITE   = "\u001B[37m";

    // ── Bright variants ───────────────────────────────────────────────────────
    public static final String BRIGHT_RED     = "\u001B[91m";
    public static final String BRIGHT_GREEN   = "\u001B[92m";
    public static final String BRIGHT_YELLOW  = "\u001B[93m";
    public static final String BRIGHT_BLUE    = "\u001B[94m";
    public static final String BRIGHT_MAGENTA = "\u001B[95m";
    public static final String BRIGHT_CYAN    = "\u001B[96m";
    public static final String BRIGHT_WHITE   = "\u001B[97m";

    // ── Text styles ───────────────────────────────────────────────────────────
    public static final String BOLD      = "\u001B[1m";
    public static final String UNDERLINE = "\u001B[4m";

    private ColorUtil() { /* utility class – no instantiation */ }

    /**
     * Renders a simple ASCII progress bar.
     *
     * @param percent  value in [0, 100]
     * @param width    total bar width in characters
     * @return coloured string like {@code [████████░░] 80%}
     */
    public static String progressBar(double percent, int width) {
        int filled = (int) Math.round((percent / 100.0) * width);
        filled = Math.max(0, Math.min(filled, width));
        String bar = "█".repeat(filled) + "░".repeat(width - filled);
        String colour = (percent >= 80) ? RED : (percent >= 50) ? YELLOW : GREEN;
        return colour + "[" + bar + "]" + RESET + String.format(" %5.1f%%", percent);
    }
}
