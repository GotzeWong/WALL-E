package com.a7clk.wall_e_android;

import android.app.Application;

import com.lzy.okgo.OkGo;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;

import java.util.logging.Level;

public class WalleApplication extends Application implements BootstrapNotifier {
    private static final String TAG = "WalleApplication";
    private BackgroundPowerSaver backgroundPowerSaver;

    public void onCreate() {
        super.onCreate();
        BeaconManager beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);

        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));

        backgroundPowerSaver = new BackgroundPowerSaver(this);
        //init okGo
        OkGo.init(this);
        try{
            OkGo.getInstance().debug("OkGo", Level.INFO, true);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void didEnterRegion(Region arg0) {
    }

    @Override
    public void didExitRegion(Region region) {
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
    }

}