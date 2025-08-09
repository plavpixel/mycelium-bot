package com.myceliumbot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CommandRegistry {

    public static void registerCommands(JDA jda) {
        System.out.println("Scanning for script metadata and registering commands...");
        File scriptDir = new File("scripts");
        List<SlashCommandData> commandsToRegister = new ArrayList<>();

        for (File file : Objects.requireNonNull(scriptDir.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".js")) {
                try {
                    String content = new String(Files.readAllBytes(file.toPath()));
                    String metadataJson = extractMetadata(content);

                    if (metadataJson != null) {
                        // The metadata is now a JSON array of commands
                        List<SlashCommandData> commandsFromFile = parseAndBuildCommands(metadataJson);
                        commandsToRegister.addAll(commandsFromFile);
                        System.out.println(" + Prepared " + commandsFromFile.size() + " command(s) from '" + file.getName() + "'.");
                    }
                } catch (IOException e) {
                    System.err.println("Error reading script file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }

        if (!commandsToRegister.isEmpty()) {
            CommandListUpdateAction updateAction = jda.updateCommands();
            updateAction.addCommands(commandsToRegister).queue(
                    (success) -> System.out.println("Successfully registered/updated " + success.size() + " commands with Discord!"),
                    (error) -> System.err.println("Failed to register commands with Discord: " + error.getMessage())
            );
        } else {
            System.out.println("No commands found to register.");
        }
    }

    private static String extractMetadata(String scriptContent) {
        final String startTag = "/**";
        final String endTag = "*/";
        int startIndex = scriptContent.indexOf(startTag);
        int endIndex = scriptContent.indexOf(endTag, startIndex);

        if (startIndex != -1 && endIndex != -1) {
            return scriptContent.substring(startIndex + startTag.length(), endIndex).trim();
        }
        return null;
    }

    private static List<SlashCommandData> parseAndBuildCommands(String json) {
        List<SlashCommandData> commandDataList = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            // We now expect a List of command definitions
            List<Map<String, Object>> commandList = mapper.readValue(json, new TypeReference<>() {});

            for (Map<String, Object> commandMap : commandList) {
                String name = (String) commandMap.get("name");
                String description = (String) commandMap.get("description");
                SlashCommandData command = Commands.slash(name, description);

                if (commandMap.containsKey("options")) {
                    command.addOptions(parseOptions((List<Map<String, Object>>) commandMap.get("options")));
                }

                if (commandMap.containsKey("subcommands")) {
                    List<Map<String, Object>> subcommandsList = (List<Map<String, Object>>) commandMap.get("subcommands");
                    for (Map<String, Object> subMap : subcommandsList) {
                        String subName = (String) subMap.get("name");
                        String subDesc = (String) subMap.get("description");
                        SubcommandData subcommand = new SubcommandData(subName, subDesc);
                        if (subMap.containsKey("options")) {
                            subcommand.addOptions(parseOptions((List<Map<String, Object>>) subMap.get("options")));
                        }
                        command.addSubcommands(subcommand);
                    }
                }
                commandDataList.add(command);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse command metadata JSON array: " + e.getMessage());
            e.printStackTrace();
        }
        return commandDataList;
    }

    private static List<OptionData> parseOptions(List<Map<String, Object>> optionsList) {
        List<OptionData> options = new ArrayList<>();
        for (Map<String, Object> optMap : optionsList) {
            OptionType type = OptionType.valueOf(((String) optMap.get("type")).toUpperCase());
            String optName = (String) optMap.get("name");
            String optDesc = (String) optMap.get("description");
            boolean required = (boolean) optMap.getOrDefault("required", false);
            options.add(new OptionData(type, optName, optDesc, required));
        }
        return options;
    }
}
