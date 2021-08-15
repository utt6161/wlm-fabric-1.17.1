package com.znkv.wlm.Commands;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.znkv.wlm.DonationProcessor;
import net.minecraft.command.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;


public class RestartCommand {
//public class ReloadAndRestart{

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> restartCommand = CommandManager.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.literal("restart")
                        .executes(context -> {
                            DonationProcessor.full_restart(context.getSource().getServer());
                            return 0;
                        }));

        dispatcher.register(restartCommand);
    }

}
