package com.example.administrator.ncfss;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.lzyzsd.circleprogress.CircleProgress;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;
import com.skyfishjy.library.RippleBackground;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import msg.MsgValue;
import utils.LocalInfor;
import utils.MyFileUtils;
import wifi.APHelper;
import wifi.TCPClient;
import wifi.TCPServer;
import wifi.WifiAdmin;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //所需要申请的权限数组
    private static final String[] permissionsArray = new String[]{
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.WAKE_LOCK
    };
    //还需申请的权限列表
    private List<String> permissionsList = new ArrayList<String>();
    //申请权限后的返回码
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;

    //处理返回的文件地址
    public String startPath = "";   //记住上次地址，作为再次开始的目录
    private static final int FILE_CODE = 0;
    //热点和WiFi操作
    public APHelper mAPHelper = null;
    public TCPServer mTCPServer = null;
    public boolean OpenSocketServerPort = false;   //作为端口是否开启的标志
    public TCPClient mTcpClient = null;
    public WifiAdmin mWifiAdmin = null;

    public String myFolderPath = "";      //app目录
    public String myTempPath = "";        //暂存目录
    public String myFileRevPath = "";     //已接收文件目录


    //点两次返回按键退出程序的时间
    private long mExitTime;
    //按钮变量
    public Button bt_buildConnect;
    public Button bt_joinConnect;
    public Button bt_shareFile;

    //progressBar
    private ProgressBar progressBar;

    //用来记录现在是服务器还是客户端
    public String server_client = "";


    //用来实现水波纹和进度球
    private boolean wavOpen=false;
    //client
    private RippleBackground ripple_client;
    private CircleProgress cirPro_client;
    private TextView tv_myPhoneName;
    private CircleProgress cirPro_server;
    private TextView tv_serverPhoneName;
    //server
    private RippleBackground ripple_server;
    private CircleProgress circleProgress0;
    private TextView tv_phoneName0;
    //处理连接的4个手机
    private CircleProgress circleProgress1;
    private TextView tv_phoneName1;
    private CircleProgress circleProgress2;
    private TextView tv_phoneName2;
    private CircleProgress circleProgress3;
    private TextView tv_phoneName3;
    private CircleProgress circleProgress4;
    private TextView tv_phoneName4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //初始化水波纹和进度球
        initWavCirclePro();

        //检查权限
        checkRequiredPermission(MainActivity.this);
        //文件夹操作，创建数据存储文件夹
        myFolderPath = MyFileUtils.creatFolder(Environment.getExternalStorageDirectory().getPath(), "HHFSS");
        myTempPath = MyFileUtils.creatFolder(myFolderPath, "Temp");
        myFileRevPath = MyFileUtils.creatFolder(myFolderPath, "FileRev");

        //用以打开热点,如果ap已经打开则先关闭
        mAPHelper = new APHelper(MainActivity.this);
//        if (mAPHelper.isApEnabled()) {
//            mAPHelper.setWifiApEnabled(null, false);
//        }
        //用以处理SocketServer
        mTCPServer = new TCPServer(MainActivity.this, myTempPath, myFileRevPath, handler);
        //用以连接Server Socket
        mTcpClient = new TCPClient(MainActivity.this, myTempPath, myFileRevPath, handler);
        //管理wifi
        mWifiAdmin = new WifiAdmin(MainActivity.this);

        byte[] bt_startPath = MyFileUtils.readFile(myFolderPath, "fpStartPath.txt");
        if (bt_startPath == null) {
            startPath = "";
        } else {
            startPath = new String(bt_startPath);
        }

        //等待效果
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        bt_buildConnect = (Button) findViewById(R.id.button_buildConnect);
        bt_buildConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                server_client = "isServer";

                startWav(server_client);
                //打开监听端口
                //mTCPServer.StartServer();
                // progressBar.setVisibility(View.VISIBLE);
                //progressBar.setVisibility(View.GONE);
                if (!OpenSocketServerPort) {
                    mTCPServer.StartServer();
                    OpenSocketServerPort = true;
                }


                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        if (!mAPHelper.isApEnabled()) {
                            //打开热点
                            if (mAPHelper.setWifiApEnabled(APHelper.createWifiCfg(), true)) {
                                //成功
                                Message APOpenSuccess = new Message();
                                APOpenSuccess.what = MsgValue.APOPENSUCCESS;
                                handler.sendMessage(APOpenSuccess);
                                //Toast.makeText(MainActivity.this, "热点开启", Toast.LENGTH_SHORT).show();
                            } else {
                                //失败
                                Message APOpenFailed = new Message();
                                APOpenFailed.what = MsgValue.APOPENFAILED;
                                handler.sendMessage(APOpenFailed);
                                // Toast.makeText(MainActivity.this, "打开热点失败", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            //
                        }
                    }
                }).start();
            }
        });
        bt_joinConnect = (Button) findViewById(R.id.button_joinConnect);
        bt_joinConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



                server_client = "isClient";
                // 如果热点已经打开，需要先关闭热点
                if (mAPHelper.isApEnabled()) {
                    mAPHelper.setWifiApEnabled(null, false);

                    mTCPServer.CloseServer();
                    //当手机当做热点时，自身IP地址为192.168.43.1
                    //GetIpAddress();
                    Toast.makeText(MainActivity.this, "热点关闭", Toast.LENGTH_SHORT).show();
                }

                //开始水波纹
                startWav(server_client);
                //连接WiFi
                connectWifi();

            }
        });


        bt_shareFile = (Button) findViewById(R.id.button_shareFile);
        bt_shareFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //测试水波纹
                //  rippleBackground.startRippleAnimation();
//                //打开文件选择器
                if (startPath.equals("")) {
                    startPath = Environment.getExternalStorageDirectory().getPath();
                }
                Intent i = new Intent(MainActivity.this, FilePickerActivity.class);
                //单选
                // i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                //多选
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
                //设置开始时的路径
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, startPath);
                //i.putExtra(FilePickerActivity.EXTRA_START_PATH, "/storage/emulated/0/DCIM");
                startActivityForResult(i, FILE_CODE);
            }
        });

    }

    /**
     * 处理文件选择器返回的文件地址
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            // Use the provided utility method to parse the result
            List<Uri> files = Utils.getSelectedFilesFromResult(data);
            for (Uri uri : files) {
                File file = Utils.getFileForUri(uri);
                String s = file.getParent();
                //如果有改变则写入新的
                if(!s.equals(startPath)){
                    MyFileUtils.writeToFile(myFolderPath, "fpStartPath.txt", s.getBytes());
                }
                startPath=s;
                //结束循环
                break;
                // Do something with the result...
            }

//            for (Uri uri : files) {
//                File file = Utils.getFileForUri(uri);
//                String fileName = file.getName();
//                Toast.makeText(this, fileName, Toast.LENGTH_SHORT).show();
//                // Do something with the result...
//            }

            sendFiles(files);
        }

    }

    public void sendFiles(final List<Uri> files) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (server_client.equals("isServer")) {
                    mTCPServer.SendFile(files);
                } else if (server_client.equals("isClient")) {
                    mTcpClient.sendFile(files);
                } else {
                    return;
                }

            }
        }).start();
    }

    //连接热点
    public void connectWifi() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //连接指定wifi
                mWifiAdmin.openWifi();
                mWifiAdmin.connectAppointedNet();

                //连接Server Socket
                mTcpClient.connectServer();


            }
        }).start();
    }





    /**
     * 处理消息
     */
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MsgValue.APOPENSUCCESS:
                    Toast.makeText(MainActivity.this, "热点开启", Toast.LENGTH_SHORT).show();
                    break;
                case MsgValue.APOPENFAILED:
                    Toast.makeText(MainActivity.this, "打开热点失败", Toast.LENGTH_SHORT).show();
                    break;

                //处理 TCPClient的信息
                case MsgValue.CONNECT_SF:
                    //连接成功或失败
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case MsgValue.SP_NAME:
                    //接收父手机名字
                    String sp_name = msg.obj.toString();
                    Toast.makeText(MainActivity.this, "已连接到" + msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case MsgValue.SET_SERVER_CIRPRO:
                    //显示server的圆形进度球
                    String serverPhoneName = msg.obj.toString();
                    addCirclePro(cirPro_server,tv_serverPhoneName,serverPhoneName);
                    break;
                case MsgValue.SET_REV_PROGRESS:
                    //显示接收进度
                    int progress=msg.arg1;   //进度
                    //String phoneName=msg.obj.toString();
                    cirPro_client.setProgress(progress);
                    break;
                case MsgValue.SET_SEND_PROGRESS:
                    //显示发送进度
                    int send_progress=msg.arg1;   //进度
                    //String phoneName=msg.obj.toString();
                    cirPro_server.setProgress(send_progress);
                    break;
                case MsgValue.C_REVFINISH:
                    //接收成功
                    //String tempDataPath = msg.obj.toString();
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case MsgValue.C_REV_ERROR_FILELEN:
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;


                //处理TCPServer信息
                case MsgValue.CP_NAME:
                    String cp_name = msg.obj.toString();
                    Toast.makeText(MainActivity.this, msg.obj.toString() + "已连接", Toast.LENGTH_SHORT).show();
                    break;
                case MsgValue.SFOPEN_LISTENER:
                    //开启监听端口成功或失败
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case MsgValue.S_SET_CLIENT_CIRPRO:
                    int clientNum=msg.arg1;
                    String client_phoneName=msg.obj.toString();
                    addCirclePro(circleProgress1,tv_phoneName1,client_phoneName);
//                    if(clientNum==1){
//                        addCirclePro(circleProgress1,tv_phoneName1,client_phoneName);
//                    } else if (clientNum == 2) {
//                        addCirclePro(circleProgress2,tv_phoneName2,client_phoneName);
//                    }else if (clientNum == 3) {
//                        addCirclePro(circleProgress3,tv_phoneName3,client_phoneName);
//                    }else if(clientNum==4){
//                        addCirclePro(circleProgress4,tv_phoneName4,client_phoneName);
//                    }else{
//                        //只支持显示4个client
//                    }
                    break;
                case MsgValue.S_SET_REV_PROGRESS:
                    //设置接收进度
                    int s_progress=msg.arg1;   //进度
                    //String s_phoneName=msg.obj.toString();
                    //本机的进度显示处   0
                    circleProgress0.setProgress(s_progress);
                    break;
                case MsgValue.S_SET_SENT_PROGRESS:
                    //显示发送进度
                    int s_send_progress=msg.arg1;   //进度
                    //int clientNo=msg.arg2;  //是第几个用户
                    //String s_phoneName=msg.obj.toString();
                    circleProgress1.setProgress(s_send_progress);
                    break;
                case MsgValue.S_REVFINISH:
                    //接收成功
                    //String tempDataPath = msg.obj.toString();
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case MsgValue.S_REV_ERROR_FILELEN:
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;

                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    /**
     * 点击两次退出程序
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //若是导航栏打开则关闭导航栏
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }

            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();

            } else {
                //将文件选择器的开始目录写入文件
                MyFileUtils.writeToFile(myFolderPath, "fpStartPath.txt", startPath.getBytes());
                //执行退出操作,并释放资源
                finish();
                //Dalvik VM的本地方法完全退出app
                Process.killProcess(Process.myPid());    //获取PID
                System.exit(0);   //常规java、c#的标准退出法，返回值为0代表正常退出
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
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

    /**
     * 导航栏
     *
     * @param item
     * @return
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.nav_openFolder:
                //打开应用文件夹
                //String path="/storage/emulated/0/DCIM";
                Intent intent = new Intent(MainActivity.this, FilesListViewActivity.class);
                //intent.putExtra("data_path",path);
                intent.putExtra("data_path", myFileRevPath);
                startActivity(intent);
                //FileUtils.openAssignFolder(MainActivity.this, myFolderPath);
                break;
            case R.id.nav_feedback:
                //用户反馈
                Toast.makeText(MainActivity.this, "功能未实现", Toast.LENGTH_SHORT).show();

                break;
            case R.id.nav_softversion:
                /**菜单中“版本”选项的弹出显示版本信息的对话框*/
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("软件版本")
                        .setMessage("版本号：" + BuildConfig.VERSION_CODE + "\n版本名：" + BuildConfig.VERSION_NAME)
                        .setPositiveButton("确定", null)
                        .show();
                break;
            case R.id.nav_softdescribe:
                /**菜单中“软件描述”选项的弹出对话框*/
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("软件描述")
                        .setMessage("这是一个利用网络编码安全方案的文件共享项目。\n网址：https://github.com/WangLei20167/FileSharing")
                        .setPositiveButton("确定", null)
                        .show();
                break;
            case R.id.nav_aboutus:
                //关于我们
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("关于我们")
                        .setMessage("瀚海制作 \n邮箱：1092951104@qq.com")
                        .setPositiveButton("确定", null)
                        .show();
                break;
            default:
                break;
        }

        // DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        // drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * 用来初始化水波纹控件和圆形进度控件
     */
    private void initWavCirclePro() {
        //client
        ripple_client = (RippleBackground) findViewById(R.id.waterWav_client);
        cirPro_client = (CircleProgress) findViewById(R.id.circle_progress_client);
        tv_myPhoneName = (TextView) findViewById(R.id.tv_phone_name_client);
        cirPro_server = (CircleProgress) findViewById(R.id.circle_progress_server);
        tv_serverPhoneName = (TextView) findViewById(R.id.tv_phone_name_server);


        //server
        ripple_server = (RippleBackground) findViewById(R.id.waterWav_server);

        //获取屏幕的宽高
//        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//
//        int width = wm.getDefaultDisplay().getWidth();
//        //int height = wm.getDefaultDisplay().getHeight();
//
//        //设置控件高等于屏幕宽
//        RelativeLayout.LayoutParams parms=(RelativeLayout.LayoutParams)ripple_server.getLayoutParams();
//        parms.height=width;
//        parms.width=width;
//        ripple_server.setLayoutParams(parms);



        //本手机
        circleProgress0 = (CircleProgress) findViewById(R.id.circle_progress0);
        tv_phoneName0 = (TextView) findViewById(R.id.tv_phone_name0);

        //处理连接来的手机
        circleProgress1 = (CircleProgress) findViewById(R.id.circle_progress1);
        tv_phoneName1 = (TextView) findViewById(R.id.tv_phone_name1);

        circleProgress2 = (CircleProgress) findViewById(R.id.circle_progress2);
        tv_phoneName2 = (TextView) findViewById(R.id.tv_phone_name2);

        circleProgress3 = (CircleProgress) findViewById(R.id.circle_progress3);
        tv_phoneName3 = (TextView) findViewById(R.id.tv_phone_name3);

        circleProgress4 = (CircleProgress) findViewById(R.id.circle_progress4);
        tv_phoneName4 = (TextView) findViewById(R.id.tv_phone_name4);

    }
    /**
     * 开始水波纹
     * @param type
     */
    public void startWav(String type) {
        //开始之前首先关闭正在执行的水波纹
        stopWav(checkWavState());
        if (type.equals("isClient")) {
            ripple_client.setVisibility(View.VISIBLE);
            cirPro_client.setVisibility(View.VISIBLE);
            tv_myPhoneName.setVisibility(View.VISIBLE);
            cirPro_client.setProgress(0);
            tv_myPhoneName.setText(LocalInfor.getPhoneModel());
            //开始水波纹
            ripple_client.startRippleAnimation();
        }else if(type.equals("isServer")){
            ripple_server.setVisibility(View.VISIBLE);
            circleProgress0.setVisibility(View.VISIBLE);
            tv_phoneName0.setVisibility(View.VISIBLE);
            circleProgress0.setProgress(0);
            tv_phoneName0.setText(LocalInfor.getPhoneModel());
            ripple_server.startRippleAnimation();
        }
    }

    /**
     * 结束水波纹
     * @param type
     */
    public void stopWav(String type) {
        if (type.equals("isClient")) {
            ripple_client.stopRippleAnimation();
            ripple_client.setVisibility(View.GONE);

            cirPro_client.setVisibility(View.GONE);
            tv_myPhoneName.setVisibility(View.GONE);

            tv_serverPhoneName.setText("");
            cirPro_server.setVisibility(View.GONE);
            tv_serverPhoneName.setVisibility(View.GONE);

        }else if(type.equals("isServer")){
            ripple_server.stopRippleAnimation();

            ripple_server.setVisibility(View.GONE);
            circleProgress0.setVisibility(View.GONE);
            tv_phoneName0.setVisibility(View.GONE);

            circleProgress1.setVisibility(View.GONE);
            tv_phoneName1.setVisibility(View.GONE);

            circleProgress2.setVisibility(View.GONE);
            tv_phoneName2.setVisibility(View.GONE);

            circleProgress3.setVisibility(View.GONE);
            tv_phoneName3.setVisibility(View.GONE);

            circleProgress4.setVisibility(View.GONE);
            tv_phoneName4.setVisibility(View.GONE);
        }else{

        }
    }

    /**
     * 检查wav的状态
     * @return
     */
    public String checkWavState(){
        if(ripple_client.isRippleAnimationRunning()){
            return "isClient";
        }else if(ripple_server.isRippleAnimationRunning()){
            return "isServer";
        }else{
            return "";
        }
    }

    /**
     * 添加圆形进度控件
     * @param circleProgress
     * @param tv
     * @param phoneName
     */
    public void addCirclePro(CircleProgress circleProgress,TextView tv,String phoneName){
        circleProgress.setProgress(0);
        tv.setText(phoneName);
        circleProgress.setVisibility(View.VISIBLE);
        tv.setVisibility(View.VISIBLE);
    }

    /**
     * 删除指定的圆形进度控件
     * @param circleProgress
     * @param tv
     */
    public void deleteCirPro(CircleProgress circleProgress,TextView tv){
        circleProgress.setVisibility(View.GONE);
        tv.setVisibility(View.GONE);
    }



    //检查和申请权限
    private void checkRequiredPermission(final Activity activity) {
        for (String permission : permissionsArray) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permission);
            }
        }
        //若是permissionsList为空，则说明所有权限已用
        if (permissionsList.size() == 0) {
            return;
        }
        ActivityCompat.requestPermissions(activity, permissionsList.toArray(new String[permissionsList.size()]), REQUEST_CODE_ASK_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        //Toast.makeText(MainActivity.this, "做一些申请成功的权限对应的事！" + permissions[i], Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "权限被拒绝： " + permissions[i], Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


}
