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

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

public class UserAndPasswordCommand  {
    //public class ReloadAndRestart{

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> userAndPasswordCommand = CommandManager.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.literal("lognpass")
                        .then(CommandManager.argument("user",word())
                                .then(CommandManager.argument("password", word())
                                        .executes(context -> {
                                            WlmConfig.changeUserAndPassword(getString(context, "user"),getString(context,"password"));
                                            return 0;
                                        }))));

        dispatcher.register(userAndPasswordCommand);
    }

}