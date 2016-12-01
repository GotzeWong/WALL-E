package com.a7clk.wall_e_android;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
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
import android.widget.TextView;

import com.a7clk.wall_e_android.model.Config;
import com.a7clk.wall_e_android.model.Order;

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

    private Thread thread;                //執行緒
    private Socket clientSocket;        //客戶端的socket
    private BufferedWriter bw;            //取得網路輸出串流
    private BufferedReader br;            //取得網路輸入串流
    private String tmp;                    //做為接收時的緩存
    private JSONObject json_write,json_read;
    DataOutputStream dos = null;
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

    @OnClick(R.id.initBtn) void init() {
        // TODO call server...
        thread=new Thread(Connection);                //賦予執行緒工作
        thread.start();

    }

    @OnClick(R.id.initBtn) void stop() {
        // TODO call server...
        doOrder(Order.STOP);
    }

    @OnClick(R.id.forwardBtn) void forward() {
        // TODO call server...
        doOrder(Order.FORWARD);
    }


    @OnClick(R.id.backwardBtn) void back() {
        // TODO call server...
        doOrder(Order.BACKWARD);
    }

    @OnClick(R.id.leftBtn) void left() {
        // TODO call server...
        doOrder(Order.LEFT);
    }

    @OnClick(R.id.rightBtn) void right() {
        // TODO call server...
        doOrder(Order.RIGHT);
    }

    @OnClick(R.id.fastBtn) void fast() {
        // TODO call server...
        doOrder(Order.ADD_SPEED);
    }

    @OnClick(R.id.slowBtn) void slow() {
        // TODO call server...
        doOrder(Order.SUB_SPEED);
    }

    @OnClick(R.id.wiFiBtn) void wifi() {
        // TODO call server...

        doOrder(Order.GET_WIFI_INFO);
    }

    @OnClick(R.id.bleBtn) void ble() {
        // TODO call server...

        doOrder(Order.GET_BLUETOOTH);
    }

    public void doOrder(String order){
        if(null != out && clientSocket.isConnected()) {
            out.print(order);
            out.flush();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

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

                out.print(Order.CAR_INIT);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {

//            bw.flush();
            //關閉輸出入串流後,關閉Socket
            //最近在小作品有發現close()這3個時,導致while (clientSocket.isConnected())這個迴圈內的區域錯誤
            //會跳出java.net.SocketException:Socket is closed錯誤,讓catch內的處理再重複執行,如有同樣問題的可以將下面這3行註解掉
//            bw.close();
            if(null != br)
                br.close();
            if(null != clientSocket)
                clientSocket.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
