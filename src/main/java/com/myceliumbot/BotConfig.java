package com.myceliumbot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BotConfig {
    // Bot appearance
    private String botName = "Mycelium";
    private String activityType = "WATCHING";
    private String activityText = "the network grow";
    private String embedColor = "#5865F2";
    private String errorColor = "#ED4245";
    private String successColor = "#57F287";

    // Bot behavior
    private int commandCooldownSeconds = 3;
    private boolean allowDMCommands = false;
    private List<Long> ownerIds = new ArrayList<>();
    private boolean mentionRepliesEnabled = true;

    // Paths and directories
    private String scriptsDirectory = "./scripts";
    private String databasePath = "./data/bot.db";
    private String logsDirectory = "./logs";

    // Debug settings
    private boolean debugMode = false;
    private boolean logCommands = true;
    private String logLevel = "INFO";

    // Script settings
    private List<String> disabledScripts = new ArrayList<>();
    private boolean enableScriptHotReload = false;
    private boolean enableJsConsoleAccess = false;

    // HTTP settings
    private int httpTimeoutSeconds = 30;
    private boolean allowUnsafeConnections = false;

    // Rate limiting
    private int globalRateLimitPerMinute = 100;
    private boolean enablePerUserRateLimit = true;
    private int perUserRateLimitPerMinute = 10;

    // Getters and setters
    public String getBotName() { return botName; }
    public void setBotName(String botName) { this.botName = botName; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getActivityText() { return activityText; }
    public void setActivityText(String activityText) { this.activityText = activityText; }

    public String getEmbedColor() { return embedColor; }
    public void setEmbedColor(String embedColor) { this.embedColor = embedColor; }

    public String getErrorColor() { return errorColor; }
    public void setErrorColor(String errorColor) { this.errorColor = errorColor; }

    public String getSuccessColor() { return successColor; }
    public void setSuccessColor(String successColor) { this.successColor = successColor; }

    public int getCommandCooldownSeconds() { return commandCooldownSeconds; }
    public void setCommandCooldownSeconds(int cooldown) { this.commandCooldownSeconds = cooldown; }

    public boolean isAllowDMCommands() { return allowDMCommands; }
    public void setAllowDMCommands(boolean allowDMCommands) { this.allowDMCommands = allowDMCommands; }

    public List<Long> getOwnerIds() { return ownerIds; }
    public void setOwnerIds(List<Long> ownerIds) { this.ownerIds = ownerIds; }

    public boolean isMentionRepliesEnabled() { return mentionRepliesEnabled; }
    public void setMentionRepliesEnabled(boolean mentionRepliesEnabled) { this.mentionRepliesEnabled = mentionRepliesEnabled; }

    public String getScriptsDirectory() { return scriptsDirectory; }
    public void setScriptsDirectory(String scriptsDirectory) { this.scriptsDirectory = scriptsDirectory; }

    public String getDatabasePath() { return databasePath; }
    public void setDatabasePath(String databasePath) { this.databasePath = databasePath; }

    public String getLogsDirectory() { return logsDirectory; }
    public void setLogsDirectory(String logsDirectory) { this.logsDirectory = logsDirectory; }

    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }

    public boolean isLogCommands() { return logCommands; }
    public void setLogCommands(boolean logCommands) { this.logCommands = logCommands; }

    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

    public List<String> getDisabledScripts() { return disabledScripts; }
    public void setDisabledScripts(List<String> disabledScripts) { this.disabledScripts = disabledScripts; }

    public boolean isEnableScriptHotReload() { return enableScriptHotReload; }
    public void setEnableScriptHotReload(boolean enableScriptHotReload) { this.enableScriptHotReload = enableScriptHotReload; }

    public boolean isEnableJsConsoleAccess() { return enableJsConsoleAccess; }
    public void setEnableJsConsoleAccess(boolean enableJsConsoleAccess) { this.enableJsConsoleAccess = enableJsConsoleAccess; }

    public int getHttpTimeoutSeconds() { return httpTimeoutSeconds; }
    public void setHttpTimeoutSeconds(int httpTimeoutSeconds) { this.httpTimeoutSeconds = httpTimeoutSeconds; }

    public boolean isAllowUnsafeConnections() { return allowUnsafeConnections; }
    public void setAllowUnsafeConnections(boolean allowUnsafeConnections) { this.allowUnsafeConnections = allowUnsafeConnections; }

    public int getGlobalRateLimitPerMinute() { return globalRateLimitPerMinute; }
    public void setGlobalRateLimitPerMinute(int globalRateLimitPerMinute) { this.globalRateLimitPerMinute = globalRateLimitPerMinute; }

    public boolean isEnablePerUserRateLimit() { return enablePerUserRateLimit; }
    public void setEnablePerUserRateLimit(boolean enablePerUserRateLimit) { this.enablePerUserRateLimit = enablePerUserRateLimit; }

    public int getPerUserRateLimitPerMinute() { return perUserRateLimitPerMinute; }
    public void setPerUserRateLimitPerMinute(int perUserRateLimitPerMinute) { this.perUserRateLimitPerMinute = perUserRateLimitPerMinute; }

    // Utility methods
    @JsonIgnore
    public Color getEmbedColorAsColor() {
        return Color.decode(embedColor);
    }

    @JsonIgnore
    public Color getErrorColorAsColor() {
        return Color.decode(errorColor);
    }

    @JsonIgnore
    public Color getSuccessColorAsColor() {
        return Color.decode(successColor);
    }

    public boolean isUserOwner(long userId) {
        return ownerIds.contains(userId);
    }

    // Static config handling
    private static final File CONFIG_FILE = new File("config.json");
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static BotConfig instance;

    public static BotConfig getInstance() {
        if (instance == null) {
            instance = loadConfig();
        }
        return instance;
    }

    public static BotConfig loadConfig() {
        try {
            // Ensure parent directory for database exists, as defined in config
            // This is just a good practice for when paths are configurable.
            File dbFile = new File(new BotConfig().getDatabasePath());
            Files.createDirectories(dbFile.getParentFile().toPath());
        } catch (IOException e) {
            System.err.println("Warning: Could not create data directory: " + e.getMessage());
        }

        if (!CONFIG_FILE.exists()) {
            BotConfig defaultConfig = new BotConfig();
            try {
                mapper.writeValue(CONFIG_FILE, defaultConfig);
                System.out.println("Created default configuration file: config.json");
            } catch (IOException e) {
                System.err.println("Failed to create default config file: " + e.getMessage());
            }
            return defaultConfig;
        }

        try {
            instance = mapper.readValue(CONFIG_FILE, BotConfig.class);
            System.out.println("Loaded configuration from config.json");
            return instance;
        } catch (IOException e) {
            System.err.println("Error loading config, using defaults: " + e.getMessage());
            return new BotConfig();
        }
    }

    public void save() {
        try {
            mapper.writeValue(CONFIG_FILE, this);
            System.out.println("Configuration saved to config.json");
        } catch (IOException e) {
            System.err.println("Failed to save configuration: " + e.getMessage());
        }
    }
}