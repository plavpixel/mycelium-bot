/**
 [
 {
 "event": "MEMBER_JOIN",
 "handler": "handleMemberJoin"
 },
 {
 "event": "MESSAGE_RECEIVED",
 "handler": "handleMessage"
 }
 ]
 */

// event: JDA GuildMemberJoinEvent object
// utils, db, http, scheduler: The standard utility objects
function handleMemberJoin(event, utils, db, http, scheduler) {
    const member = event.getMember();
    const guild = event.getGuild();
    // Find a channel named "general" or "welcome" to send the message
    const welcomeChannel = guild.getTextChannelsByName("general", true).get(0) || guild.getTextChannelsByName("welcome", true).get(0);

    if (welcomeChannel) {
        const embed = utils.createEmbed("Welcome!", `Please welcome ${member.getAsMention()} to ${guild.getName()}!`, utils.SUCCESS_COLOR);
        embed.setThumbnail(member.getUser().getEffectiveAvatarUrl());
        welcomeChannel.sendMessageEmbeds(embed.build()).queue();
    }

    // Log the join to the database
    db.execute(
        "INSERT INTO mod_logs (guild_id, moderator_id, target_id, action, reason) VALUES (?, ?, ?, ?, ?)",
        guild.getId(),
        "SYSTEM", // Moderator is the system itself
        member.getId(),
        "MEMBER_JOIN",
        "User joined the server."
    );
}

// event: JDA MessageReceivedEvent object
function handleMessage(event, utils, db) {
    const message = event.getMessage().getContentRaw();
    if (message.toLowerCase() === "hello mycelium") {
        event.getChannel().sendMessage(`Hello, ${event.getAuthor().getAsMention()}!`).queue();
    }
}
