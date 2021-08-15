package com.znkv.wlm.Storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static com.znkv.wlm.Utils.WlmLogger.logError;
import static com.znkv.wlm.Utils.WlmLogger.logInfo;

// a wierd singleton
public class WlmConfig {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    public static class MainConfig{
        public String Database_Password = "1234";
        public String DonationAlerts_Token = "token";
        public int Whitelist_Reload_time = 60000;
        public int Monthly_Payment_Rubbles = 50;
        public boolean Should_I_Check_Whitelist = true;
        public String Database_User = "root";
        public String Database_Url = "jdbc:mysql://localhost:3306/minedb";
    }

    public static MainConfig main = new MainConfig();
    public static WlmConfig currentInstance = new WlmConfig();
    public static String cfgPath;
    // load cfg
    public static WlmConfig load(File file) {
        cfgPath = file.getPath();
        MainConfig config = new MainConfig();
        if (file.exists()) {
            try (FileReader fileReader = new FileReader(file, StandardCharsets.UTF_8)) {
                config = gson.fromJson(fileReader, MainConfig.class);
            } catch (IOException e) {
                throw new RuntimeException("[Wlm] Problem occurred when trying to load config: ", e);
            }
        }
        currentInstance.main = config;
        currentInstance.save(file);
        return currentInstance;
    }

//    public synchronized static WlmConfig getInstance(){
//        if(currentInstance == null) {
//            currentInstance = new WlmConfig();
//        }
//        return currentInstance;
//    }

    // save cfg
    public void save(File file) {
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(main, writer);
        } catch (IOException e) {
            logError("Problem occurred when saving config: " + e.getMessage());
        }
    }

    public static void toggleReload(){
        if (!currentInstance.main.Should_I_Check_Whitelist) {
            currentInstance.main.Should_I_Check_Whitelist = true ;
            String status = "Timer for whitelist reload is ON";
            logInfo(status);
        }
        else {
            currentInstance.main.Should_I_Check_Whitelist = false;
            String status = "Timer for whitelist reload is OFF";
            logInfo(status);
        }
        currentInstance.save(new File(cfgPath));
    }

    public static void changeUrl(String url){
        boolean flag = false; // Assuming that the current status is on
        if(currentInstance.main.Should_I_Check_Whitelist){
            offStatus();
            flag = true;
        }
        currentInstance.main.Database_Url = url;
        if(flag){
            onStatus();
        }
        logInfo("Url for database access has been changed, next time we'll use this one");
        currentInstance.save(new File(cfgPath));
    }

    public static void changeUser(String user){
        boolean flag = false; // Assuming that the current status is on
        if(currentInstance.main.Should_I_Check_Whitelist){
            offStatus();
            flag = true;
        }
        currentInstance.main.Database_User = user;
        if(flag){
            onStatus();
        }
        logInfo("User for database access has been changed, next time we'll use this one");
        currentInstance.save(new File(cfgPath));
    }

    public static void changePassword(String password){
        boolean flag = false; // Assuming that the current status is on
        if(currentInstance.main.Should_I_Check_Whitelist){
            offStatus();
            flag = true;
        }
        currentInstance.main.Database_Password = password;
        if(flag){
            onStatus();
        }
        logInfo("Password for database access has been changed, next time we'll use this one");
        currentInstance.save(new File(cfgPath));
    }

    public static void changeUserAndPassword(String user, String password){
        boolean flag = false; // Assuming that the current status is on
        if(currentInstance.main.Should_I_Check_Whitelist){
            offStatus();
            flag = true;
        }
        currentInstance.main.Database_User = user;
        currentInstance.main.Database_Password = password;
        if(flag){
            onStatus();
        }
        logInfo("Both user and password for database access has been changed, next time we'll use them instead");
        currentInstance.save(new File(cfgPath));
    }


    public static void changeDbCred(String url, String user, String password){
        boolean flag = false; // Assuming that the current status is on
        if(currentInstance.main.Should_I_Check_Whitelist){
            offStatus();
            flag = true;
        }
        currentInstance.main.Database_Url = url;
        currentInstance.main.Database_User = user;
        currentInstance.main.Database_Password = password;
        if(flag){
            onStatus();
        }
        logInfo("All of the database credentials has been changed, it'll be applied immediately");
        currentInstance.save(new File(cfgPath));
    }

    public static void changeReloadTime(String whitelistreloadtime){
        boolean flag = false; // Assuming that the current status is on
        if(currentInstance.main.Should_I_Check_Whitelist){
            offStatus();
            flag = true;
        }
        currentInstance.main.Whitelist_Reload_time = Integer.parseInt(whitelistreloadtime);
        if(flag){
            onStatus();
        }
        logInfo("Whitelist reload time has been changed, if the timer is on, then we'll start counting from zero");
        currentInstance.save(new File(cfgPath));
    }

    public static void changeMonthlyPayment(String monthlypayment){
        currentInstance.main.Monthly_Payment_Rubbles = Integer.parseInt(monthlypayment);
        logInfo("Monthly payment has been changed, new donations will have to abide by new sum :^)");
        currentInstance.save(new File(cfgPath));
    }

    public static void changeDAToken(String da_token){
        currentInstance.main.DonationAlerts_Token = da_token;
        logInfo("Token has been changed, use '/wl restart' to apply changes");
        currentInstance.save(new File(cfgPath));
    }

    public static void offStatus(){  // USE ONLY IN PAIR WITH ON() !!!
        if(currentInstance.main.Should_I_Check_Whitelist){
            currentInstance.main.Should_I_Check_Whitelist = false;
        }
    }

    public static void onStatus(){  // USE ONLY IN PAIR WITH OFF() !!!
        if(!currentInstance.main.Should_I_Check_Whitelist){
            currentInstance.main.Should_I_Check_Whitelist = true;
        }
    }


}
