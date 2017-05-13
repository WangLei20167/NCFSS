package com.example.administrator.ncfss;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import Utils.AppFolder;
import msg.MsgValue;
import wifi.APHelper;
import wifi.TCPClient;
import wifi.TCPServer;
import wifi.WifiAdmin;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //所需要申请的权限数组
    private static final String[] permissionsArray = new String[]{
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
    private static final int FILE_CODE = 0;
    //热点和WiFi操作
    public APHelper mAPHelper = null;
    public TCPServer mTCPServer = null;
    public boolean OpenSocketServerPort = false;   //作为端口是否开启的标志
    public TCPClient mTcpClient = null;
    public AppFolder mAppFolder = null;        //用以文件操作
    public String myFolderPath = "";      //app目录
    public String myTempPath = "";        //暂存目录
    public String myFileRevPath = "";     //已接收文件目录


    //点两次返回按键退出程序的时间
    private long mExitTime;
    //按钮变量
    public Button bt_buildConnect;
    public Button bt_joinConnect;
    public Button bt_shareFile;


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

        //检查权限
        checkRequiredPermission(MainActivity.this);
        //文件操作
        mAppFolder = new AppFolder();
        if (mAppFolder.createPath("hanhaiKuaiChuan")) {
            //记录下三个文件夹的路径
            myFolderPath = mAppFolder.FolderPath;
            myTempPath = mAppFolder.TempPath;
            myFileRevPath = mAppFolder.FileRevPath;

            //用以打开热点,如果ap已经打开则先关闭
            mAPHelper = new APHelper(MainActivity.this);
            if (mAPHelper.isApEnabled()) {
                mAPHelper.setWifiApEnabled(null, false);
            }
            //用以处理SocketServer
            mTCPServer = new TCPServer(MainActivity.this, myTempPath, myFileRevPath);
            //用以连接Server Socket
            mTcpClient = new TCPClient(MainActivity.this, myTempPath, myFileRevPath, handler);
        } else {
            Toast.makeText(this, "文件夹创建失败,app无法正常使用", Toast.LENGTH_SHORT).show();
        }

        bt_buildConnect = (Button) findViewById(R.id.button_buildConnect);
        bt_buildConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //打开监听端口
                //mTCPServer.StartServer();
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
                                Message APOpenSuceess = new Message();
                                APOpenSuceess.what = MsgValue.APOPENSUCCESS;
                                handler.sendMessage(APOpenSuceess);
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

                // 如果热点已经打开，需要先关闭热点
                if (mAPHelper.isApEnabled()) {
                    mAPHelper.setWifiApEnabled(null, false);

                    mTCPServer.CloseServer();
                    //当手机当做热点时，自身IP地址为192.168.43.1
                    //GetIpAddress();
                    Toast.makeText(MainActivity.this, "热点关闭", Toast.LENGTH_SHORT).show();
                }
                //连接WiFi
                connectWifi();

            }
        });


        bt_shareFile = (Button) findViewById(R.id.button_shareFile);
        bt_shareFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //打开文件选择器// This always works
                Intent i = new Intent(MainActivity.this, FilePickerActivity.class);

                //单选
                // i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                //多选
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
                //设置开始时的路径
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
                //i.putExtra(FilePickerActivity.EXTRA_START_PATH, "/storage/emulated/0/DCIM");
                startActivityForResult(i, FILE_CODE);
            }
        });

    }

    /**
     * 处理文件选择器返回的文件地址
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
                String fileName=file.getName();
                Toast.makeText(this, fileName, Toast.LENGTH_SHORT).show();
                // Do something with the result...
            }
        }

    }

    //连接热点
    public void connectWifi() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //连接指定wifi
                WifiAdmin mWifiAdmin = new WifiAdmin(MainActivity.this);
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
                case MsgValue.REVFINISH:
                    //接收数据成功，开始解码操作
                    int fileNum = msg.arg1;
                    String tempDataPath = msg.obj.toString();
                    Toast.makeText(MainActivity.this, "接收数据成功开始解码", Toast.LENGTH_SHORT).show();
                    break;
                case MsgValue.APOPENSUCCESS:
                    Toast.makeText(MainActivity.this, "热点开启", Toast.LENGTH_SHORT).show();
                    break;
                case MsgValue.APOPENFAILED:
                    Toast.makeText(MainActivity.this, "打开热点失败", Toast.LENGTH_SHORT).show();
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
                //执行退出操作,并释放资源
                finish();
                //Dalvik VM的本地方法完全退出app
                android.os.Process.killProcess(android.os.Process.myPid());    //获取PID
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
                String path="/storage/emulated/0/DCIM";
                Intent intent=new Intent(MainActivity.this,FilesListViewActivity.class);
                intent.putExtra("data_path",path);
                //intent.putExtra("data_path",myFileRevPath);
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
                        Toast.makeText(MainActivity.this, "做一些申请成功的权限对应的事！" + permissions[i], Toast.LENGTH_SHORT).show();
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
