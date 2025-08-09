/**
 [
 {
 "name": "ban",
 "description": "Ban a user from the server.",
 "handler": "handleBan",
 "options": [
 { "type": "USER", "name": "user", "description": "The user to ban.", "required": true },
 { "type": "STRING", "name": "reason", "description": "The reason for the ban.", "required": false }
 ]
 },
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
 "name": "timeout",
 "description": "Timeout a user for a specified duration.",
 "handler": "handleTimeout",
 "options": [
 { "type": "USER", "name": "user", "description": "The user to timeout.", "required": true },
 { "type": "INTEGER", "name": "duration_minutes", "description": "How long the timeout should last in minutes.", "required": true },
 { "type": "STRING", "name": "reason", "description": "The reason for the timeout.", "required": false }
 ]
 }
 ]
 */

// Import necessary Java classes
const Permission = Java.type('net.dv8tion.jda.api.Permission');
const TimeUnit = Java.type('java.util.concurrent.TimeUnit');

// --- Utility Functions for this file ---
function sendError(event, utils, message) {
    const embed = utils.createErrorEmbed('Command Error', message);
    utils.addDefaultFooter(embed, event);
    event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
}

function sendSuccess(event, utils, message) {
    const embed = utils.createSuccessEmbed('Success', message);
    utils.addDefaultFooter(embed, event);
    event.getHook().sendMessageEmbeds(embed.build()).queue();
}


// --- Command Handlers ---

function handleBan(event, utils) {
    if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
        return sendError(event, utils, 'You lack the `BAN_MEMBERS` permission.');
    }
    const target = event.getOption('user').getAsMember();
    const reason = event.getOption('reason') ? event.getOption('reason').getAsString() : 'No reason provided.';

    if (!target) {
        return sendError(event, utils, 'Could not find that member in the server.');
    }
    if (!event.getMember().canInteract(target)) {
        return sendError(event, utils, 'You cannot ban this member. They may have a higher role than you.');
    }

    event.getGuild().ban(target, 0, TimeUnit.SECONDS).reason(reason).queue(
        () => sendSuccess(event, utils, `Successfully banned ${target.getUser().getName()}. Reason: ${reason}`),
        (error) => sendError(event, utils, `Failed to ban member: ${error.message}`)
    );
}

function handleKick(event, utils) {
    if (!event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
        return sendError(event, utils, 'You lack the `KICK_MEMBERS` permission.');
    }
    const target = event.getOption('user').getAsMember();
    const reason = event.getOption('reason') ? event.getOption('reason').getAsString() : 'No reason provided.';

    if (!target) {
        return sendError(event, utils, 'Could not find that member in the server.');
    }
    if (!event.getMember().canInteract(target)) {
        return sendError(event, utils, 'You cannot kick this member. They may have a higher role than you.');
    }

    event.getGuild().kick(target).reason(reason).queue(
        () => sendSuccess(event, utils, `Successfully kicked ${target.getUser().getName()}. Reason: ${reason}`),
        (error) => sendError(event, utils, `Failed to kick member: ${error.message}`)
    );
}

function handleTimeout(event, utils) {
    if (!event.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
        return sendError(event, utils, 'You lack the `MODERATE_MEMBERS` permission (for timeout).');
    }
    const target = event.getOption('user').getAsMember();
    const duration = event.getOption('duration_minutes').getAsLong();
    const reason = event.getOption('reason') ? event.getOption('reason').getAsString() : 'No reason provided.';

    if (!target) {
        return sendError(event, utils, 'Could not find that member in the server.');
    }
    if (!event.getMember().canInteract(target)) {
        return sendError(event, utils, 'You cannot timeout this member. They may have a higher role than you.');
    }

    target.timeoutFor(duration, TimeUnit.MINUTES).reason(reason).queue(
        () => sendSuccess(event, utils, `Successfully timed out ${target.getUser().getName()} for ${duration} minutes. Reason: ${reason}`),
        (error) => sendError(event, utils, `Failed to timeout member: ${error.message}`)
    );
}
