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

import static com.znkv.wlm.Utils.WlmLogger.logInfo;


public class WhitelistStatusCommand {
    //public class ReloadAndRestart{

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> whitelistStatus = CommandManager.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.literal("status")
                        .executes(context -> {
                            logInfo("Whitelist db check: " + (WlmConfig.main.Should_I_Check_Whitelist ? "True" : "False"));
                            logInfo("Monthly payment: " + WlmConfig.main.Monthly_Payment_Rubbles);
                            logInfo("Whitelist reload time: " + WlmConfig.main.Whitelist_Reload_time + " ticks" );
                            logInfo("DB User: " + WlmConfig.main.Database_User);
                            logInfo("DB Pass: " + WlmConfig.main.Database_Password);
                            logInfo("DB Url: " + WlmConfig.main.Database_Url);
                            logInfo("DA Token: " + WlmConfig.main.DonationAlerts_Token);
                            return 0;
                        }));

        dispatcher.register(whitelistStatus);
    }

}
