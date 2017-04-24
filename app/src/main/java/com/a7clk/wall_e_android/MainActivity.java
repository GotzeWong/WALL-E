package com.a7clk.wall_e_android;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.a7clk.wall_e_android.PDR.StepDetector;
import com.a7clk.wall_e_android.PDR.StepListener;
import com.a7clk.wall_e_android.model.Command;
import com.a7clk.wall_e_android.model.Config;
import com.a7clk.wall_e_android.ui.JoystickView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private Thread thread;                //執行緒
    private Socket clientSocket;        //客戶端的socket

    private BufferedReader br;            //取得網路輸入串流
    private String tmp;                    //做為接收時的緩存
    PrintWriter out = null;

    @Bind(R.id.initBtn)
    Button initBtn;

    @Bind(R.id.forwardBtn)
    Button forwardBtn;

    @Bind(R.id.backwardBtn)
    Button backwardBtn;

    @Bind(R.id.rightBtn)
    Button rightBtn;

    @Bind(R.id.leftBtn)
    Button leftBtn;

    @Bind(R.id.recvTextView)
    TextView recvTextView;

    @Bind(R.id.speedSeekBar)
    SeekBar speedSeekBar;

    @Bind(R.id.volumSeekBar)
    SeekBar volumSeekBar;

    @Bind(R.id.joystickView)
    JoystickView joystickView;

    @Bind(R.id.oderTextView)
    TextView oderTextView;

    private SensorManager mSensorManager;
    private Sensor mStepSensor;
    private Sensor mAccSensor;
    private Sensor mStepCountSensor;

    private static int mSteps = 0;

    private static boolean onPressed = false;
    private static String currentCommand = Command.STOP;
    private static String lastCommand = Command.STOP;

    @OnClick(R.id.initBtn) void init() {
        // TODO call server...
        thread=new Thread(Connection);                //賦予執行緒工作
        thread.start();
    }

    @OnClick(R.id.stopBtn) void stop() {
        // TODO call server...
        doCommand(Command.STOP);
    }

    @OnClick(R.id.forwardBtn) void forward() {
        // TODO call server...
        doCommand(Command.FORWARD_SHORT_TIME);
    }

    @OnClick(R.id.backwardBtn) void back() {
        // TODO call server...
        doCommand(Command.BACKWARD_SHORT_TIME);
    }

    @OnClick(R.id.leftBtn) void left() {
        // TODO call server...
        doCommand(Command.LEFT_SHORT_TIME);
    }

    @OnClick(R.id.rightBtn) void right() {
        // TODO call server...
        doCommand(Command.RIGHT_SHORT_TIME);
    }

    @OnClick(R.id.fastBtn) void fast() {
        // TODO call server...
        doCommand(Command.ADD_SPEED);
    }

    @OnClick(R.id.slowBtn) void slow() {
        // TODO call server...
        doCommand(Command.SUB_SPEED);
    }

    public void setSpeed(long speed) {
        // TODO call server...
        doCommand(Command.SPEED+speed);
    }

    public void setVolum(long volum) {
        // TODO call server...
        doCommand(Command.VOLUM+volum);
    }

    @OnClick(R.id.wiFiBtn) void wifi() {
        // TODO call server...

        doCommand(Command.GET_WIFI_INFO);
    }

    @OnClick(R.id.bleBtn) void ble() {
        // TODO call server...

        doCommand(Command.GET_BLUETOOTH);
    }

    public void doCommand(String command){
        Log.i("the command to car:",command);
        if(null != out && clientSocket.isConnected()) {
            out.print(command);
            out.flush();
        }
        oderTextView.setText(command);
    }

    public void doCommand(){
        Log.i("the command to car:",currentCommand);
        if(null != out && clientSocket.isConnected() && !lastCommand.equals(currentCommand)) {
            out.print(currentCommand);
            out.flush();
            lastCommand = currentCommand;
            oderTextView.setText(currentCommand+"...");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        mAccSensor= mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mStepCountSensor= mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        StepDetector stepDetector = new StepDetector();
        stepDetector.addStepListener(new StepListener() {
            @Override
            public void onStep() {
                Log.i(TAG,
                    "onStep. Total step count: " + ++mSteps);
                    doCommand(Command.FORWARD_SHORT_TIME);
            }

            @Override
            public void passValue() {
                Log.i(TAG,
                        "passValue: " + ++mSteps);
            }
        });
        mSensorManager.registerListener(stepDetector, mAccSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private long _progress = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                _progress = progress;//1~100
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setSpeed(_progress);
            }
        });

        volumSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private long _progress = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                _progress = progress;//1~100
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setVolum(_progress);
            }
        });

        joystickView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    //当手指按下的时候
                    onPressed = true;
                    doCommand();
                }
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    //当手指离开的时候
                    onPressed = false;
                    currentCommand = Command.STOP;
                    doCommand();
                }
                return false;
            }
        });


        joystickView.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {

            @Override
            public void onValueChanged(int angle, int power, int direction) {
                // TODO Auto-generated method stub
                //angleTextView.setText(" " + String.valueOf(angle) + "°");
                //powerTextView.setText(" " + String.valueOf(power) + "%");A
                Log.i("the ANGLE:",angle+"");

                //doCommand(Command.ANGLE+angle);
                switch (direction) {
                    case JoystickView.FRONT:
                        if(onPressed)
                            currentCommand = Command.FORWARD;
                        else
                            doCommand(Command.FORWARD_SHORT_TIME);
                        break;
                    case JoystickView.FRONT_RIGHT:
                        break;
                    case JoystickView.RIGHT:
                        if(onPressed)
                            currentCommand = Command.RIGHT;
                        else
                            doCommand(Command.RIGHT_SHORT_TIME);
                        break;
                    case JoystickView.RIGHT_BOTTOM:
                        break;
                    case JoystickView.BOTTOM:
                        if(onPressed)
                            currentCommand = Command.BACKWARD;
                        else
                            doCommand(Command.BACKWARD_SHORT_TIME);
                        break;
                    case JoystickView.BOTTOM_LEFT:
                        break;
                    case JoystickView.LEFT:
                        if(onPressed)
                            currentCommand = Command.LEFT;
                        else
                            doCommand(Command.LEFT_SHORT_TIME);
                        break;
                    case JoystickView.LEFT_FRONT:
                        break;
                    default:
                        break;
                }
                doCommand();

            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //連結socket伺服器做傳送與接收
    private Runnable Connection=new Runnable(){
        @Override
        public void run() {
            // TODO Auto-generated method stub
            try{
                InetAddress serverIp = InetAddress.getByName(Config.CAR_IP);
                clientSocket = new Socket(InetAddress.getByName(Config.CAR_IP), Config.CAR_PORT);

                br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                        clientSocket.getOutputStream())), true);

                out.print(Command.CAR_INIT);
                out.flush();

                while (clientSocket.isConnected()) {
                    // 取得網路訊息
                    tmp += br.readLine();    //宣告一個緩衝,從br串流讀取值
                    // 如果不是空訊息
                    if(tmp!=null){
                        Log.i("SOCKET RESPONSE", tmp);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recvTextView.setText(tmp);
                            }
                        });
                    }
                }
            }catch(Exception e){
                //當斷線時會跳到catch,可以在這裡寫上斷開連線後的處理
                e.printStackTrace();
                finish();    //當斷線時自動關閉房間
            }
        }
    };

    private void closeSocket() {
        try {

//            bw.flush();
            //關閉輸出入串流後,關閉Socket
            //最近在小作品有發現close()這3個時,導致while (clientSocket.isConnected())這個迴圈內的區域錯誤
            //會跳出java.net.SocketException:Socket is closed錯誤,讓catch內的處理再重複執行,如有同樣問題的可以將下面這3行註解掉
//            bw.close();

            if(null != out) {
                out.flush();
                out.close();
            }
            if(null != br)
                br.close();
            if(null != clientSocket)
                clientSocket.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        private int mStep;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.i("SensorEventListener","Accuracy Change: " + accuracy);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.values[0] == 1.0f) {
                doCommand(Command.FORWARD);
                Log.i("SensorEventListener","Sensor Change FORWARD");
            }else{
                doCommand(Command.STOP);
                Log.i("SensorEventListener","Sensor Change STOP");
            }
        }
    };

    /*
     * SensorEventListener接口的实现，需要实现两个方法
     * 方法1 onSensorChanged 当数据变化的时候被触发调用
     * 方法2 onAccuracyChanged 当获得数据的精度发生变化的时候被调用，比如突然无法获得数据时
     * */
    final SensorEventListener myAccelerometerListener = new SensorEventListener(){

        //复写onSensorChanged方法
        public void onSensorChanged(SensorEvent sensorEvent){
            if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                Log.i(TAG,"onSensorChanged");

                //图解中已经解释三个值的含义
                float X_lateral = sensorEvent.values[0];
                float Y_longitudinal = sensorEvent.values[1];
                float Z_vertical = sensorEvent.values[2];
                Log.i(TAG,"\n heading "+X_lateral);
                Log.i(TAG,"\n pitch "+Y_longitudinal);
                Log.i(TAG,"\n roll "+Z_vertical);
            }
        }
        //复写onAccuracyChanged方法
        public void onAccuracyChanged(Sensor sensor , int accuracy){
            Log.i(TAG, "onAccuracyChanged");
        }
    };

    final SensorEventListener myStepCounterListener = new SensorEventListener(){

        //复写onSensorChanged方法
        public void onSensorChanged(SensorEvent sensorEvent){
            if(sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER){
                mSteps += sensorEvent.values.length;

                Log.i(TAG,
                        "New step detected by STEP_DETECTOR sensor. Total step count: " + mSteps);
            }
        }
        //复写onAccuracyChanged方法
        public void onAccuracyChanged(Sensor sensor , int accuracy){
            Log.i(TAG, "onAccuracyChanged");
        }
    };

    protected void onResume() {
        mSensorManager.registerListener(mSensorEventListener, mStepSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
//        mSensorManager.registerListener(myAccelerometerListener, mAccSensor,
//                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(myStepCounterListener, mStepCountSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(mSensorEventListener);
        mSensorManager.unregisterListener(myAccelerometerListener);
        Log.i("SensorEventListener","unregisterListener");

        closeSocket();
    }

}
