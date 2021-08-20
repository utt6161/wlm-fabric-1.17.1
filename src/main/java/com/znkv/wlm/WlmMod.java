package com.znkv.wlm;

import com.znkv.wlm.Commands.*;
import com.znkv.wlm.Storage.WlmConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import static com.znkv.wlm.Wlm.THREADPOOL;

public class WlmMod implements ModInitializer {

	private static int tick = 0;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		Wlm.init(FabricLoader.getInstance().getGameDir());

		ServerLifecycleEvents.SERVER_STARTED.register((server -> {
			Wlm.start(server);
		}));

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			AddUserCommand.registerCommand(dispatcher);
			DATokenCommand.registerCommand(dispatcher);
			DbCredCommand.registerCommand(dispatcher);
			DeleteUserCommand.registerCommand(dispatcher);
			infoCommand.registerCommand(dispatcher);
			MonthlyPaymentCommand.registerCommand(dispatcher);
			PasswordCommand.registerCommand(dispatcher);
			ReloadCommand.registerCommand(dispatcher);
			ReloadTimeCommand.registerCommand(dispatcher);
			RestartCommand.registerCommand(dispatcher);
			ToggleReloadingCommand.registerCommand(dispatcher);
			UrlCommand.registerCommand(dispatcher);
			UserAndPasswordCommand.registerCommand(dispatcher);
			UserCommand.registerCommand(dispatcher);
			WhitelistStatusCommand.registerCommand(dispatcher);
		});

		ServerTickEvents.START_SERVER_TICK.register((server)->{
			if(Wlm.config.currentInstance.main.Should_I_Check_Whitelist){
				tick++;
			} else {
				tick = 0;
				return;
			}
			if(tick >= Wlm.config.currentInstance.main.Whitelist_Reload_time){
				THREADPOOL.submit(()->{
					DonationProcessor.run();
				});
				tick = 0;
			}
		});
	}
}
