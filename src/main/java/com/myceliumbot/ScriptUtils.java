package com.myceliumbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.Color;
import java.time.Instant;

/**
 * A utility class passed to every script to provide helper methods
 * for common tasks, like creating standardized embeds.
 */
public class ScriptUtils {

    // Pre-defined colors for consistency
    public static final Color SUCCESS_COLOR = new Color(0x2ECC71);
    public static final Color ERROR_COLOR = new Color(0xE74C3C);
    public static final Color INFO_COLOR = new Color(0x3498DB);

    /**
     * Creates a generic EmbedBuilder with a title, description, and color.
     * @param title The title of the embed.
     * @param description The main text content of the embed.
     * @param color The color of the left-hand bar of the embed.
     * @return A pre-configured EmbedBuilder instance.
     */
    public EmbedBuilder createEmbed(String title, String description, Color color) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp(Instant.now());
    }

    /**
     * Creates a success-themed embed.
     * @param title The title of the embed.
     * @param description The description of the successful operation.
     * @return An EmbedBuilder instance styled for success.
     */
    public EmbedBuilder createSuccessEmbed(String title, String description) {
        return createEmbed(title, description, SUCCESS_COLOR);
    }

    /**
     * Creates an error-themed embed.
     * @param title The title of the embed.
     * @param description The description of the error.
     * @return An EmbedBuilder instance styled for errors.
     */
    public EmbedBuilder createErrorEmbed(String title, String description) {
        return createEmbed(title, description, ERROR_COLOR);
    }

    /**
     * Adds a standardized footer to any EmbedBuilder.
     * The footer contains gateway ping, shard info, and the requester's identity.
     * @param embedBuilder The EmbedBuilder to modify.
     * @param event The command event to extract information from.
     * @return The same EmbedBuilder instance for chaining.
     */
    public EmbedBuilder addDefaultFooter(EmbedBuilder embedBuilder, SlashCommandInteractionEvent event) {
        JDA.ShardInfo shardInfo = event.getJDA().getShardInfo();
        long gatewayPing = event.getJDA().getGatewayPing();
        String footerText = String.format("Ping: %dms | Shard: [%d/%d] | Requested by: %s",
                gatewayPing,
                shardInfo.getShardId() + 1, // Add 1 for human-readable format (e.g., 1/1 instead of 0/1)
                shardInfo.getShardTotal(),
                event.getUser().getName());

        embedBuilder.setFooter(footerText, event.getUser().getEffectiveAvatarUrl());
        return embedBuilder;
    }
}
