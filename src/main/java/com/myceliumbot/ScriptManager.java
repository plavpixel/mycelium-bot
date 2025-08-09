package com.myceliumbot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ScriptManager {

    // Inner record to hold information about where to find a command's logic
    private record ScriptExecutionInfo(File scriptFile, String handlerFunction) {}

    private final Map<String, ScriptExecutionInfo> commandHandlers = new HashMap<>();
    private final Context graalContext;
    private final ScriptUtils scriptUtils;

    public ScriptManager() {
        this.scriptUtils = new ScriptUtils();
        this.graalContext = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowIO(true)
                .build();
    }

    public void loadScripts() {
        File scriptDir = new File("scripts");
        if (!scriptDir.exists() || !scriptDir.isDirectory()) {
            System.err.println("Warning: 'scripts' directory not found. Creating it.");
            scriptDir.mkdirs();
            return;
        }

        for (File file : Objects.requireNonNull(scriptDir.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".js")) {
                try {
                    String content = new String(Files.readAllBytes(file.toPath()));
                    String metadataJson = extractMetadata(content);
                    if (metadataJson != null) {
                        parseAndMapHandlers(metadataJson, file);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading script file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    private void parseAndMapHandlers(String json, File scriptFile) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> commandList = mapper.readValue(json, new TypeReference<>() {});

            for (Map<String, Object> commandMap : commandList) {
                String commandName = (String) commandMap.get("name");
                String handlerName = (String) commandMap.get("handler");

                if (commandName != null && handlerName != null) {
                    commandHandlers.put(commandName, new ScriptExecutionInfo(scriptFile, handlerName));
                    System.out.println(" - Mapped command '/" + commandName + "' to handler '" + handlerName + "' in " + scriptFile.getName());
                } else {
                    System.err.println(" - WARNING: Command in " + scriptFile.getName() + " is missing 'name' or 'handler' property.");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse handler metadata for " + scriptFile.getName() + ": " + e.getMessage());
        }
    }

    private String extractMetadata(String scriptContent) {
        final String startTag = "/**";
        final String endTag = "*/";
        int startIndex = scriptContent.indexOf(startTag);
        int endIndex = scriptContent.indexOf(endTag, startIndex);
        return (startIndex != -1 && endIndex != -1) ? scriptContent.substring(startIndex + startTag.length(), endIndex).trim() : null;
    }

    public boolean hasScript(String commandName) {
        return commandHandlers.containsKey(commandName);
    }

    public void executeScript(String commandName, SlashCommandInteractionEvent event) {
        ScriptExecutionInfo info = commandHandlers.get(commandName);
        if (info == null) {
            event.getHook().sendMessage("Error: Could not find script execution info for command '" + commandName + "'.").queue();
            return;
        }

        try {
            String scriptContent = new String(Files.readAllBytes(info.scriptFile().toPath()));

            // A fresh context for each execution to ensure isolation
            try (Context isolatedContext = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).build()) {
                // Load the entire script file to define all functions
                isolatedContext.eval("js", scriptContent);

                // Get the specific handler function
                Value handler = isolatedContext.getBindings("js").getMember(info.handlerFunction());

                if (handler == null || !handler.canExecute()) {
                    String errorMsg = "Error: Handler function '" + info.handlerFunction() + "' not found or not executable in " + info.scriptFile().getName();
                    System.err.println(errorMsg);
                    event.getHook().sendMessage(errorMsg).queue();
                    return;
                }

                // Execute the function, passing our API objects as arguments
                handler.execute(event, this.scriptUtils);
            }
        } catch (Exception e) {
            System.err.println("An error occurred while executing script: " + info.scriptFile().getName());
            event.getHook().sendMessage("Error: An error occurred during script execution. Check console.").queue();
            e.printStackTrace();
        }
    }
}
