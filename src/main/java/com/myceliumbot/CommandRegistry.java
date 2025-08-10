package com.myceliumbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.util.List;

public class CommandRegistry {

    /**
     * Registers a list of slash commands with Discord.
     * @param jda The JDA instance.
     * @param commandsToRegister The list of commands to register.
     */
    public static void registerCommands(JDA jda, List<SlashCommandData> commandsToRegister) {
        if (commandsToRegister != null && !commandsToRegister.isEmpty()) {
            System.out.println("Registering " + commandsToRegister.size() + " commands with Discord...");
            CommandListUpdateAction updateAction = jda.updateCommands();
            updateAction.addCommands(commandsToRegister).queue(
                    (success) -> System.out.println("Successfully registered/updated " + success.size() + " commands!"),
                    (error) -> System.err.println("Failed to register commands with Discord: " + error.getMessage())
            );
        } else {
            System.out.println("No commands found to register.");
        }
    }
}