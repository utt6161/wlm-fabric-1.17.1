package com.znkv.wlm.Commands;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static com.znkv.wlm.Utils.WlmLogger.logInfo;

public class infoCommand {
    //public class ReloadAndRestart{

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> whitelistStatus = CommandManager.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.literal("info")
                        .executes(context -> {
                            logInfo("ALL COMMANDS: ");
                            logInfo("'/wl restart' - full mod restart");
                            logInfo("'/wl reload' - get new data from config");
                            logInfo("'/wl toggle' - off/on whitelist timer");
                            logInfo("'/wl add <username> <months>' - add new player, if months are negative, then i'll try to subtract them from existing player");
                            logInfo("'/wl delete <username>' - remove player from database and whitelist");
                            logInfo("'/wl url <database_url>' - change config field 'url'");
                            logInfo("'/wl user <database_user>' - change config field 'user'");
                            logInfo("'/wl password <database_user_password>' - change config field 'password'");
                            logInfo("'/wl lognpass <user> <pass>' - change pair of user and password");
                            logInfo("'/wl dbcred <url> <user> <pass>' - change all data for database");
                            logInfo("'/wl money <money>' - change config field 'money'");
                            logInfo("'/wl reloadtime <time_in_ticks>' - change config field 'whitelistreloadtime'");
                            logInfo("'/wl datoken <da_token>' - change config field 'token'");
                            logInfo("'/wl status' - check if mod checks database every 'reload time' ticks");
                            logInfo("'/wl info' - the command that you just used :^)");

                            return 0;
                        }));

        dispatcher.register(whitelistStatus);
    }


}