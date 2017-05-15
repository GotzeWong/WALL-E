package com.a7clk.wall_e_android.model;

/**
 * Created by Gotze on 12/2/2016.
 */

public class Config {
    public static final String CAR_IP = "192.168.1.100";
    public static final String SERVER_IP = "192.168.1.127";
    public static final int CAR_PORT = 8080;
    public static final int SERVER_PORT = 8090;

    public static final String LOCATION_SERVER_URL_IBEACON = "http://" + SERVER_IP + ":8081/ibeacon";
    public static final String LOCATION_SERVER_URL_WIFI = "http://" + SERVER_IP + ":8081/wifi";
}
