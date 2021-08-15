package com.znkv.wlm;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.znkv.wlm.Storage.WlmConfig;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.EngineIOException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WhitelistEntry;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.Months;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;
import net.minecraft.server.Whitelist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.znkv.wlm.Utils.WlmLogger.logError;
import static com.znkv.wlm.Utils.WlmLogger.logInfo;

public class DonationProcessor {

    private final static String da_url = "https://socket.donationalerts.ru:443";
    private static Emitter.Listener donationListener;
    private static Emitter.Listener errorListener;
    private static Emitter.Listener connectListener;
    private static WlmConfig cfg;
    private static Socket sock;
    private static JSONObject json;
    public static MinecraftServer server;
    private static WlmConfig config;
//    private final IO.Options options = IO.Options.builder()
//            .setReconnection(true)
//            .setReconnectionAttempts(Integer.MAX_VALUE)
//            .setReconnectionDelay(1_000)
//            .setReconnectionDelayMax(5_000)
//            .setRandomizationFactor(0.5)
//            .setTimeout(20_000)
//            .build();

    public static void StartSyncing(MinecraftServer server, WlmConfig config) {
        cfg = config;
        DonationProcessor.server = server;
        logInfo("Well, lets try to make DonationAlerts thingie work");
        try {
//            IO.Options opt = new IO.Options();
//            opt.forceNew = true;
//            opt.path = "/wss";
            sock = IO.socket(da_url);
        } catch(URISyntaxException e) {
            logInfo("We got skull fucked, donation alerts closed their url for donations socket");
        }
        donationListener = new Emitter.Listener() {
            @Override
            public void call(Object... arg0) {
                try {
                    logInfo(new JSONObject((String)arg0[0]).getString("username"));
                } catch (JSONException e) {
                    logError(e.getMessage());
                }
                try {
                    logInfo("amount_main: " + new JSONObject((String)arg0[0]).getInt("amount_main"));
                } catch (JSONException e) {
                    logError(e.getMessage());
                }
                try {
                    logInfo(new JSONObject((String)arg0[0]).toString());
                } catch (JSONException e) {
                    logError(e.getMessage());
                }
                processDonate((String)arg0[0], server, config);

            }
        };
        errorListener = new Emitter.Listener() {
            @Override
            public void call(Object... arg0) {
                logError("THERE IS SOME PROBLEM!!");
                EngineIOException obj = (EngineIOException)arg0[0];
                logError(obj.getMessage());
                logError(ExceptionUtils.getStackTrace(obj));

//                JSONObject error = (JSONObject) args[0];
//                String message = error.getString("message");
//                System.out.println(error); // not authorized
//                JSONObject data = error.getJSONObject("data"); // additional details (optional)
                logError(obj.code.toString());
            }
        };
        connectListener = new Emitter.Listener() {
            @Override
            public void call(Object... arg0) {
                logInfo("Connection with donationalerts server has been established!");
            }
        };
        try {
            connectAndSetListeners(config);
        } catch(JSONException e){
            logError(e.getMessage());
        }
//        run();
    }




    private static void processDonate(String data, MinecraftServer server, WlmConfig config){
        try {
            json = new JSONObject(data);
        } catch (JSONException e) {
            logInfo(e.getMessage());
        }
        ResultSet rslt = null;
        Statement stmt = null;
        String query = null;
        String username = null;
        try {
            username = json.getString("username");
        } catch (JSONException e) {
            logError(e.getMessage());
        }
        String regexp = "[a-zA-Z0-9_]{3,16}";
        String regexp2 = "[a-zA-Z0-9]*_?[a-zA-Z0-9]*";
        if(!(Pattern.matches(regexp, username) && Pattern.matches(regexp2, username))){
            logInfo("Some dumbass forgot how to read or has severe brain damage so he cant understand any of those fucking rules that we have");
            logInfo("Thats his 'name': " + username);
            logInfo("Expect him to reach out soon");
        } else {
            double amount_in_rubbles = 0;
            try {
                amount_in_rubbles = json.getDouble("amount_main");
            } catch (JSONException e) {
                logError(e.getMessage());
            }
            int months_to_add = (int) amount_in_rubbles / 50;
            UUID offlineUUID = PlayerEntity.getOfflinePlayerUuid(username);
            GameProfile donaterProfile = new GameProfile(offlineUUID, username);
            if (months_to_add > 0) {
                try (Connection conn = DriverManager.getConnection(config.main.Database_Url, config.main.Database_User, config.main.Database_Password)) {
                    stmt = conn.createStatement();
                    DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss");
                    if (server.getPlayerManager().isWhitelisted(donaterProfile)) {
                        ResultSet results = stmt.executeQuery("select end_datetime from users where username = '" + username + "'");
                        DateTime end_datetime_from_db = null;
                        DateTime new_datetime = null;
                        while (results.next()) {
                            end_datetime_from_db = new DateTime(results.getTimestamp("end_datetime"));
                        }
                        new_datetime = end_datetime_from_db.plusMonths(months_to_add);
                        logInfo("Player with the name: " + username + " is already whitelisted. Adding " + months_to_add + " more month(s) to his limit");
                        query = "update users set end_datetime = '" + new_datetime.toString(format) + "' where username = '" + username + "'";
                        logInfo("End DateTime from database: " + end_datetime_from_db.toString(format));
                        logInfo("New DateTime" + new_datetime.toString(format));
                    } else {
                        DateTime current_time = new DateTime();
                        DateTime current_time_plus = current_time.plusMonths(months_to_add);
                        logInfo("New player: " + username + " got added for " + months_to_add + " month(s)");
                        logInfo(current_time.toString(format));
                        logInfo(current_time_plus.toString(format));
                        query = "insert into users (`username`,`start_datetime`,`end_datetime`) values ('" + username + "','" + current_time.toString(format) + "','" + current_time_plus.toString(format) + "')";
                    }
                    rslt = stmt.executeQuery(query);
                    run(); // sync whitelist with database
                } catch (Exception e) {
                    logError(e.getMessage());
                }
            } else {
                logInfo("Whitelist entry with the name: " + username + " got denied, sum is less than 50 rubles");
                logInfo("Money donated: " + amount_in_rubbles);
                logInfo("You better expect someone to reach you out soon, admin");
            }
        }
    }


    public static void connectAndSetListeners(WlmConfig config) throws JSONException {
        String _token = config.main.DonationAlerts_Token;
        logInfo(_token);
        sock.connect();
        logInfo("Sending token to DA server");
        sock.emit("add-user", new JSONObject()
                .put("token", _token)
                .put("type", "minor"));

        sock.on(Socket.EVENT_CONNECT, connectListener)
                .on(Socket.EVENT_CONNECT_ERROR, errorListener)
                .on("donation", donationListener);
        logInfo("Listeners for events has been set");
    }

    public static void run() {
        try
        {
//            Connection conn = DriverManager.getConnection(Config.url, Config.user, Config.password);
//            Statement stmt = conn.createStatement();
//            ResultSet rslt = stmt.executeQuery("SELECT username, uuid FROM users");
//            conn.close();
            ResultSet rslt = null;
            Statement stmt = null;
            ResultSet deleteRslt = null;
            ArrayList<String> DbPlayers = new ArrayList<>();
            try(Connection conn = DriverManager.getConnection(config.main.Database_Url, config.main.Database_User, config.main.Database_Password)){
                stmt = conn.createStatement();
                deleteRslt = stmt.executeQuery("DELETE FROM users WHERE users.end_datetime <= (NOW() - INTERVAL 12 HOUR)");
                rslt = stmt.executeQuery("SELECT username FROM users");
                while(rslt.next()){
                    DbPlayers.add(rslt.getString(1));
                }
                try { conn.close(); } catch (Exception e) { /* ignored */ }
            } catch (Exception e){
                logError(e.getMessage());
            } finally {
                try { rslt.close(); } catch (Exception e) { /* ignored */ }
                try { stmt.close(); } catch (Exception e) { /* ignored */ }
            }

            ArrayList<GameProfile> databaseProfiles = new ArrayList<>();
            for(String u : DbPlayers){
                UUID offlineUUID = PlayerEntity.getOfflinePlayerUuid(u);
                databaseProfiles.add(new GameProfile(offlineUUID, u));
            }

            ArrayList<GameProfile> whitelistProfiles = new ArrayList<>();
            try(BufferedReader bufferedReader = new BufferedReader(new FileReader(FabricLoader.getInstance().getGameDir() + "/whitelist.json"))) {
                ArrayList<UserMapping> whitelistPlayersList = new ArrayList<>(Arrays.asList(new Gson().fromJson(bufferedReader, UserMapping[].class)));
                for(UserMapping user : whitelistPlayersList){
                    whitelistProfiles.add(new GameProfile(UUID.fromString(user.uuid), user.name));
                }
            } catch (Exception e){
                logError(e.getMessage());
                logError("Something got fucked up during attempt to create a list of players from whitelist");
            }
            removePlayers(server, whitelistProfiles);
            addPlayers(server, databaseProfiles);
            reload(server, "Server's whitelist got synced with database");
        }
        catch (Exception e)
        {
            logError(e.getMessage());
            logError("Something went wrong while.. doing database things! \n");
        }
    }

    private static int reload(MinecraftServer server, String message) {
        server.getPlayerManager().reloadWhitelist();
        logInfo(message);
        server.kickNonWhitelistedPlayers(server.getCommandSource());
        return 1;
    }

    private static int addPlayers(MinecraftServer server, Collection<GameProfile> players) throws CommandSyntaxException {
        Whitelist whitelist = server.getPlayerManager().getWhitelist();
        int i = 0;

        for(GameProfile gameprofile : players) {
            if (!whitelist.isAllowed(gameprofile)  ){
                WhitelistEntry whitelistentry = new WhitelistEntry(gameprofile);
                whitelist.add(whitelistentry);
                ++i;
            }
        }
        return i;
//        if (i == 0) {
//            throw PLAYER_ALREADY_WHITELISTED.create();
//        } else {
//            return i;
//        }
    }

    private static int removePlayers(MinecraftServer server, Collection<GameProfile> players) throws CommandSyntaxException {
        Whitelist whitelist = server.getPlayerManager().getWhitelist();
        int i = 0;

        for(GameProfile gameprofile : players) {
            if (whitelist.isAllowed(gameprofile)) {
                WhitelistEntry whitelistentry = new WhitelistEntry(gameprofile);
                whitelist.remove(whitelistentry);
                ++i;
            }
        }
        return i;
//        if (i == 0) {
//            throw PLAYER_NOT_WHITELISTED.create();
//        } else {
////            server.kickPlayersNotWhitelisted(server.getCommandSource());
//            return i;
//        }
    }

    public static void addPlayerFromCommand(String username, String months){
        ResultSet rslt = null;
        Statement stmt = null;
        String query = null;
        String regexp = "[a-zA-Z0-9_]{3,16}";
        String regexp2 = "[a-zA-Z0-9]*_?[a-zA-Z0-9]*";
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss");

        cfg.offStatus();

        if(!(Pattern.matches(regexp, username) && Pattern.matches(regexp2, username))){
            logInfo("I'll remind you of our username policy, 3-16 chars latin + nums and 1 _ (underscore)");
            logInfo("Now try this command once again");
        } else {
            try {
                int months_to_add = Integer.parseInt(months);
                logInfo("Months to add/subtract: " + months_to_add);
                Whitelist whitelist = server.getPlayerManager().getWhitelist();
                UUID offlineUUID = PlayerEntity.getOfflinePlayerUuid(username);
                GameProfile playerProfile = new GameProfile(offlineUUID, username);
                if (months_to_add > 0) {
                    try (Connection conn = DriverManager.getConnection(cfg.main.Database_Url, cfg.main.Database_User, cfg.main.Database_Password)) {
                        stmt = conn.createStatement();
                        if (whitelist.isAllowed(playerProfile)) {
                            ResultSet results = stmt.executeQuery("select end_datetime from users where username = '" + username + "'");
                            DateTime end_datetime_from_db = null;
                            DateTime new_datetime = null;
                            while (results.next()) {
                                end_datetime_from_db = new DateTime(results.getTimestamp("end_datetime"));
                            }
                            new_datetime = end_datetime_from_db.plusMonths(months_to_add);
                            logInfo("Player with the name: " + username + " is already whitelisted. Adding " + months_to_add + " more month(s) to his limit");
                            query = "update users set end_datetime = '" + new_datetime.toString(format) + "' where username = '" + username + "'";
                            logInfo("End DateTime from database: " + end_datetime_from_db.toString(format));
                            logInfo("New DateTime: " + new_datetime.toString(format));
                        } else {
                            DateTime current_time = new DateTime();
                            DateTime current_time_plus = current_time.plusMonths(months_to_add);
                            logInfo("New player: " + username + " got added for " + months_to_add + " month(s)");
                            logInfo(current_time.toString(format));
                            logInfo(current_time_plus.toString(format));
                            query = "insert into users (`username`,`start_datetime`,`end_datetime`) values ('" + username + "','" + current_time.toString(format) + "','" + current_time_plus.toString(format) + "')";
                        }
                        rslt = stmt.executeQuery(query);
                        logInfo("So called 'po blatu', huh :^)");
                        run(); // sync whitelist with database
                    } catch (Exception e) {
                        logError(e.getMessage());
                    }
                } else {
                    if(months_to_add == 0){
                        logInfo("I mean.. how can i add or remove something with the value of ZERO MONTHS??? FOR FUCKS SAKE MAN");
                    } else {
                        logInfo("Well, lets subtract some months from somebody");
                        try (Connection conn = DriverManager.getConnection(cfg.main.Database_Url, cfg.main.Database_User, cfg.main.Database_Password)) {
                            stmt = conn.createStatement();
                            if (whitelist.isAllowed(playerProfile)) {
                                ResultSet results = stmt.executeQuery("select end_datetime from users where username = '" + username + "'");
                                DateTime end_datetime_from_db = null;
                                DateTime current_datetime = new DateTime();
                                DateTime new_datetime = null;
                                months_to_add = months_to_add * -1;
                                while (results.next()) {
                                    end_datetime_from_db = new DateTime(results.getTimestamp("end_datetime"));
                                }
                                if(Months.monthsBetween(current_datetime, end_datetime_from_db).getMonths() <= 0){
                                    logInfo("This player has around 1 month of playtime left, if you want to, just delete him, you know the command");
                                    logInfo("Playtime ends at: " + end_datetime_from_db.toString());
                                    logInfo("Time now: " + current_datetime.toString());
                                } else {
                                    new_datetime = end_datetime_from_db.minusMonths(months_to_add);
                                    logInfo("Player with the name: " + username + ". Subtracting " + months_to_add + " month(s) from his limit");
                                    query = "update users set end_datetime = '" + new_datetime.toString(format) + "' where username = '" + username + "'";
                                    logInfo("End DateTime from database: " + end_datetime_from_db.toString(format));
                                    logInfo("New DateTime: " + new_datetime.toString(format));
                                    rslt = stmt.executeQuery(query);
                                    run(); // sync whitelist with database
                                }
                            } else {
                                logInfo("This player is not whitelisted, i cant subtract months from nothing, you know");
                            }
                        } catch (Exception e) {
                            logError(e.getMessage());
                        }

                    }
                }
            } catch (NumberFormatException e){
                logError("Your 'months' doesnt seem like a number to me, try again");
            }
        }
        cfg.onStatus();
    }

    public static void deletePlayer(String username){
        Whitelist whitelist = server.getPlayerManager().getWhitelist();
        UUID offlineUUID = PlayerEntity.getOfflinePlayerUuid(username);
        GameProfile playerProfile = new GameProfile(offlineUUID, username);
        ResultSet deleteRslt = null;
        Statement stmt = null;

        cfg.offStatus();

        if(whitelist.isAllowed(playerProfile)){
            logInfo("Attempting to remove the player");
            try (Connection conn = DriverManager.getConnection(cfg.main.Database_Url, cfg.main.Database_User, cfg.main.Database_Password)) {
                stmt = conn.createStatement();
                deleteRslt = stmt.executeQuery("DELETE FROM users where username = '" + username + "'");
                run();
            } catch (Exception e) {
                logError(e.getMessage());
            }
        } else {
            logError("Player not whitelisted");
        }

        cfg.onStatus();
    }


    public static void full_restart(MinecraftServer server){
        cfg.onStatus();

        logInfo("Commencing full restart");
        cfg.load(new File(FabricLoader.getInstance().getGameDir()+ "/config/WlmConfig.json"));
        sock.close();
        StartSyncing(server, cfg);
    }

    public static void reload_from_db(){
        reload(server, "Server's whitelist got synced with database per Admin request");
    }


}
