package com.znkv.wlm.Commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.znkv.wlm.Storage.WlmConfig;
import net.minecraft.command.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class DATokenCommand {
    //public class ReloadAndRestart{
    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> daTokenCommand = CommandManager.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.literal("datoken")
                        .then(CommandManager.argument("daToken",word())
                                .executes(context -> {
                                    WlmConfig.changeDAToken(getString(context, "daToken"));
                                    return 0;
                                })));

        dispatcher.register(daTokenCommand);
    }


}
