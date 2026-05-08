package com.zenvix.ui.logs;

/**
 * Represents a single line in the real-time log file, mapping string heuristics 
 * extracting LogLevel components and isolating Timestamps explicitly.
 */
public class LogLine {
    public final String timestamp;
    public final String level;
    public final String message;
    public final String rawLine;

    public LogLine(String rawLine) {
        this.rawLine = rawLine;
        
        String parsedLevel = "INFO";
        if (rawLine.contains("ERROR") || rawLine.contains("SEVERE")) parsedLevel = "ERROR";
        else if (rawLine.contains("WARN")) parsedLevel = "WARN";
        else if (rawLine.contains("DEBUG") || rawLine.contains("FINE")) parsedLevel = "DEBUG";
        
        this.level = parsedLevel;
        
        if (rawLine.length() > 20 && Character.isDigit(rawLine.charAt(0))) {
            this.timestamp = rawLine.substring(0, 19);
            this.message = rawLine.substring(19).trim();
        } else {
            this.timestamp = "";
            this.message = rawLine;
        }
    }

    @Override
    public String toString() {
        return rawLine;
    }
}
