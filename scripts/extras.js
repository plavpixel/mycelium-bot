/**
 [
 {
 "name": "catfact",
 "description": "Gets a random cat fact from an external API.",
 "handler": "handleCatFact"
 },
 {
 "name": "remindme",
 "description": "Sets a reminder for the future.",
 "handler": "handleReminder",
 "options": [
 { "type": "STRING", "name": "duration", "description": "When to be reminded (e.g., 1h, 45m, 10s).", "required": true },
 { "type": "STRING", "name": "reminder", "description": "What to be reminded of.", "required": true }
 ]
 },
 {
 "name": "log",
 "description": "Manually logs a moderation action.",
 "handler": "handleLog"
 }
 ]
 */

// --- Handler Functions ---

// Note the new 'time' argument in all handlers
function handleCatFact(event, utils, db, http, scheduler, time) {
    const apiUrl = "https://catfact.ninja/fact";
    const response = http.get(apiUrl);

    if (response.startsWith("Error:")) {
        const errorEmbed = utils.createErrorEmbed("API Error", response);
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
        return;
    }

    const json = JSON.parse(response);
    const fact = json.fact;

    const embed = utils.createEmbed("🐱 Cat Fact", fact, utils.INFO_COLOR);
    utils.addDefaultFooter(embed, event);
    event.getHook().sendMessageEmbeds(embed.build()).queue();
}

function handleReminder(event, utils, db, http, scheduler, time) {
    const durationStr = event.getOption('duration').getAsString();
    const durationSeconds = time.parseDuration(durationStr); // Use the new Java utility

    if (durationSeconds <= 0) {
        const errorEmbed = utils.createErrorEmbed("Invalid Duration", "Please provide a valid duration (e.g., 1h, 45m, 10s).");
        event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
        return;
    }

    const reminderText = event.getOption('reminder').getAsString();
    const targetUser = event.getUser();
    const channelId = event.getChannel().getId();

    const scriptFileName = "extras.js";
    const handlerName = "executeReminder";

    const payload = JSON.stringify({ text: reminderText, channelId: channelId });

    db.execute(
        "INSERT INTO mod_logs (guild_id, moderator_id, target_id, action, reason) VALUES (?, ?, ?, ?, ?)",
        event.getGuild().getId(), targetUser.getId(), "REMINDER_TASK", handlerName, payload
    );

    scheduler.scheduleOnce(scriptFileName, handlerName, durationSeconds, "SECONDS");

    const formattedDuration = time.formatDuration(durationSeconds);
    const successEmbed = utils.createSuccessEmbed("Reminder Set!", `Okay, I will remind you about "${reminderText}" in ${formattedDuration}.`);
    event.getHook().sendMessageEmbeds(successEmbed.build()).setEphemeral(true).queue();
}

function executeReminder(jda, utils, db, http, scheduler, time) {
    const resultJson = db.query("SELECT * FROM mod_logs WHERE target_id = 'REMINDER_TASK' ORDER BY timestamp DESC LIMIT 1");
    const results = JSON.parse(resultJson);

    if (results.length > 0) {
        const reminderData = results[0];
        const userId = reminderData.moderator_id;
        const guildId = reminderData.guild_id;
        const payload = JSON.parse(reminderData.reason);
        const reminderText = payload.text;
        const channelId = payload.channelId;

        jda.retrieveUserById(userId).queue(user => {
            if (user) {
                const embed = utils.createEmbed("⏰ Reminder!", reminderText, utils.INFO_COLOR);
                user.openPrivateChannel().queue(
                    (dmChannel) => dmChannel.sendMessageEmbeds(embed.build()).queue(),
                    (error) => {
                        console.log(`Failed to DM user ${userId}. Sending reminder in original channel.`);
                        const guild = jda.getGuildById(guildId);
                        if (guild) {
                            const channel = guild.getTextChannelById(channelId);
                            if (channel) {
                                channel.sendMessage(`${user.getAsMention()}, I couldn't DM you, so here is your reminder:`).addEmbeds(embed.build()).queue();
                            }
                        }
                    }
                );
            }
        });

        db.execute("DELETE FROM mod_logs WHERE id = ?", reminderData.id);
    }
}

function handleLog(event, utils, db, http, scheduler, time) {
    db.execute(
        "INSERT INTO mod_logs (guild_id, moderator_id, target_id, action, reason) VALUES (?, ?, ?, ?, ?)",
        event.getGuild().getId(), event.getMember().getId(), "MANUAL_LOG_TARGET", "MANUAL_LOG", "This is a test log entry."
    );
    const embed = utils.createSuccessEmbed("Logged!", "A manual entry was added to the database.");
    event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
}
