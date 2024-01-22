package com.example.pfldv3;

import android.app.Application;
import android.content.Context;

import com.secneo.sdk.Helper;

import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.remotecontroller.RemoteController;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.common.flightcontroller.FlightControllerState;

public class MApplication extends Application {
    private DemoApplication demoApplication;
    private static BaseProduct product;
    private static Application app = null;

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
        if (demoApplication == null) {
            demoApplication = new DemoApplication();
            demoApplication.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        demoApplication.onCreate();
    }
    /**
     * Gets instance of the specific product connected after the
     * API KEY is successfully validated. Please make sure the
     * API_KEY has been added in the Manifest
     */
    public static synchronized BaseProduct getProductInstance() {
        product = DJISDKManager.getInstance().getProduct();

        return product;
    }

    public static synchronized FlightControllerState getFlightControllerState(){ //飞行器状态

        return null;
    }

    public static synchronized RemoteController getRemoteControllerInstance() {
        if (getProductInstance() == null) return null;
        RemoteController remoteController = null;

        if (getProductInstance() instanceof Aircraft){
            remoteController = ((Aircraft) getProductInstance()).getRemoteController();
        }
        return remoteController;
    }

    public static synchronized Aircraft getAircraftInstance() {
        if (!isAircraftConnected()) {
            return null;
        }
        return (Aircraft) getProductInstance();
    }
    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }
    public static Application getInstance() {
        return MApplication.app;
    }
}
