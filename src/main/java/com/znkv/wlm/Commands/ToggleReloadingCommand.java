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

public class ToggleReloadingCommand {
    //public class ReloadAndRestart{

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> toggleCommand = CommandManager.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.literal("toggle")
                        .executes(context -> {
                            WlmConfig.toggleReload();
                            return 0;
                        }));

        dispatcher.register(toggleCommand);

    }
}
