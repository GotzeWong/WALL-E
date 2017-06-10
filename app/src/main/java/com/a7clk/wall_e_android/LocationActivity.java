package com.a7clk.wall_e_android;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.a7clk.wall_e_android.model.Config;
import com.a7clk.wall_e_android.model.Location;
import com.google.gson.Gson;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Response;

public class LocationActivity extends AppCompatActivity implements BeaconConsumer {
    protected static final String TAG = "LocationActivity";
    protected static final String IBEACON="1";
    protected static final String WIFI="2";

    @Bind(R.id.my_location)
    ImageView myLocation;
    @Bind(R.id.btn_switch)
    Button btnSwitch;
    @Bind(R.id.map)
    ImageView map;
    private boolean isWorking = false;
    private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
    private WifiManager wifiManager;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    HashMap<String, Integer> ibeaconMap = new HashMap<>();//待发送的ibeacon
    final Handler handler = new Handler();
    Runnable runnable;
    float wifiY=0;
    float wifiX=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        ButterKnife.bind(this);

        initBeacon();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        setLocation(200, 200);//设置初始位置
        initTasks();
    }

    private void initTasks() {
        runnable = new Runnable() {
            @Override
            public void run() {
                //通过wifi获取位置信息
                List<ScanResult> results = wifiManager.getScanResults();
                HashMap<String, Integer> wifiMap = new HashMap<>();
                for (ScanResult result : results) {
                    wifiMap.put((result.BSSID).toUpperCase(), result.level);
                }
                if(wifiMap.size()>0){
                    OkGo.post(Config.LOCATION_SERVER_URL_WIFI)
                            .tag(this)
                            .upJson(new Gson().toJson(wifiMap))
                            .execute(new StringCallback() {
                                @Override
                                public void onSuccess(String s, Call call, Response response) {
                                    postSuccess(s, WIFI);
                                }

                                @Override
                                public void onError(Call call, Response response, Exception e) {
                                    Toast.makeText(getApplicationContext(),"网络错误",Toast.LENGTH_SHORT).show();
                                    super.onError(call, response, e);
                                }
                            });
                }
                if(ibeaconMap.size()>0){//发送蓝牙检测情况
                    OkGo.post(Config.LOCATION_SERVER_URL_IBEACON)
                            .tag(this)
                            .upJson(new Gson().toJson(ibeaconMap))
                            .execute(new StringCallback() {
                                @Override
                                public void onSuccess(String s, Call call, Response response) {
                                    postSuccess(s, IBEACON);
                                }

                                @Override
                                public void onError(Call call, Response response, Exception e) {
                                    Toast.makeText(getApplicationContext(),"网络错误",Toast.LENGTH_SHORT).show();
                                    super.onError(call, response, e);
                                }
                            });
                    ibeaconMap.clear();
                }
                handler.postDelayed(this, 5000);
            }
        };
    }

    private void postSuccess(String json, String type){
        Location location = new Gson().fromJson(json, Location.class);
        if(type.equals(IBEACON)){
            Toast.makeText(getApplicationContext(),"ibeacon" + json,Toast.LENGTH_SHORT).show();
            if(location.x==-1&&location.y==-1){
                setLocation((int)wifiX,(int)wifiY);
            }
            else{
                setLocation((int) location.x, (int) location.y);
            }
        }
        else{
            wifiX=location.x;
            wifiY=location.y;
        }
    }

    private void initBeacon() {
        verifyBluetooth();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons in the background.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @TargetApi(23)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }

                });
                builder.show();
            }
        }
    }

    private void setLocation(int x, int y) {
        //按比例调整 4000*6000 => 200*300
        x/=20;
        y/=20;

        AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) myLocation.getLayoutParams();
        params.x = (x + 50 - 12)* 2;//不知为啥是2倍的关系
        params.y = (y + 50 - 12)* 2;
        myLocation.setLayoutParams(params);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                }
                return;
            }
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                Iterator<Beacon> it = beacons.iterator();
                while (it.hasNext()) {
                    Beacon b = it.next();
                    String uuid = b.getId1().toHexString();//0xfda50693a4e24fb1afcf000000000099
                    uuid = uuid.substring(2, uuid.length());//去除0x
                    uuid = appendSeprator(uuid, " ", 2);//添加空格
                    uuid = uuid.toUpperCase();//转换为大写
                    ibeaconMap.put(uuid, b.getRssi());
                }
            }

        });

        if(isWorking){
            try {
                beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
            } catch (RemoteException e) {
            }
        }
    }

    private void verifyBluetooth() {

        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
//                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
//                    @Override
//                    public void onDismiss(DialogInterface dialog) {
//                        finish();
//                        System.exit(0);
//                    }
//                });
                builder.show();
            }
        } catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
//            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
//
//                @Override
//                public void onDismiss(DialogInterface dialog) {
//                    finish();
//                    System.exit(0);
//                }
//
//            });
            builder.show();

        }

    }

    private String appendSeprator(String srcStr, String seprator, int count) {
        StringBuffer sb = new StringBuffer(srcStr);
        int index = count;
        while (sb.length() > count && index < sb.length() - 1) {
            sb.insert(index, seprator);
            index += count + 1;
        }
        return sb.toString();
    }

    @OnClick(R.id.btn_switch)
    public void onViewClicked() {
        if (isWorking) {
            beaconManager.unbind(this);//关闭ibeacon检测
            handler.removeCallbacks(runnable);//停止检测wifi线程
            btnSwitch.setText("开始定位");//更新button
        } else {
            beaconManager.bind(this);//开启ibeacon检测
            ibeaconMap.clear();
            handler.post(runnable);//启动检测wifi线程
            btnSwitch.setText("结束定位");//更新button
        }

        isWorking = !isWorking;
    }
}
