package com.example.pfldv3.internal.utils;


import androidx.annotation.Nullable;

import com.example.pfldv3.MApplication;

import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * Created by dji on 16/1/6.
 */
public class ModuleVerificationUtil {
        public static boolean isProductModuleAvailable() {
            return (null != MApplication.getProductInstance());
        }
    public static boolean isAircraft() {
        return MApplication.getProductInstance() instanceof Aircraft;
    }
    @Nullable
    public static FlightController getFlightController() {
        Aircraft aircraft = MApplication.getAircraftInstance();
        if (aircraft != null) {
            return aircraft.getFlightController();
        }
        return null;
    }
    public static boolean isFlightControllerAvailable() {
        return isProductModuleAvailable() && isAircraft() && (null != MApplication.getAircraftInstance()
                .getFlightController());
    }
}
