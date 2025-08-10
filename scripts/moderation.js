/**
 [
 {
 "name": "kick",
 "description": "Kick a user from the server.",
 "handler": "handleKick",
 "options": [
 { "type": "USER", "name": "user", "description": "The user to kick.", "required": true },
 { "type": "STRING", "name": "reason", "description": "The reason for the kick.", "required": false }
 ]
 },
 {
 "name": "ban",
 "description": "Permanently ban a user from the server by mention or ID.",
 "handler": "handleBan",
 "options": [
 { "type": "USER", "name": "user", "description": "The user to ban.", "required": true },
 { "type": "STRING", "name": "reason", "description": "The reason for the ban.", "required": false }
 ]
 },
 {
 "name": "tempban",
 "description": "Temporarily ban a user by mention or ID.",
 "handler": "handleTempBan",
 "options": [
 { "type": "USER", "name": "user", "description": "The user to ban.", "required": true },
 { "type": "STRING", "name": "duration", "description": "How long the ban should last (e.g., 7d, 12h, 30m).", "required": true },
 { "type": "STRING", "name": "reason", "description": "The reason for the temporary ban.", "required": false }
 ]
 },
 {
 "name": "unban",
 "description": "Unban a user by mention or ID.",
 "handler": "handleUnban",
 "options": [
 { "type": "USER", "name": "user", "description": "The user to unban.", "required": true },
 { "type": "STRING", "name": "reason", "description": "The reason for the unban.", "required": false }
 ]
 },
 {
 "name": "timeout",
 "description": "Timeout a user for a specified duration.",
 "handler": "handleTimeout",
 "options": [
 { "type": "USER", "name": "user", "description": "The user to timeout.", "required": true },
 { "type": "STRING", "name": "duration", "description": "The duration of the timeout (e.g., 10m, 1h, 2d).", "required": true },
 { "type": "STRING", "name": "reason", "description": "The reason for the timeout.", "required": false }
 ]
 },
 {
 "name": "untimeout",
 "description": "Remove a timeout from a user.",
 "handler": "handleRemoveTimeout",
 "options": [
 { "type": "USER", "name": "user", "description": "The user to remove the timeout from.", "required": true },
 { "type": "STRING", "name": "reason", "description": "The reason for removing the timeout.", "required": false }
 ]
 }
 ]
 */

const Permission = Java.type('net.dv8tion.jda.api.Permission');
const TimeUnit = Java.type('java.util.concurrent.TimeUnit');

// --- Command Handlers ---

function handleKick(event, utils, db, http, scheduler, time) {
    if (!event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
        return event.getHook().sendMessageEmbeds(utils.createErrorEmbed("Permission Denied", "You do not have the `KICK_MEMBERS` permission.").build()).setEphemeral(true).queue();
    }
    const target = event.getOption('user').getAsMember();
    const reason = event.getOption('reason') ? event.getOption('reason').getAsString() : 'No reason provided.';

    if (!target) {
        return event.getHook().sendMessageEmbeds(utils.createErrorEmbed("User Not Found", "The specified user is not a member of this server.").build()).setEphemeral(true).queue();
    }

    event.getGuild().kick(target).reason(reason).queue(() => {
        const embed = utils.createSuccessEmbed("User Kicked", `${target.getAsMention()} has been kicked.`);
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }, (error) => {
        const embed = utils.createErrorEmbed("Kick Failed", `Could not kick the user. Error: ${error.message}`);
        event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
    });
}

function handleBan(event, utils, db, http, scheduler, time) {
    if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
        return event.getHook().sendMessageEmbeds(utils.createErrorEmbed("Permission Denied", "You do not have the `BAN_MEMBERS` permission.").build()).setEphemeral(true).queue();
    }
    const targetUser = event.getOption('user').getAsUser(); // Get as User, not Member
    const reason = event.getOption('reason') ? event.getOption('reason').getAsString() : 'No reason provided.';

    event.getGuild().ban(targetUser, 0, TimeUnit.SECONDS).reason(reason).queue(() => {
        const embed = utils.createSuccessEmbed("User Banned", `**${targetUser.getName()}** (${targetUser.getId()}) has been permanently banned.`);
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }, (error) => {
        const embed = utils.createErrorEmbed("Ban Failed", `Could not ban the user. Error: ${error.message}`);
        event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
    });
}

function handleTempBan(event, utils, db, http, scheduler, time) {
    if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
        return event.getHook().sendMessageEmbeds(utils.createErrorEmbed("Permission Denied", "You do not have the `BAN_MEMBERS` permission.").build()).setEphemeral(true).queue();
    }

    const targetUser = event.getOption('user').getAsUser(); // Get as User
    const durationStr = event.getOption('duration').getAsString();
    const durationSeconds = time.parseDuration(durationStr);
    const reason = event.getOption('reason') ? event.getOption('reason').getAsString() : 'No reason provided.';

    if (durationSeconds <= 0) {
        return event.getHook().sendMessageEmbeds(utils.createErrorEmbed("Invalid Duration", "Please provide a valid duration (e.g., 7d, 12h, 30m).").build()).setEphemeral(true).queue();
    }

    event.getGuild().ban(targetUser, 0, TimeUnit.SECONDS).reason(`Temp-ban: ${reason}`).queue(() => {
        const scriptFileName = "moderation.js";
        const handlerName = "executeUnban";
        const payload = JSON.stringify({ userId: targetUser.getId(), guildId: event.getGuild().getId() });

        db.execute(
            "INSERT INTO mod_logs (guild_id, moderator_id, target_id, action, reason) VALUES (?, ?, ?, ?, ?)",
            event.getGuild().getId(), event.getMember().getId(), targetUser.getId(), "SCHEDULED_UNBAN", payload
        );

        scheduler.scheduleOnce(scriptFileName, handlerName, durationSeconds, "SECONDS");

        const formattedDuration = time.formatDuration(durationSeconds);
        const embed = utils.createSuccessEmbed("User Banned Temporarily", `**${targetUser.getName()}** (${targetUser.getId()}) has been banned for ${formattedDuration}.`);
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }, (error) => {
        const embed = utils.createErrorEmbed("Ban Failed", `Could not ban the user. Error: ${error.message}`);
        event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
    });
}

function handleUnban(event, utils, db, http, scheduler, time) {
    if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
        return event.getHook().sendMessageEmbeds(utils.createErrorEmbed("Permission Denied", "You do not have the `BAN_MEMBERS` permission.").build()).setEphemeral(true).queue();
    }

    const targetUser = event.getOption('user').getAsUser();
    const reason = event.getOption('reason') ? event.getOption('reason').getAsString() : 'Manual unban.';

    db.execute("DELETE FROM mod_logs WHERE action = 'SCHEDULED_UNBAN' AND target_id = ?", targetUser.getId());

    event.getGuild().unban(targetUser).reason(reason).queue(() => {
        const embed = utils.createSuccessEmbed("User Unbanned", `**${targetUser.getName()}** (${targetUser.getId()}) has been unbanned.`);
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }, (error) => {
        const embed = utils.createErrorEmbed("Unban Failed", `Could not unban the user. Are they currently banned? Error: ${error.message}`);
        event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
    });
}

function handleTimeout(event, utils, db, http, scheduler, time) {
    if (!event.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
        return event.getHook().sendMessageEmbeds(utils.createErrorEmbed("Permission Denied", "You do not have the `MODERATE_MEMBERS` permission.").build()).setEphemeral(true).queue();
    }
    const target = event.getOption('user').getAsMember();
    const durationStr = event.getOption('duration').getAsString();
    const durationSeconds = time.parseDuration(durationStr);
    const reason = event.getOption('reason') ? event.getOption('reason').getAsString() : 'No reason provided.';

    if (!target) {
        return event.getHook().sendMessageEmbeds(utils.createErrorEmbed("User Not Found", "The specified user is not a member of this server.").build()).setEphemeral(true).queue();
    }

    if (durationSeconds <= 0) {
        return event.getHook().sendMessageEmbeds(utils.createErrorEmbed("Invalid Duration", "Please provide a valid duration (e.g., 10m, 1h, 2d).").build()).setEphemeral(true).queue();
    }

    target.timeoutFor(durationSeconds, TimeUnit.SECONDS).reason(reason).queue(() => {
        const formattedDuration = time.formatDuration(durationSeconds);
        const embed = utils.createSuccessEmbed("User Timed Out", `${target.getAsMention()} has been timed out for ${formattedDuration}.`);
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    });
}

function handleRemoveTimeout(event, utils, db, http, scheduler, time) {
    if (!event.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
        return event.getHook().sendMessageEmbeds(utils.createErrorEmbed("Permission Denied", "You do not have the `MODERATE_MEMBERS` permission.").build()).setEphemeral(true).queue();
    }
    const target = event.getOption('user').getAsMember();
    const reason = event.getOption('reason') ? event.getOption('reason').getAsString() : 'Manual timeout removal.';

    if (!target) {
        return event.getHook().sendMessageEmbeds(utils.createErrorEmbed("User Not Found", "The specified user is not a member of this server.").build()).setEphemeral(true).queue();
    }

    target.removeTimeout().reason(reason).queue(() => {
        const embed = utils.createSuccessEmbed("Timeout Removed", `The timeout has been removed for ${target.getAsMention()}.`);
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    });
}

// This function is called by the scheduler for tempbans
function executeUnban(jda, utils, db, http, scheduler, time) {
    const resultJson = db.query("SELECT * FROM mod_logs WHERE action = 'SCHEDULED_UNBAN' ORDER BY timestamp DESC LIMIT 1");
    const results = JSON.parse(resultJson);

    if (results.length > 0) {
        const unbanData = results[0];
        const payload = JSON.parse(unbanData.reason);
        const guild = jda.getGuildById(payload.guildId);
        if (guild) {
            // JDA's unban method can take a User object or just an ID string.
            // To be safe, we'll create a UserSnowflake object.
            const User = Java.type('net.dv8tion.jda.api.entities.User');
            const userToUnban = User.fromId(payload.userId);
            guild.unban(userToUnban).reason("Temporary ban expired.").queue(
                () => console.log(`Automatically unbanned user ${payload.userId} from guild ${payload.guildId}.`),
                (error) => console.error(`Failed to auto-unban user ${payload.userId}: ${error.message}`)
            );
        }
        db.execute("DELETE FROM mod_logs WHERE id = ?", unbanData.id);
    }
}
