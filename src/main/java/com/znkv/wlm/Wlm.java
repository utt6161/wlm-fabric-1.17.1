package com.znkv.wlm;

import com.znkv.wlm.Storage.WlmConfig;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import net.minecraft.server.MinecraftServer;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.znkv.wlm.Utils.WlmLogger.logInfo;
import static com.znkv.wlm.Utils.WlmLogger.logError;

public class Wlm {

    //public static final ExecutorService THREADPOOL = Executors.newCachedThreadPool();

    public static WlmConfig config;

    public static Path gameDirectory;

    private final String da_url = "https://socket.donationalerts.ru:443";

    public static void init(Path gameDir) {
        gameDirectory = gameDir;
        logInfo("Whitelist Modifier is here");
        logInfo("Time to pay up, bud~~~");

        // Creating data directory (database and config files are stored there)
        // Loading config
        config = WlmConfig.load(new File(gameDirectory + "/config/WlmConfig.json"));
    }

    public static void start(MinecraftServer server){
        DonationProcessor DP = new DonationProcessor();
        DP.StartSyncing(server, config);
    }


    /**
     * Called on server stop.
     */
    public static void stop() {
        logInfo("Shutting down Whitelist Modifier.");
//        // Closing threads
//        try {
//            THREADPOOL.shutdownNow();
//            if (!THREADPOOL.awaitTermination(500, TimeUnit.MILLISECONDS)) {
//                Thread.currentThread().interrupt();
//            }
//        } catch (InterruptedException e) {
//            logError(e.getMessage());
//            THREADPOOL.shutdownNow();
//        }
    }

}
