package com.znkv.wlm.Commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.znkv.wlm.DonationProcessor;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class AddUserCommand {
    //public class ReloadAndRestart{
    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> addUserCommand = CommandManager.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("user",word())
                                .then(CommandManager.argument("months", word())
                                        .executes(context -> {
                                            DonationProcessor.addPlayerFromCommand(getString(context, "user"), getString(context, "months"));
                                            return 0;
                                        }))));

        dispatcher.register(addUserCommand);
    }
}
