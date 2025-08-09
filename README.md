# Mycelium

A modular, scriptable Discord bot powered by JDA, GraalVM, and JavaScript.

## Description

Mycelium is a Discord bot that utilizes a Java-based core for stability and performance, while command functionality is provided by external JavaScript files. The core application is built with the Java Discord API (JDA) and runs on a GraalVM JDK to enable polyglot capabilities.

The primary design goal is to allow for the addition of new bot commands without recompiling the Java application. This is achieved by dynamically loading `.js` scripts from a `scripts/` directory at runtime. Each script can define one or more slash commands through a JSON metadata block, which the bot parses to register the commands with the Discord API.

## Requirements

-   **Java Development Kit:** GraalVM for JDK 21 or later is required. A standard OpenJDK is not sufficient due to the dependency on the GraalVM Polyglot API.
-   **Build System:** Apache Maven.
-   **Credentials:** A Discord bot token.

## Installation

### Method 1 - Build from source
1.  Clone the repository:
    ```
    $ git clone https://github.com/plavpixel/mycelium-bot.git
    $ cd mycelium-bot
    ```

2.  Provide the bot token. Create a file named `.env` in the project root with the following content:
    ```
    DISCORD_TOKEN="YourActualBotTokenHere"
    ```

3.  Build the executable JAR using Maven:
    ```
    $ mvn clean package
    ```
### Method 2 - Use prebuilt release
1. Download the latest prebuilt JAR file from the [Releases](https://github.com/plavpixel/mycelium-bot/releases) page.
Note that the packages in Releases will not always be up to date with the source code.
## Usage

Execute the packaged JAR file from the `target/` directory:
```
$ java -jar target/mycelium-bot-1.0-SNAPSHOT.jar
```

The bot will start, load all scripts from the `scripts/` directory, and register the defined commands with Discord.

## Scripting

Bot commands are defined in `.js` files located in the `scripts/` directory.

### Metadata Block

Each script file must contain a JSDoc-style comment block (`/** ... */`) at the top. This block must contain a single JSON array, where each object in the array defines a slash command.

**JSON Object Properties:**

-   `name` (string, required): The name of the slash command.
-   `description` (string, required): The command's description.
-   `handler` (string, required): The name of the JavaScript function within the file that will handle the command's execution.
-   `options` (array, optional): A list of option objects for the command.
-   `subcommands` (array, optional): A list of subcommand objects.

**Option Object Properties:**

-   `type` (string, required): The option type (e.g., `STRING`, `USER`, `INTEGER`).
-   `name` (string, required): The option's name.
-   `description` (string, required): The option's description.
-   `required` (boolean, required): Whether the option must be provided.

### Handler Function API

The JavaScript function specified in the `handler` property will be invoked with two arguments:

1.  `event`: The JDA `SlashCommandInteractionEvent` object. This provides full context for the interaction and is used to send responses via its hook (`event.getHook()`).
2.  `utils`: A helper object with methods for creating standardized `EmbedBuilder` instances (`createEmbed`, `createSuccessEmbed`, `createErrorEmbed`, `addDefaultFooter`).

### Example Script

File: `/scripts/utility.js`
```javascript
/**
[
  {
    "name": "ping",
    "description": "Checks the bot's gateway latency.",
    "handler": "handlePing"
  },
  {
    "name": "userinfo",
    "description": "Displays information about a user.",
    "handler": "handleUserInfo",
    "options": [
      { "type": "USER", "name": "user", "description": "The user to get info about.", "required": false }
    ]
  }
]
*/

function handlePing(event, utils) {
    const embed = utils.createEmbed('Ping', 'Pong!', utils.INFO_COLOR);
    utils.addDefaultFooter(embed, event);
    event.getHook().sendMessageEmbeds(embed.build()).queue();
}

function handleUserInfo(event, utils) {
    const user = event.getOption('user') ? event.getOption('user').getAsUser() : event.getUser();
    // ... implementation
    event.getHook().sendMessage(`Info for ${user.getName()}`).queue();
}
```
## License
This project is licensed under the MIT License.

The project's dependencies are distributed under their own respective licenses, including but not limited to: Apache-2.0 (JDA, Jackson), UPL-1.0 (GraalVM), EPL-1.0 & LGPL-2.1 (Logback), and MIT (java-dotenv).
