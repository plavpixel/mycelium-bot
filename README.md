# Mycelium

A modular, scriptable, open-source Discord bot powered by JDA, GraalVM, and JavaScript.

## Description

Mycelium is a Discord bot that utilizes a Java-based core for stability and performance, while command functionality is provided by external JavaScript files. The core application is built with the Java Discord API (JDA) and runs on a GraalVM JDK to enable polyglot capabilities.

The primary design goal is to allow for the addition of new bot commands without recompiling the Java application. This is achieved by dynamically loading `.js` scripts from a `scripts/` directory at runtime. Each script can define one or more slash commands through a JSON metadata block, which the bot parses to register the commands with the Discord API.

## Requirements

-   **Java Development Kit:** [GraalVM](https://www.graalvm.org/) JDK 21 or later is required. A standard OpenJDK or equivalent is not sufficient due to the dependency on the GraalVM Polyglot API.
-   **Build System:** Apache Maven.
-   **Credentials:** A Discord bot token.

## Getting Started

1.  **Clone the repository:**
    ```sh
    $ git clone https://github.com/plavpixel/mycelium-bot.git
    $ cd mycelium-bot
    ```

2.  **Build the project:**
    Open the project in your IDE and let it resolve Maven dependencies, or build from the command line:
    ```sh
    $ mvn clean install
    ```

3.  **First Run (Configuration Generation):**
    Run the `Main.java` class from your IDE or execute the packaged JAR file. This first run is expected to fail to log in, but it will automatically create two essential files:
    - `config.json`: The main configuration file for the bot.
    - `.env`: A file to store your secret bot token.

4.  **Configure the Bot:**
    - **Add Token:** Open the `.env` file and add your Discord bot token:
      ```env
      DISCORD_TOKEN="YourActualBotTokenHere"
      ```
    - **Set Owner:** Open `config.json`, find the `ownerIds` array, and add your Discord User ID(s). <br> This can be used for scripts that require owner-only access or special privileges.
      ```json
        "ownerIds": [ 123456789012345678, 112233445566778899, ... ]
      ```
      > **Tip:** To get your Discord User ID, enable Developer Mode in Discord settings (under Advanced), then right-click your username and select "Copy User ID".

5.  **Run the Bot Again:**
    Execute the `Main.java` class or the JAR file again. The bot will now start, log in, and be ready for use.

## Configuration

Mycelium uses a `config.json` file in the root directory for customization. If this file doesn't exist when the bot starts, it will be automatically created with default values.

### Example Configuration

```json
{
  "botName": "Mycelium",
  "activityType": "WATCHING",
  "activityText": "the network grow",
  "embedColor": "#5865F2",
  "errorColor": "#ED4245",
  "successColor": "#57F287",
  "commandCooldownSeconds": 3,
  "allowDMCommands": false,
  "ownerIds": [123456789012345678],
  "mentionRepliesEnabled": true,

  "scriptsDirectory": "./scripts",
  "databasePath": "./data/bot.db",
  "logsDirectory": "./logs",

  "debugMode": false,
  "logCommands": true,
  "logLevel": "INFO",

  "disabledScripts": ["example-disabled.js"],
  "enableScriptHotReload": false,
  "enableJsConsoleAccess": false,

  "httpTimeoutSeconds": 30,
  "allowUnsafeConnections": false,

  "globalRateLimitPerMinute": 100,
  "enablePerUserRateLimit": true,
  "perUserRateLimitPerMinute": 10
}
```

### Configuration Options

| Option | Type | Description |
|---|---|---|
| **Bot Appearance** | | |
| `botName` | String | Name of the bot used in logging and some embeds |
| `activityType` | String | Activity type shown in Discord (PLAYING, WATCHING, LISTENING, COMPETING, STREAMING) |
| `activityText` | String | Text displayed in the bot's status |
| `embedColor` | String | Default color for embeds (Hex code) |
| `errorColor` | String | Color for error embeds (Hex code) |
| `successColor` | String | Color for success embeds (Hex code) |
| **Bot Behavior** | | |
| `commandCooldownSeconds` | Integer | Global cooldown between commands (in seconds) |
| `allowDMCommands` | Boolean | Whether commands can be used in DMs |
| `ownerIds` | Array | List of Discord user IDs with owner privileges |
| `mentionRepliesEnabled` | Boolean | Whether to include a mention in command replies |
| **Paths** | | |
| `scriptsDirectory` | String | Directory containing JavaScript script files |
| `databasePath` | String | Path to the SQLite database file |
| `logsDirectory` | String | Directory for log files |
| **Logging & Debugging** | | |
| `debugMode` | Boolean | Enables additional debug information |
| `logCommands` | Boolean | Whether to log command usage |
| `logLevel` | String | Log level (DEBUG, INFO, WARN, ERROR) |
| `disabledScripts` | Array | List of script filenames to disable |
| `enableScriptHotReload` | Boolean | Whether scripts can be reloaded without restarting (not recommended) |
| `enableJsConsoleAccess` | Boolean | Whether scripts can access console features (security risk) |
| **HTTP & Rate Limiting** | | |
| `httpTimeoutSeconds` | Integer | Timeout for HTTP requests in seconds |
| `allowUnsafeConnections` | Boolean | Allow unsafe HTTPS connections (not recommended) |
| `globalRateLimitPerMinute` | Integer | Maximum commands processed per minute across all users |
| `enablePerUserRateLimit` | Boolean | Enable per-user rate limiting |
| `perUserRateLimitPerMinute` | Integer | Maximum commands per minute per user |

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

The JavaScript function specified in the `handler` property will be invoked with a collection of powerful tools. Your function can accept any of these arguments.

1.  `event`: The JDA `SlashCommandInteractionEvent` object. This provides full context for the interaction and is used to send responses.
2.  `utils`: A helper object with methods for creating standardized `EmbedBuilder` instances.
3.  `dbManager`: An instance of the `DatabaseManager` for executing SQL queries.
4.  `httpUtils`: An instance of `HttpUtils` for making HTTP requests.
5.  `scheduler`: An instance of the `Scheduler` for creating and canceling timed tasks.
6.  `timeUtils`: A helper object for parsing and formatting time durations.

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
    const latency = event.getJDA().getGatewayPing();
    const embed = utils.createEmbed('Ping', `Gateway Latency: ${latency}ms`, utils.INFO_COLOR);
    utils.addDefaultFooter(embed, event);
    event.replyEmbeds(embed.build()).queue();
}

// This handler demonstrates accessing a user option and accepting the new dbManager utility.
function handleUserInfo(event, utils, dbManager) {
    const user = event.getOption('user') ? event.getOption('user').getAsUser() : event.getUser();

    // You could use the dbManager here to store or retrieve stats about the user.
    // For now, we'll just display info.
    const embed = utils.createEmbed(`Info for ${user.getName()}`)
        .setThumbnail(user.getEffectiveAvatarUrl())
        .addField("User ID", user.getId(), true)
        .addField("Account Created", `<t:${user.getTimeCreated().toEpochSecond()}:R>`, true);

    event.replyEmbeds(embed.build()).queue();
}
```
## License
This project is licensed under the MIT License.

Its dependencies are distributed under their own respective licenses, including but not limited to: Apache-2.0 (JDA, Jackson, SQLite-JDBC), UPL-1.0 (GraalVM), EPL-1.0 & LGPL-2.1 (Logback), and MIT (java-dotenv, SLF4J).