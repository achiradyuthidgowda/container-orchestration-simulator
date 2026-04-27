package com.finalyear.orchestrator.logging;

import com.finalyear.orchestrator.util.ColorUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralised logging service.
 *
 * <p>Features:
 * <ul>
 *   <li>Coloured console output (ANSI escape codes)</li>
 *   <li>Persistent file output to {@code logs/logs.txt}</li>
 *   <li>Configurable minimum log level</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   EventLogger logger = EventLogger.getInstance();
 *   logger.info("Container started");
 *   logger.error("Node failed: node-2");
 * </pre>
 */
public class EventLogger {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final EventLogger INSTANCE = new EventLogger();

    public static EventLogger getInstance() { return INSTANCE; }

    // ── Configuration ─────────────────────────────────────────────────────────
    private static final String LOG_DIR  = "logs";
    private static final String LOG_FILE = LOG_DIR + "/logs.txt";

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Only messages at this level or above are printed / written. */
    private LogLevel minLevel = LogLevel.INFO;

    private PrintWriter fileWriter;

    // ── Constructor ───────────────────────────────────────────────────────────

    private EventLogger() {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
            fileWriter = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE, true)), true);
        } catch (IOException e) {
            System.err.println("[EventLogger] Cannot open log file: " + e.getMessage());
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setMinLevel(LogLevel level) { this.minLevel = level; }

    public void debug(String message)   { log(LogLevel.DEBUG,   message); }
    public void info(String message)    { log(LogLevel.INFO,    message); }
    public void success(String message) { log(LogLevel.SUCCESS, message); }
    public void warning(String message) { log(LogLevel.WARNING, message); }
    public void error(String message)   { log(LogLevel.ERROR,   message); }

    // ── Core log method ───────────────────────────────────────────────────────

    public synchronized void log(LogLevel level, String message) {
        if (level.ordinal() < minLevel.ordinal()) return;

        String time    = LocalDateTime.now().format(FMT);
        String tag     = String.format("[%-7s]", level);
        String plain   = time + " " + tag + " " + message;
        String colored = time + " " + coloredTag(level) + " " + coloredMessage(level, message);

        System.out.println(colored);

        if (fileWriter != null) {
            fileWriter.println(plain);
        }
    }

    /** Closes the file writer cleanly on JVM shutdown. */
    public synchronized void close() {
        if (fileWriter != null) {
            fileWriter.flush();
            fileWriter.close();
            fileWriter = null;
        }
    }

    // ── Colour helpers ────────────────────────────────────────────────────────

    private String coloredTag(LogLevel level) {
        String colour = switch (level) {
            case DEBUG   -> ColorUtil.CYAN;
            case INFO    -> ColorUtil.BRIGHT_BLUE;
            case SUCCESS -> ColorUtil.BRIGHT_GREEN;
            case WARNING -> ColorUtil.BRIGHT_YELLOW;
            case ERROR   -> ColorUtil.BRIGHT_RED;
        };
        return colour + ColorUtil.BOLD + String.format("[%-7s]", level) + ColorUtil.RESET;
    }

    private String coloredMessage(LogLevel level, String message) {
        return switch (level) {
            case ERROR   -> ColorUtil.RED    + message + ColorUtil.RESET;
            case WARNING -> ColorUtil.YELLOW + message + ColorUtil.RESET;
            case SUCCESS -> ColorUtil.GREEN  + message + ColorUtil.RESET;
            default      -> message;
        };
    }
}
