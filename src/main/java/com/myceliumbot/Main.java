package com.myceliumbot;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Check for and create the .env file if it's missing
        checkForEnvFile();

        // Load environment variables from .env file
        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("DISCORD_TOKEN");

        // Check if the token is null or empty
        if (token == null || token.trim().isEmpty()) {
            System.err.println("############################################################");
            System.err.println("## ERROR: DISCORD_TOKEN is missing from the .env file.  ##");
            System.err.println("## Please add your bot token to the .env file and restart. ##");
            System.err.println("############################################################");
            System.exit(1);
        }

        // Initialize the ScriptManager (this will also create the /scripts dir if needed)
        ScriptManager scriptManager = new ScriptManager();
        System.out.println("Loading scripts from ./scripts directory...");
        scriptManager.loadScripts();

        // Build the JDA instance
        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .setActivity(Activity.watching("for script commands"))
                .addEventListeners(new CommandListener(scriptManager))
                .build();

        // Wait for JDA to be ready
        jda.awaitReady();

        // --- Startup Banner ---
        String botVersion = getBotVersion();
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");

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
        System.out.println("-------------");
        System.out.printf("Bot online %s on %s %s%n", botVersion, javaVendor, javaVersion);
        System.out.println("-------------");


        // Automatically register all commands found in the scripts directory
        CommandRegistry.registerCommands(jda);
    }

    /**
     * Checks if a .env file exists in the project root. If not, it creates one.
     */
    private static void checkForEnvFile() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            System.out.println("No .env file found. Creating an empty one for you.");
            try {
                if (envFile.createNewFile()) {
                    System.out.println(".env file created successfully.");
                }
            } catch (IOException e) {
                // If file creation fails, the subsequent token check will handle the error gracefully.
                System.err.println("Warning: Could not create .env file: " + e.getMessage());
            }
        }
    }

    /**
     * Reads the bot version from the version.properties file in the classpath.
     * This works both in the IDE and from the packaged JAR.
     * @return The bot version string, or "unknown" if it cannot be found.
     */
    private static String getBotVersion() {
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("version.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                System.err.println("Sorry, unable to find version.properties");
                return "unknown";
            }
            prop.load(input);
            return prop.getProperty("bot.version");
        } catch (Exception ex) {
            ex.printStackTrace();
            return "unknown";
        }
    }
}
