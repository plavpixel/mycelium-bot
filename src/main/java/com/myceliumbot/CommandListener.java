package com.myceliumbot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class CommandListener extends ListenerAdapter {

    private final ScriptManager scriptManager;

    public CommandListener(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();

        if (scriptManager.hasScript(commandName)) {
            // Defer the reply so the script has time to execute and send a response.
            event.deferReply().queue();

            // Execute the script in a new thread to avoid blocking JDA's gateway
            new Thread(() -> scriptManager.executeScript(commandName, event)).start();

        } else {
            event.reply("Unknown command! This script may not be loaded.").setEphemeral(true).queue();
        }
    }
}