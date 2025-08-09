/**
 [
 {
 "name": "ping",
 "description": "A simple ping-pong command to check bot latency.",
 "handler": "handlePing"
 },
 {
 "name": "userinfo",
 "description": "Displays information about a user.",
 "handler": "handleUserInfo",
 "options": [
 { "type": "USER", "name": "user", "description": "The user to get info about (defaults to you).", "required": false }
 ]
 }
 ]
 */

// Handler for the /ping command
function handlePing(event, utils) {
    const description = `Pong! Replying with the bot's current gateway latency.`;
    const embed = utils.createEmbed('Ping', description, utils.INFO_COLOR);
    utils.addDefaultFooter(embed, event);
    event.getHook().sendMessageEmbeds(embed.build()).queue();
}

// Handler for the /userinfo command
function handleUserInfo(event, utils) {
    const targetUser = event.getOption('user') ? event.getOption('user').getAsUser() : event.getUser();
    const targetMember = event.getOption('user') ? event.getOption('user').getAsMember() : event.getMember();

    const embed = utils.createEmbed('User Information', `Profile for ${targetUser.getAsMention()}`, utils.INFO_COLOR);
    embed.setThumbnail(targetUser.getEffectiveAvatarUrl());
    embed.addField('Username', targetUser.getName(), true);
    embed.addField('User ID', targetUser.getId(), true);
    embed.addField('Account Created', `<t:${targetUser.getTimeCreated().toEpochSecond()}:R>`, true);

    if (targetMember) {
        embed.addField('Joined Server', `<t:${targetMember.getTimeJoined().toEpochSecond()}:R>`, true);
        const roles = targetMember.getRoles().map(role => role.getAsMention()).join(', ');
        embed.addField('Roles', roles.length > 0 ? roles : 'None', false);
    }

    utils.addDefaultFooter(embed, event);
    event.getHook().sendMessageEmbeds(embed.build()).queue();
}
