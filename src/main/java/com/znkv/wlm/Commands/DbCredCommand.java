package com.znkv.wlm.Commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.znkv.wlm.Storage.WlmConfig;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class DbCredCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> dbCredCommand = CommandManager.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.literal("dbcred")
                        .then(CommandManager.argument("url",word())
                                .then(CommandManager.argument("user",word())
                                        .then(CommandManager.argument("password", word())
                                .executes(context -> {
                                    WlmConfig.changeDbCred(getString(context, "url"), getString(context, "user"), getString(context, "password") );
                                    return 0;
                                })))));

        dispatcher.register(dbCredCommand);
    }
}
