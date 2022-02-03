package com.weclont.mesgetparent;

import android.app.Application;
import android.content.Context;

import java.net.Socket;

public class MainApplication extends Application {

    private static Context mainActivityContext;
    private static Context loginActivityContext;
    public static String ip = "";
    public static String port = "";
    public static String mcname = "";
    public static Socket socket;


    public static void setLoginActivityContext(Context c){
        loginActivityContext = c;
    }

    public static Context getLoginActivityContext() {
        return loginActivityContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    public static void setMainActivityContext(Context c){
        mainActivityContext = c;
    }

    public static Context getMainActivityContext() {
        return mainActivityContext;
    }

}
