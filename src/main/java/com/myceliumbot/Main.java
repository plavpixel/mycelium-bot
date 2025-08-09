package com.myceliumbot;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.InputStream;
import java.util.Properties;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Load environment variables from .env file
        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("DISCORD_TOKEN");

        if (token == null) {
            System.err.println("Error: DISCORD_TOKEN not found in .env file.");
            System.exit(1);
        }

        // Initialize the ScriptManager
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
        String botVersion = getBotVersion(); // Dynamically get the version
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
        System.out.printf("Bot online version %s on %s %s%n", botVersion, javaVendor, javaVersion);
        System.out.println("-------------");


        // Automatically register all commands found in the scripts directory
        CommandRegistry.registerCommands(jda);
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
