package com.znkv.wlm;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.*;
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
    public static class DbPlayerProfile{
        public String username;
        public String UUID;

        public DbPlayerProfile(String username, String UUID) {
            this.username = username;
            this.UUID = UUID;
        }
    }

    private static void writeLogToFile(String message) throws IOException {
//        FileHandler handler = new FileHandler("WLM.log", false);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("WLM.log", true)))
        {
            String newline = System.getProperty("line.separator");
            String formatedMessage = message.replaceAll("(\n|\\n|\\\\n)", newline);
            bw.newLine();
            bw.write("[WLM log entry]");
            bw.newLine();
            DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss");
            bw.write("Date and time: " + new DateTime().toString(format));
            bw.newLine();
            bw.write(formatedMessage);
            bw.newLine();
        }
        catch (IOException e)
        {
            logInfo(e.getMessage());
        }
//        Logger logger = Logger.getLogger("com.ut.wl");
//        logger.addHandler(handler);
//        logger.info(message);
//        logger.removeHandler(handler);
    }

    public static UUID getUUIDfromUsername(String username) {
        UUID uuid = PlayerEntity.getOfflinePlayerUuid(username); // in case we fucked up something, we still have a uuid;

        try {
            logInfo("Attempting to retrieve the UUID from the ely.by server");
            URL url = new URL("https://authserver.ely.by/api/users/profiles/minecraft/" + username);
            UuidUserResponse mapped;
            try {
                // Open a connection(?) on the URL(??) and cast the response(???)
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Now it's "open", we can set the request method, headers etc.
                connection.setRequestProperty("accept", "application/json");
                // This line makes the request
                InputStream responseStream = connection.getInputStream();

                // Manually converting the response body InputStream to APOD using Jackson
                ObjectMapper mapper = new ObjectMapper();
                mapped = mapper.readValue(responseStream, UuidUserResponse.class);
                byte[] data;
                try {
                    uuid = UUID.fromString(mapped.getUuid().replaceFirst( "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5" ));
                    logInfo("Retrieved uuid: " + uuid);
                } catch (IllegalArgumentException e) {
                    logInfo(e.getMessage());
                    logInfo("Something is not right with UUID, failed to extract UUID object");
                    logInfo("Whitelist will be populated with default \"offline player\" UUID");
                    try{
                        writeLogToFile(e.getMessage() + "\n" +
                                "Something is not right with UUID, failed to extract UUID object\n" +
                                "Whitelist will be populated with \"offline player\"  UUID\n" +
                                "Generated UUID: " + uuid + "\n" +
                                "Username: " + mapped.name + "\n" + "Returned UUID: " + mapped.getUuid());
                        logInfo("Have written entry to the log.");
                    } catch (IOException b){
                        logInfo(b.getMessage());
                        logInfo("Couldnt write down the log entry");
                    }
                }
            } catch (IOException e) {
                logInfo(e.getMessage());
                logInfo("Either Username was not found, or something else happened");
                try{
                    writeLogToFile(e.getMessage() + "\n" +
                            "Either Username was not found, or something else happened\n" +
                            "Whitelist will be populated with \"offline player\"  UUID\n" +
                            "Generated UUID: " + uuid + "\n" +
                            "Username: " + username);
                    logInfo("Have written entry to the log.");
                } catch (IOException b){
                    logInfo(b.getMessage());
                    logInfo("Couldnt write down the log entry");
                }
            }
            // Finally we have the response
        } catch (MalformedURLException e) {
            logInfo(e.getMessage());
            logInfo("No fucking clue what happened, seems like internal URL got broken");
            logInfo("better restart the server and try to contact \"you know who\"");
            try{
                writeLogToFile(e.getMessage() + "\n" +
                        "No fucking clue what happened, seems like internal URL got broken\n" +
                        "better restart the server and try to contact \"you know who\"");
            } catch (IOException b){
                logInfo(b.getMessage());
                logInfo("Couldnt write down the log entry");
            }
        }
        return uuid;
    }
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
        String username = null;
        try {
            username = json.getString("username");
        } catch (JSONException e) {
            logError(e.getMessage());
        }
        double amount_in_rubbles = 0;
        try {
            amount_in_rubbles = json.getDouble("amount_main");
        } catch (JSONException e) {
            logError(e.getMessage());
        }
        int months_to_add = (int) amount_in_rubbles / config.main.Monthly_Payment_Rubbles;
        if (months_to_add > 0) {
            addPlayerFromCommandOrDonation(username, String.valueOf(months_to_add), true);
        } else {
            logInfo("Whitelist entry with the name: " + username + " got denied, sum is less than " + config.main.Monthly_Payment_Rubbles + " rubles");
            logInfo("Money donated: " + amount_in_rubbles);
            logInfo("You better expect someone to reach you out soon, admin");
            try{
                writeLogToFile("Whitelist entry with the name: " + username + " got denied, sum is less than " + config.main.Monthly_Payment_Rubbles + " rubles\n" +
                        "Money donated: " + amount_in_rubbles + "\n" +
                        "Expect him to reach out soon");
                logInfo("Have written entry to the log.");
            } catch (IOException e) {
                logInfo(e.getMessage());
                logInfo("Have failed to write entry to the log.");
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
            ArrayList<DbPlayerProfile> DbPlayers = new ArrayList<>();
            try(Connection conn = DriverManager.getConnection(config.main.Database_Url, config.main.Database_User, config.main.Database_Password)){
                stmt = conn.createStatement();
                deleteRslt = stmt.executeQuery("DELETE FROM users WHERE users.end_datetime <= (NOW() - INTERVAL 12 HOUR)");
                rslt = stmt.executeQuery("SELECT username, uuid FROM users");
                while(rslt.next()){
                    DbPlayers.add(new DbPlayerProfile(rslt.getString(1), rslt.getString(2)));
                }
                try { conn.close(); } catch (Exception e) { /* ignored */ }
            } catch (Exception e){
                logError(e.getMessage());
            } finally {
                try { rslt.close(); } catch (Exception e) { /* ignored */ }
                try { stmt.close(); } catch (Exception e) { /* ignored */ }
            }

            ArrayList<GameProfile> databaseProfiles = new ArrayList<>();
            for(DbPlayerProfile u : DbPlayers){
                UUID uuid = UUID.fromString(u.UUID);
                databaseProfiles.add(new GameProfile(uuid, u.username));
            }
            removeEveryone(server); // nuking the shit out of whitelist
            addPlayers(server, databaseProfiles);
            reload(server, "Server's whitelist got reloaded");
        }
        catch (Exception e)
        {
            logError(e.getMessage());
            logError("Something went wrong while.. doing database things! \n");
        }
    }

    private static int removeEveryone(MinecraftServer server) throws IOException {
        server.getPlayerManager().getWhitelist().values().clear();
        server.getPlayerManager().getWhitelist().save();
        return 1;
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

    public static void addPlayerFromCommandOrDonation(String username, String months, boolean isInternal){
        ResultSet rslt = null;
        Statement stmt = null;
        String query = null;
        String regexp = "[a-zA-Z0-9_]{3,16}";
        String regexp2 = "[a-zA-Z0-9]*_?[a-zA-Z0-9]*";
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss");

        cfg.offStatus();

        if(!(Pattern.matches(regexp, username) && Pattern.matches(regexp2, username))){
            if(isInternal){
                logInfo("Someone has no clue about our nickname policy");
                logInfo("Thats his 'name': " + username);
                logInfo("Expect him to reach out soon");
                try{
                    writeLogToFile("Someone has no clue about our nickname policy\n" +
                            "Thats his 'name': " + username + "\n" +
                            "Expect him to reach out soon");
                    logInfo("Have written entry to the log.");
                } catch (IOException e) {
                    logInfo(e.getMessage());
                    logInfo("Have failed to write entry to the log.");
                }
            } else {
                logInfo("I'll remind you of our username policy, 3-16 chars latin + nums and 1 _ (underscore)");
                logInfo("Now try this command once again");
            }
        } else {
            ResultSet userRslt = null;
            PreparedStatement userStmt = null;
            ArrayList<DbPlayerProfile> DbPlayers = new ArrayList<>();
            try (Connection conn = DriverManager.getConnection(cfg.main.Database_Url, cfg.main.Database_User, cfg.main.Database_Password)) {
                userStmt = conn.prepareStatement("SELECT username, uuid FROM users WHERE username = (?)");
                userStmt.setString(1, username);
                userRslt = userStmt.executeQuery();
                // it should virtually impossible to get any duplicates of the same user
                // but who knows...?
                while (userRslt.next()) {
                    DbPlayers.add(new DbPlayerProfile(userRslt.getString(1), userRslt.getString(2)));
                }

                if (DbPlayers.size() > 1)
                    try {
                        conn.close();
                    } catch (Exception e) { logInfo(e.getMessage()); }

                Whitelist whitelist = server.getPlayerManager().getWhitelist();
                UUID uuid = PlayerEntity.getOfflinePlayerUuid("something_went_horribly_wrong");
                GameProfile playerProfile = new GameProfile(uuid, "something_went_horribly_wrong");
                int months_to_add = Integer.parseInt(months);
                // in case we didnt find such user in our database
                if(DbPlayers.size() == 0) {
                    uuid = getUUIDfromUsername(username);
                    playerProfile = new GameProfile(uuid, username);
                } else if(DbPlayers.size() == 1) {
                    uuid = UUID.fromString(DbPlayers.get(0).UUID);
                    playerProfile = new GameProfile(uuid, username);
                }
                if(DbPlayers.size()>1) {

                    if(isInternal){
                        try{
                            logInfo("Found multiple database entries of such username during donation processing");
                            writeLogToFile("Found multiple database entries of such username during donation processing\n" +
                                    "Thats his 'name': " + username + "\n" +
                                    "failed to process the donation");
                            logInfo("Have written entry to the log.");
                        } catch (IOException e) {
                            logInfo(e.getMessage());
                            logInfo("Have failed to write entry to the log.");
                        }
                    } else {
                        logInfo("Now this is getting funny, there is multiple of users with such name");
                        logInfo("Fix your database and get back");
                    }
                } else {
                    if (months_to_add > 0) {
                        try (Connection conn1 = DriverManager.getConnection(cfg.main.Database_Url, cfg.main.Database_User, cfg.main.Database_Password)) {
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
                                try{
                                    writeLogToFile("Player with the name: " + username + " is already whitelisted. Adding " + months_to_add + " more month(s) to his limit\n" +
                                            "End DateTime from database: " + end_datetime_from_db.toString(format) + "\n" +
                                            "New DateTime: " + new_datetime.toString(format));
                                    logInfo("Have written entry to the log.");
                                } catch (IOException e) {
                                    logInfo(e.getMessage());
                                    logInfo("Have failed to write entry to the log.");
                                }
                            } else {
                                DateTime current_time = new DateTime();
                                DateTime current_time_plus = current_time.plusMonths(months_to_add);
                                logInfo("New player: " + username + " got added for " + months_to_add + " month(s)");
                                logInfo(current_time.toString(format));
                                logInfo(current_time_plus.toString(format));
                                try{
                                    writeLogToFile("New player: " + username + " got added for " + months_to_add + " month(s)\n" +
                                            current_time.toString(format) + "\n" +
                                            current_time_plus.toString(format));
                                    logInfo("Have written entry to the log.");
                                } catch (IOException e) {
                                    logInfo(e.getMessage());
                                    logInfo("Have failed to write entry to the log.");
                                }
                                query = "insert into users (`username`,`start_datetime`,`end_datetime`,`uuid`) values ('" + username + "','" + current_time.toString(format) + "','" + current_time_plus.toString(format) + "','" + uuid + "')";
                            }
                            rslt = stmt.executeQuery(query);
                            if(!isInternal){
                                logInfo("So called 'po blatu', huh :^)");
                            }
                            run(); // sync whitelist with database
                        } catch (Exception e) {
                            logInfo(e.getMessage());
                        }
                    } else {
                        if(months_to_add == 0){
                            logInfo("I mean.. how can i add or remove something with the value of ZERO MONTHS??? FOR FUCKS SAKE MAN");
                        } else {
                            logInfo("Well, lets subtract some months from somebody");
                            try (Connection conn2 = DriverManager.getConnection(cfg.main.Database_Url, cfg.main.Database_User, cfg.main.Database_Password)) {
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
                                logInfo(e.getMessage());
                            }

                        }
                    }
                }


            } catch (SQLException e){
                logInfo(e.getMessage());
                logInfo("Failed during player addition process");
            }


        }


//            try {
//                int months_to_add = Integer.parseInt(months);
//                logInfo("Months to add/subtract: " + months_to_add);
//                Whitelist whitelist = server.getPlayerManager().getWhitelist();
//                UUID offlineUUID = PlayerEntity.getOfflinePlayerUuid(username);
//                GameProfile playerProfile = new GameProfile(offlineUUID, username);
//                if (months_to_add > 0) {
//                    try (Connection conn = DriverManager.getConnection(cfg.main.Database_Url, cfg.main.Database_User, cfg.main.Database_Password)) {
//                        stmt = conn.createStatement();
//                        if (whitelist.isAllowed(playerProfile)) {
//                            ResultSet results = stmt.executeQuery("select end_datetime from users where username = '" + username + "'");
//                            DateTime end_datetime_from_db = null;
//                            DateTime new_datetime = null;
//                            while (results.next()) {
//                                end_datetime_from_db = new DateTime(results.getTimestamp("end_datetime"));
//                            }
//                            new_datetime = end_datetime_from_db.plusMonths(months_to_add);
//                            logInfo("Player with the name: " + username + " is already whitelisted. Adding " + months_to_add + " more month(s) to his limit");
//                            query = "update users set end_datetime = '" + new_datetime.toString(format) + "' where username = '" + username + "'";
//                            logInfo("End DateTime from database: " + end_datetime_from_db.toString(format));
//                            logInfo("New DateTime: " + new_datetime.toString(format));
//                        } else {
//                            DateTime current_time = new DateTime();
//                            DateTime current_time_plus = current_time.plusMonths(months_to_add);
//                            logInfo("New player: " + username + " got added for " + months_to_add + " month(s)");
//                            logInfo(current_time.toString(format));
//                            logInfo(current_time_plus.toString(format));
//                            query = "insert into users (`username`,`start_datetime`,`end_datetime`) values ('" + username + "','" + current_time.toString(format) + "','" + current_time_plus.toString(format) + "')";
//                        }
//                        rslt = stmt.executeQuery(query);
//                        logInfo("So called 'po blatu', huh :^)");
//                        run(); // sync whitelist with database
//                    } catch (Exception e) {
//                        logError(e.getMessage());
//                    }
//                } else {
//                    if(months_to_add == 0){
//                        logInfo("I mean.. how can i add or remove something with the value of ZERO MONTHS??? FOR FUCKS SAKE MAN");
//                    } else {
//                        logInfo("Well, lets subtract some months from somebody");
//                        try (Connection conn = DriverManager.getConnection(cfg.main.Database_Url, cfg.main.Database_User, cfg.main.Database_Password)) {
//                            stmt = conn.createStatement();
//                            if (whitelist.isAllowed(playerProfile)) {
//                                ResultSet results = stmt.executeQuery("select end_datetime from users where username = '" + username + "'");
//                                DateTime end_datetime_from_db = null;
//                                DateTime current_datetime = new DateTime();
//                                DateTime new_datetime = null;
//                                months_to_add = months_to_add * -1;
//                                while (results.next()) {
//                                    end_datetime_from_db = new DateTime(results.getTimestamp("end_datetime"));
//                                }
//                                if(Months.monthsBetween(current_datetime, end_datetime_from_db).getMonths() <= 0){
//                                    logInfo("This player has around 1 month of playtime left, if you want to, just delete him, you know the command");
//                                    logInfo("Playtime ends at: " + end_datetime_from_db.toString());
//                                    logInfo("Time now: " + current_datetime.toString());
//                                } else {
//                                    new_datetime = end_datetime_from_db.minusMonths(months_to_add);
//                                    logInfo("Player with the name: " + username + ". Subtracting " + months_to_add + " month(s) from his limit");
//                                    query = "update users set end_datetime = '" + new_datetime.toString(format) + "' where username = '" + username + "'";
//                                    logInfo("End DateTime from database: " + end_datetime_from_db.toString(format));
//                                    logInfo("New DateTime: " + new_datetime.toString(format));
//                                    rslt = stmt.executeQuery(query);
//                                    run(); // sync whitelist with database
//                                }
//                            } else {
//                                logInfo("This player is not whitelisted, i cant subtract months from nothing, you know");
//                            }
//                        } catch (Exception e) {
//                            logError(e.getMessage());
//                        }
//
//                    }
//                }
//            } catch (NumberFormatException e){
//                logError("Your 'months' doesnt seem like a number to me, try again");
//            }
//        }
        cfg.onStatus();
    }

    public static void deletePlayer(String username){
        Whitelist whitelist = server.getPlayerManager().getWhitelist();
        try {
            ResultSet rslt = null;
            PreparedStatement stmt = null;
            ArrayList<DbPlayerProfile> DbPlayers = new ArrayList<>();
            try (Connection conn = DriverManager.getConnection(cfg.main.Database_Url, cfg.main.Database_User, cfg.main.Database_Password)) {
                stmt = conn.prepareStatement("SELECT username, uuid FROM users WHERE username = (?)");
                stmt.setString(1, username);
                rslt = stmt.executeQuery();
                // it should virtually impossible to get any duplicates of the same user
                // but who knows...?
                while (rslt.next()) {
                    DbPlayers.add(new DbPlayerProfile(rslt.getString(1), rslt.getString(2)));
                }
                try {
                    conn.close();
                } catch (Exception e) { logInfo(e.getMessage()); }
            } catch (Exception e) {
                logInfo(e.getMessage());
            } finally {
                try {
                    rslt.close();
                } catch (Exception e) { logInfo(e.getMessage()); }
                try {
                    stmt.close();
                } catch (Exception e) { logInfo(e.getMessage()); }
            }
            if(DbPlayers.size()==0){
                logInfo("Players not whitelisted");
            } else if(DbPlayers.size()>1){
                logInfo("Now this is getting funny, there is multiple of users with such name");
                logInfo("Fix your database and get back");
            } else {
                UUID uuid = UUID.fromString(DbPlayers.get(0).UUID);
                GameProfile playerProfile = new GameProfile(uuid, username);
                ResultSet deleteRslt = null;
                Statement deleteStmt = null;

                cfg.offStatus();

                if(whitelist.isAllowed(playerProfile)){
                    logInfo("Attempting to remove the player");
                    try (Connection conn = DriverManager.getConnection(cfg.main.Database_Url, cfg.main.Database_User, cfg.main.Database_Password)) {
                        deleteStmt = conn.createStatement();
                        deleteRslt = deleteStmt.executeQuery("DELETE FROM users where username = '" + username + "'");
                        run();
                    } catch (Exception e) {
                        logInfo(e.getMessage());
                    }
                } else {
                    logInfo("Players not whitelisted");
                }

                cfg.onStatus();
            }
        } catch (Exception e)
        {
            logInfo(e.getMessage());
            logInfo("Couldnt connect to the database during player removal per console command\n");
        }
    }


    public static void full_restart(MinecraftServer server){
        cfg.onStatus();

        logInfo("Commencing full restart");
        cfg.load(new File(FabricLoader.getInstance().getGameDir()+ "/config/WlmConfig.json"));
        sock.close();
        StartSyncing(server, cfg);
    }

    public static void reload_from_db(){
        run();
        logInfo("(per admins request)");
    }


}
