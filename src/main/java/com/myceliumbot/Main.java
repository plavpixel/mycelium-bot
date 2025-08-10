package com.myceliumbot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDAInfo; // <-- Import JDAInfo
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class Main {
    private static JDA jda;
    private static ScriptManager scriptManager;

    public static void main(String[] args) throws InterruptedException {
        printBanner();
        checkForEnvFile();

        // Load configuration and core services
        BotConfig config = BotConfig.getInstance();
        createDirectories(config);
        Dotenv dotenv = Dotenv.load();
        DatabaseManager dbManager = new DatabaseManager();
        HttpUtils httpUtils = new HttpUtils();

        // Initialize scriptManager and load scripts
        scriptManager = new ScriptManager(dbManager, httpUtils);
        List<SlashCommandData> commandsToRegister = scriptManager.loadScripts(); // This now returns the commands

        // Token check
        String token = dotenv.get("DISCORD_TOKEN");
        if (token == null || token.trim().isEmpty()) {
            System.err.println("############################################################");
            System.err.println("## ERROR: DISCORD_TOKEN is missing from the .env file.    ##");
            System.err.println("## Please add your bot token to the .env file and restart. ##");
            System.err.println("############################################################");
            System.exit(1);
        }

        // Configure activity
        Activity activity;
        switch (config.getActivityType().toUpperCase()) {
            case "PLAYING": activity = Activity.playing(config.getActivityText()); break;
            case "LISTENING": activity = Activity.listening(config.getActivityText()); break;
            case "COMPETING": activity = Activity.competing(config.getActivityText()); break;
            case "STREAMING": activity = Activity.streaming(config.getActivityText(), "https://www.twitch.tv/"); break;
            default: activity = Activity.watching(config.getActivityText()); break;
        }

        // Build JDA
        jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.ROLE_TAGS)
                .setActivity(activity)
                .addEventListeners(
                        new CommandListener(scriptManager),
                        new EventManager(scriptManager)
                )
                .build();

        jda.awaitReady();

        // Check for bot owners
        if (config.getOwnerIds().isEmpty()) {
            System.out.println("Info: No bot owners specified in config.json.");
        } else {
            System.out.println("Fetching owner information...");
            for (long ownerId : config.getOwnerIds()) {
                jda.retrieveUserById(ownerId).queue(
                        user -> System.out.printf("Bot owner set to: %s (%d)%n", user.getName(), user.getIdLong()),
                        failure -> System.err.printf("Warning: Could not find owner with ID: %d. Check config.json.%n", ownerId)
                );
            }
        }

        // Finalize setup
        Scheduler scheduler = new Scheduler(scriptManager, jda);
        scriptManager.setScheduler(scheduler);
        CommandRegistry.registerCommands(jda, commandsToRegister);
    }

    private static void createDirectories(BotConfig config) {
        try {
            Files.createDirectories(Paths.get(config.getScriptsDirectory()));
            Files.createDirectories(Paths.get(config.getLogsDirectory()));
            Files.createDirectories(Paths.get(config.getDatabasePath()).getParent());
        } catch (IOException e) {
            System.err.println("Warning: Could not create required directories: " + e.getMessage());
        }
    }

    private static void printBanner() {
        String botVersion = getBotVersion();
        String jdaVersion = JDAInfo.VERSION;
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        String vmName = System.getProperty("java.vm.name");
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");

        String asciiArt = """

                                   _ _
                                  | (_)
          _ __ ___  _   _  ___ ___| |_ _   _ _ __ ___
         | '_ ` _ \\| | | |/ __/ _ \\ | | | | | '_ ` _ \\
         | | | | | | |_| | (_|  __/ | | |_| | | | | | |
         |_| |_| |_|\\__, |\\___\\___|_|_|\\__,_|_| |_| |_|
                     __/ |
                    |___/
        """;
        System.out.println(asciiArt);
        System.out.println("----------------------------------------------------");
        System.out.printf(" Mycelium Bot v%s starting up...%n", botVersion);
        System.out.println("----------------------------------------------------");
        System.out.printf("  Library:      JDA v%s%n", jdaVersion);
        System.out.printf("  Environment:  %s %s (%s)%n", javaVendor, javaVersion, vmName);
        System.out.printf("  System:       %s (%s)%n", osName, osArch);
        System.out.println("----------------------------------------------------");
    }

    private static void checkForEnvFile() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            System.out.println("No .env file found. Creating an empty one.");
            try {
                if (!envFile.createNewFile()) {
                    System.err.println("Failed to create .env file.");
                }
            } catch (IOException e) {
                System.err.println("Failed to create .env file: " + e.getMessage());
            }
        }
    }

    private static String getBotVersion() {
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("version.properties")) {
            Properties prop = new Properties();
            if (input == null) return "unknown";
            prop.load(input);
            return prop.getProperty("bot.version");
        } catch (Exception ex) {
            return "unknown";
        }
    }

    public static void reloadScripts() {
        if (scriptManager != null && jda != null) {
            List<SlashCommandData> commands = scriptManager.loadScripts();
            CommandRegistry.registerCommands(jda, commands);
            System.out.println("Scripts reloaded successfully");
        } else {
            System.err.println("Cannot reload scripts: Bot not fully initialized");
        }
    }
}