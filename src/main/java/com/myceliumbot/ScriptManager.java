package com.myceliumbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptManager {
    private final DatabaseManager dbManager;
    private final HttpUtils httpUtils;
    private Scheduler scheduler;
    private final TimeUtils timeUtils;
    private Context context;
    private final Map<String, String> commandScripts = new HashMap<>();
    private final Map<String, List<String>> eventHandlers = new HashMap<>();
    private final File scriptsDirectory;
    private final BotConfig config;

    public ScriptManager(DatabaseManager dbManager, HttpUtils httpUtils) {
        this.dbManager = dbManager;
        this.httpUtils = httpUtils;
        this.timeUtils = new TimeUtils();
        this.config = BotConfig.getInstance();
        this.scriptsDirectory = new File(config.getScriptsDirectory());
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public List<SlashCommandData> loadScripts() {
        commandScripts.clear();
        eventHandlers.clear();
        initializeContext();

        List<SlashCommandData> foundCommands = new ArrayList<>();
        File[] files = scriptsDirectory.listFiles((dir, name) -> name.endsWith(".js"));
        if (files == null) {
            System.out.println("Could not find scripts directory: " + scriptsDirectory.getPath());
            return foundCommands;
        }

        System.out.println("Loading scripts and parsing metadata...");
        Pattern pattern = Pattern.compile("/\\*\\*([\\s\\S]*?)\\*/");

        for (File file : files) {
            String scriptName = file.getName();
            if (config.getDisabledScripts().contains(scriptName)) {
                System.out.println("Skipping disabled script: " + scriptName);
                continue;
            }

            try {
                String scriptContent = Files.readString(file.toPath());
                Matcher matcher = pattern.matcher(scriptContent);

                if (matcher.find()) {
                    String metadataBlock = matcher.group(1).trim();
                    foundCommands.addAll(parseMetadata(metadataBlock, scriptName));
                }

                context.eval(Source.newBuilder("js", scriptContent, scriptName).build());
            } catch (IOException | PolyglotException e) {
                System.err.println("Failed to load script: " + scriptName + " - " + e.getMessage());
                if (config.isDebugMode()) e.printStackTrace();
            }
        }
        return foundCommands;
    }

    private List<SlashCommandData> parseMetadata(String json, String scriptName) {
        List<SlashCommandData> commands = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Map<String, Object>> definitions = mapper.readValue(json, new TypeReference<>() {});
            for (Map<String, Object> def : definitions) {
                if (def.containsKey("name") && def.containsKey("handler")) {
                    String name = (String) def.get("name");
                    String description = (String) def.get("description");
                    SlashCommandData command = Commands.slash(name, description);

                    if (def.containsKey("options")) {
                        command.addOptions(parseOptions((List<Map<String, Object>>) def.get("options")));
                    }
                    if (def.containsKey("subcommands")) {
                        for (Map<String, Object> subMap : (List<Map<String, Object>>) def.get("subcommands")) {
                            command.addSubcommands(new SubcommandData((String) subMap.get("name"), (String) subMap.get("description")));
                        }
                    }
                    commands.add(command);
                    commandScripts.put(name, scriptName);
                } else if (def.containsKey("event") && def.containsKey("handler")) {
                    String eventType = ((String) def.get("event")).toUpperCase(Locale.ROOT);
                    eventHandlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add((String) def.get("handler"));
                }
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error parsing metadata in " + scriptName + ": " + e.getMessage());
        }
        System.out.printf(" + Parsed %d command(s) and %d event handler type(s) from '%s'.%n", commands.size(), eventHandlers.size(), scriptName);
        return commands;
    }

    private List<OptionData> parseOptions(List<Map<String, Object>> optionsList) {
        List<OptionData> options = new ArrayList<>();
        for (Map<String, Object> optMap : optionsList) {
            try {
                OptionType type = OptionType.valueOf(((String) optMap.get("type")).toUpperCase());
                String name = (String) optMap.get("name");
                String desc = (String) optMap.get("description");
                boolean required = (boolean) optMap.getOrDefault("required", false);
                options.add(new OptionData(type, name, desc, required));
            } catch (Exception e) {
                System.err.println("Failed to parse option: " + optMap.get("name"));
            }
        }
        return options;
    }

    // ... (The rest of your ScriptManager file remains the same)
    public void handleCommand(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        String scriptName = commandScripts.get(commandName);
        if (scriptName == null) {
            event.getHook().sendMessage("Command script not found for: " + commandName).setEphemeral(true).queue();
            return;
        }

        try {
            String handlerName = getHandlerForCommand(commandName);
            if (handlerName == null) {
                event.getHook().sendMessage("Handler name not found for command: " + commandName).setEphemeral(true).queue();
                return;
            }

            Value handler = context.getBindings("js").getMember(handlerName);
            if (handler == null || !handler.canExecute()) {
                event.getHook().sendMessage("Handler function missing or invalid in script: " + handlerName).setEphemeral(true).queue();
                return;
            }

            ScriptUtils utils = new ScriptUtils();
            try {
                // Try calling with all the new tools first (for new scripts)
                handler.execute(event, utils, dbManager, httpUtils, scheduler, timeUtils);
            } catch (PolyglotException e) {
                // If it's an argument count error, it's an old script. Fallback.
                if (e.getMessage().contains("Invalid number of arguments")) {
                    try {
                        // Call with the old, simple signature
                        handler.execute(event, utils);
                    } catch (Exception inner) {
                        event.getHook().sendMessage("Error executing command (fallback): " + inner.getMessage()).setEphemeral(true).queue();
                        if (config.isDebugMode()) inner.printStackTrace();
                    }
                } else {
                    // It's a different error, so re-throw it
                    throw e;
                }
            }
        } catch (Exception e) {
            event.getHook().sendMessage("Error executing command: " + e.getMessage()).setEphemeral(true).queue();
            if (config.isDebugMode()) e.printStackTrace();
        }
    }

    private String getHandlerForCommand(String commandName) {
        String scriptName = commandScripts.get(commandName);
        if (scriptName == null) return null;
        try {
            File scriptFile = new File(scriptsDirectory, scriptName);
            String scriptContent = Files.readString(scriptFile.toPath());
            Pattern pattern = Pattern.compile("/\\*\\*([\\s\\S]*?)\\*/");
            Matcher matcher = pattern.matcher(scriptContent);
            if (matcher.find()) {
                String metadataBlock = matcher.group(1).trim();
                JsonNode array = new ObjectMapper().readTree(metadataBlock);
                for (JsonNode node : array) {
                    if (node.has("name") && commandName.equals(node.get("name").asText())) {
                        return node.get("handler").asText();
                    }
                }
            }
        } catch (Exception e) {
            if (config.isDebugMode()) e.printStackTrace();
        }
        return null;
    }

    public boolean hasEventHandler(String eventType) {
        return eventHandlers.containsKey(eventType.toUpperCase(Locale.ROOT));
    }

    public void executeEventHandler(String eventType, GenericEvent event) {
        List<String> handlers = eventHandlers.get(eventType.toUpperCase(Locale.ROOT));
        if (handlers == null) return;
        // This part can be expanded with the same fallback logic as handleCommand if needed
        handlers.forEach(handlerName -> {
            try {
                context.getBindings("js").getMember(handlerName).execute(event, new ScriptUtils(), dbManager, httpUtils, scheduler, timeUtils);
            } catch (Exception e) {
                System.err.printf("Error in event handler %s: %s%n", handlerName, e.getMessage());
            }
        });
    }

    public void executeScheduledTask(String scriptFileName, String handlerName, JDA jda) {
        // This part can also be expanded with the same fallback logic
        try {
            context.getBindings("js").getMember(handlerName).execute(jda, new ScriptUtils(), dbManager, httpUtils, scheduler, timeUtils);
        } catch (Exception e) {
            System.err.printf("Error in scheduled task %s: %s%n", handlerName, e.getMessage());
        }
    }

    private void initializeContext() {
        context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(s -> true)
                .allowIO(IOAccess.ALL)
                .allowAllAccess(config.isEnableJsConsoleAccess())
                .option("js.ecmascript-version", "2022")
                .build();
    }
}