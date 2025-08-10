package com.myceliumbot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import org.jetbrains.annotations.NotNull;

public class CommandListener extends ListenerAdapter {
    private final ScriptManager scriptManager;
    private final BotConfig config;

    public CommandListener(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
        this.config = BotConfig.getInstance();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // Skip DM commands if disabled
        if (!config.isAllowDMCommands() && event.getChannelType() == ChannelType.PRIVATE) {
            event.reply("Commands in DMs are disabled").setEphemeral(true).queue();
            return;
        }

        // Log commands if enabled
        if (config.isLogCommands()) {
            String guildName = event.getGuild() != null ? event.getGuild().getName() : "DM";
            String userName = event.getUser().getName();
            System.out.printf("[Command] %s used /%s in %s%n",
                    userName, event.getName(), guildName);
        }

        // Handle the command
        event.deferReply().queue();
        scriptManager.handleCommand(event);
    }
}