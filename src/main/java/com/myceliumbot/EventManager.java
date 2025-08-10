package com.myceliumbot;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class EventManager extends ListenerAdapter {
    private final ScriptManager scriptManager;

    public EventManager(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    private void handleGenericEvent(String eventType, GenericEvent event) {
        if (scriptManager.hasEventHandler(eventType)) {
            // Execute in a new thread to avoid blocking the gateway
            new Thread(() -> scriptManager.executeEventHandler(eventType, event)).start();
        }
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        handleGenericEvent("MEMBER_JOIN", event);
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        handleGenericEvent("MEMBER_LEAVE", event);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        handleGenericEvent("MESSAGE_RECEIVED", event);
    }
}
