package com.znkv.wlm.Commands;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.znkv.wlm.Storage.WlmConfig;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

public class MonthlyPaymentCommand {
    //public class ReloadAndRestart{
    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> monthlyPayment = CommandManager.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.literal("money")
                        .then(CommandManager.argument("monthlyPayment",word())
                                .executes(context -> {
                                    try {
                                        WlmConfig.changeMonthlyPayment(getString(context, "monthlyPayment"));
                                    } catch(NumberFormatException e){
                                        throw new CommandException(new LiteralText("Monthly payment doesnt seem to be a number"));
                                    }
                                    return 0;
                                })));

        dispatcher.register(monthlyPayment);
    }

}
