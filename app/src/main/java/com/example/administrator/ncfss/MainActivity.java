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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.lzyzsd.circleprogress.CircleProgress;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;
import com.skyfishjy.library.RippleBackground;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import msg.MsgValue;
import nc.ConstantValue;
import nc.EncodeFile;
import nc.MyEncodeFile;
import nc.NCUtil;
import nc.PieceFile;
import utils.LocalInfor;
import utils.MyFileUtils;
import utils.SelectFileDialog;
import utils.SettingDialog;
import wifi.APHelper;
import wifi.Constant;
import wifi.MyCircleProgress;
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
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
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
    //文件名
    public TextView tv_fileName;
    //文件个数
    public TextView tv_cur_total_num;
    //按钮变量
    public Button bt_buildConnect;
    public Button bt_joinConnect;
    public Button bt_shareFile;

    //等待的转圈效果
    //  private ProgressBar progressBar;

    //用来记录现在是服务器还是客户端
    public String server_client = "";


    //用来实现水波纹和进度球
    //private boolean wavOpen = false;
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


    //用以操作进度球   注意界面中只设置了4个圆形进度球的位置
    private List<MyCircleProgress> myClientProgressList = new ArrayList<MyCircleProgress>();
    private List<CircleProgress> client_progress_list = new ArrayList<CircleProgress>();
    private List<TextView> tv_client_phoneName_list = new ArrayList<TextView>();


    //用于网络编码的变量，设置一个初值
    private int N = 4;
    private int K = 4;
    private int SFN = 4;  //send file num
    //private ArrayList<MyEncodeFile> myEncodeFiles=new ArrayList<MyEncodeFile>();
    // private MyEncodeFile myEncodeFile = null;
    //用以管理编码文件
    private EncodeFile myEncodeFile = null;

    //用以管理所有编码文件的配置信息
    private JSONArray json_all_file_config = new JSONArray();
    private ArrayList<EncodeFile> myEncodeFiles = new ArrayList<EncodeFile>();

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
        myFolderPath = MyFileUtils.creatFolder(Environment.getExternalStorageDirectory().getPath(), ConstantValue.APP_FOLDER);
        myTempPath = MyFileUtils.creatFolder(myFolderPath, ConstantValue.TEMP);
        myFileRevPath = MyFileUtils.creatFolder(myFolderPath, ConstantValue.FILE_REV);


        //用以打开热点,如果ap已经打开则先关闭
        mAPHelper = new APHelper(MainActivity.this);
        mTCPServer = new TCPServer(MainActivity.this, myTempPath, myFileRevPath, handler);
        //用以连接Server Socket
        mTcpClient = new TCPClient(MainActivity.this, myTempPath, myFileRevPath, handler);
        //管理wifi
        mWifiAdmin = new WifiAdmin(MainActivity.this, handler);


        controlLocalData();
        byte[] bt_startPath = MyFileUtils.readFile(myFolderPath, ConstantValue.START_SELECT_PATH);
        if (bt_startPath == null) {
            startPath = "";
        } else {
            startPath = new String(bt_startPath);
        }


        tv_fileName = (TextView) findViewById(R.id.textView_fileName);
        tv_cur_total_num = (TextView) findViewById(R.id.textView_cur_total);

        //等待效果
        //progressBar = (ProgressBar) findViewById(R.id.progressBar);
        bt_buildConnect = (Button) findViewById(R.id.button_buildConnect);
        bt_buildConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTCPServer.getLocalData() == null) {
                    bt_shareFile.performClick();
                    SendMessage(MsgValue.TELL_ME_SOME_INFOR, 0, 0, "待发送文件为空");
                    return;
                }
                //如果已作为client连接到了socket  则先断开
                if (server_client.equals(Constant.isClient) && mTcpClient.getSocket_flag()) {
                    //断开
                    mTcpClient.disconnectServer();
                }


                server_client = Constant.isServer;

                startWav(server_client);
                // progressBar.setVisibility(View.VISIBLE);
                //progressBar.setVisibility(View.GONE);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //关闭WiFi
                        mWifiAdmin.closeWifi();

                        if (!mAPHelper.isApEnabled()) {
                            //打开热点
                            if (mAPHelper.setWifiApEnabled(APHelper.createWifiCfg(), true)) {
                                //成功
                                //打开监听端口
                                if (!OpenSocketServerPort) {
                                    mTCPServer.StartServer();
                                    OpenSocketServerPort = true;
                                }
                                SendMessage(MsgValue.TELL_ME_SOME_INFOR,0,0,"热点开启成功");
                            } else {
                                //失败
                                SendMessage(MsgValue.TELL_ME_SOME_INFOR,0,0,"热点开启失败");
                            }
                        } else {
                            //
                        }
                        // String s= LocalInfor.getHostIP();
                    }
                }).start();


            }
        });
        bt_joinConnect = (Button) findViewById(R.id.button_joinConnect);
        bt_joinConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //连接WiFi    所有操作封装到一起
                search_connect_wifi();
               // myEncodeFile.try2decode();

            }
        });


        bt_shareFile = (Button) findViewById(R.id.button_shareFile);
        bt_shareFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //测试水波纹
                //  rippleBackground.startRippleAnimation();
                ArrayList<File> folders = MyFileUtils.getListFolders(myTempPath);
                if (folders.size() == 0) {
                    openSelectFile();
                } else {
                    showSelectFileDialog();
                }

            }
        });


        //刚开始就处于搜索状态
        //连接WiFi    所有操作封装到一起
        search_connect_wifi();

    }

    /**
     * 恢复对本地数据的控制
     */
    public void controlLocalData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<File> folders = MyFileUtils.getListFolders(myTempPath);
                int size = folders.size();
                for (int i = 0; i < size; ++i) {
                    File folder=folders.get(i);
                    String json_file_path=folder.getPath() + File.separator + "json.txt";
                    File file = new File(json_file_path);
                    if(!file.exists()){
                        SendMessage(MsgValue.TELL_ME_SOME_INFOR,0,0,json_file_path+"文件损坏");
                        //删除损坏的文件
                        MyFileUtils.deleteAllFile(folder.getPath(),true);
                        continue;
                    }
                    EncodeFile encodeFile = EncodeFile.parse_JSON_File(file);
                    if (encodeFile == null) {
                        continue;
                    }
                    encodeFile.getLocalData();
                    myEncodeFiles.add(encodeFile);
                }

            }
        }).start();

    }

    public void openSelectFile() {
        //打开文件选择器
        if (startPath.equals("")) {
            startPath = Environment.getExternalStorageDirectory().getPath();
        }
        Intent i = new Intent(MainActivity.this, FilePickerActivity.class);
        //单选
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        //多选
        //i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
        //设置开始时的路径
        i.putExtra(FilePickerActivity.EXTRA_START_PATH, startPath);
        //i.putExtra(FilePickerActivity.EXTRA_START_PATH, "/storage/emulated/0/DCIM");
        startActivityForResult(i, FILE_CODE);
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

            ArrayList<File> fileList = new ArrayList<File>();
            //使用循环把文件放入list
            for (Uri uri : files) {
                File file = Utils.getFileForUri(uri);
                fileList.add(file);
            }
            final File file = fileList.get(0);
            String s = file.getParent();
            //如果有改变则写入新的
            if (!s.equals(startPath)) {
                MyFileUtils.writeToFile(myFolderPath, ConstantValue.START_SELECT_PATH, s.getBytes());
            }
            startPath = s;
            final String fileName = file.getName();
            tv_fileName.setText("文件名：" + fileName);
            final EncodeFile encodeFile = new EncodeFile(myTempPath, fileName, K);
            //encodeFile.setOriginFilePath(file.getPath());
            new Thread(new Runnable() {
                @Override
                public void run() {

                    NCUtil.encode_file(file, encodeFile);
                    //再编码
//                    for (PieceFile pieceFile : encodeFile.getMyPiecesFiles()) {
//                        NCUtil.re_encode_file(pieceFile);
//                    }
                    myEncodeFiles.add(encodeFile);
                    mTCPServer.setLocalData(encodeFile);
                    SendMessage(MsgValue.TELL_ME_SOME_INFOR, 0, 0, fileName + "编码完成");
                    int curNum = encodeFile.getCurrentSmallPieceNum();
                    int totalNum = encodeFile.getTotalSmallPieceNum();
                    SendMessage(MsgValue.SET_CUR_TOTAL_TV, curNum, totalNum, null);
                }
            }).start();

        }

    }


    //连接ServerSocket
    public void search_connect_wifi() {
        //若是已经是服务端，则告知客户端已关闭
        if (server_client.equals(Constant.isServer) && mTCPServer.getServerSocketState()) {
            //断开
            mTCPServer.CloseServer();
        } else if (server_client.equals(Constant.isClient) && mTcpClient.getSocket_flag()) {
            //若已经是客户端，则告知服务端已关闭
            mTcpClient.disconnectServer();
        }
        server_client = Constant.isClient;
        //开始水波纹
        startWav(server_client);

        //清空
        myClientProgressList.clear();

        new Thread(new Runnable() {
            @Override
            public void run() {
                //如果已作为client连接到了socket  则先断开

                // 如果热点已经打开，需要先关闭热点
                if (mAPHelper.isApEnabled()) {
                    mAPHelper.setWifiApEnabled(null, false);
                    //关闭监听端口
                    mTCPServer.CloseServer();
                    //当手机当做热点时，自身IP地址为192.168.43.1
                    //GetIpAddress();
                    // Toast.makeText(MainActivity.this, "热点关闭", Toast.LENGTH_SHORT).show();
                }


                mWifiAdmin.openWifi();
                String bssid = mWifiAdmin.searchWifi(Constant.HOST_SPOT_SSID);
                if (!bssid.equals("")) {
                    //如果WiFi已经连接上别的WiFi   则先断开
//                    if(mWifiAdmin.isWifiConnected()&&(!mWifiAdmin.getBSSID().equals(bssid))){
//                        int netId=mWifiAdmin.getNetworkId();
//                        mWifiAdmin.disconnectWifi(netId);
//                    }
                    //连接到指定网络
                    mWifiAdmin.addNetwork(mWifiAdmin.CreateWifiInfo(bssid, Constant.HOST_SPOT_SSID, Constant.HOST_SPOT_PASS_WORD, 3));
                } else {
                    //说明没找到指定wifi
                    return;
                }
                while (!mWifiAdmin.isWifiConnected()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //连接到指定网络
                    // mWifiAdmin.addNetwork(mWifiAdmin.CreateWifiInfo(bssid,Constant.HOST_SPOT_SSID, Constant.HOST_SPOT_PASS_WORD, 3));
                }
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
                case MsgValue.TELL_ME_SOME_INFOR:
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case MsgValue.SET_CUR_TOTAL_TV:
                    int curNum = msg.arg1;
                    int totalNum = msg.arg2;
                    tv_cur_total_num.setText("已有/共需文件片数：" + curNum + "/" + totalNum);
                    break;

                //处理 TCPClient的信息
                case MsgValue.CONNECT_SF:
                    //连接成功或失败
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;

                case MsgValue.SET_SERVER_CIRPRO:
                    //显示server的圆形进度球
                    String serverPhoneName = msg.obj.toString();
                    Toast.makeText(MainActivity.this, "已连接到" + serverPhoneName, Toast.LENGTH_SHORT).show();
                    addCirclePro(cirPro_server, tv_serverPhoneName, serverPhoneName);
//                    tv_fileName.setText("文件名：" + "Never Say Never.mp4");
//                    cirPro_client.setProgress((int)((3.0/16)*100));
//                    tv_cur_total_num.setText("已有/共需文件片数：" + 3 + "/" + 16);
                    break;
                //接收到配置文件
                case MsgValue.C_PARSE_JSON_FILR:
                    break;
                case MsgValue.C_CREATE_ENCODE_FILE_FOLDER:
                    break;
                case MsgValue.SET_REV_PROGRESS:
                    //显示接收进度
                    int progress = msg.arg1;   //进度
                    //String phoneName=msg.obj.toString();
                    cirPro_client.setProgress(progress);
                    break;
                case MsgValue.SET_SEND_PROGRESS:
                    //显示发送进度
                    int send_progress = msg.arg1;   //进度
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
                case MsgValue.C_SOCKET_END_FLAG:
                    //关闭服务端的进度球
                    deleteCirPro(cirPro_server, tv_serverPhoneName);
                    //重新搜索wifi
                    search_connect_wifi();
                    break;
                case MsgValue.C_REV_ALL_FINISH:
                    //接收数据完毕，尝试解码
                    String _folder_name = msg.obj.toString();
                    break;


                //处理TCPServer信息
                case MsgValue.CP_NAME:
                    //处理连接来的手机名信息,并设置进度球
                    String cp_name_ip = msg.obj.toString();
                    String cp_name = "";
                    String cp_ip = "";
                    String[] split = cp_name_ip.split("#");
                    int flag = 1;
                    for (String val : split) {
                        if (flag == 1) {
                            cp_name = val;
                            ++flag;
                        } else if (flag == 2) {
                            cp_ip = val;
                            ++flag;
                        }
                    }
                    //设置进度球
                    setClientProgress(cp_name, cp_ip);
                    //circleProgress0.setProgress(100);
                    //向连接上的客户端发送FileRev中的所有文件
                    final String _cp_ip = cp_ip;
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            //  mTCPServer.SendFile(_cp_ip, myFileRevPath);
//                            if (myEncodeFile != null) {
//                                mTCPServer.SendFile(_cp_ip, myEncodeFile, SFN);
//                            }
//                        }
//                    }).start();

                    Toast.makeText(MainActivity.this, cp_name + "已连接", Toast.LENGTH_SHORT).show();
                    break;
                case MsgValue.S_CREATE_ENCODE_FILE_FOLDER:
                    //不用做处理   myEncodeFile对象和之前的一样

                    break;
                case MsgValue.SFOPEN_LISTENER:
                    //开启监听端口成功或失败
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;

                case MsgValue.S_SET_REV_PROGRESS:
                    //设置接收进度
                    int s_progress = msg.arg1;   //进度

                    //String s_phoneName=msg.obj.toString();
                    //本机的进度显示处   0
                    circleProgress0.setProgress(s_progress);
                    break;
                case MsgValue.S_SET_SENT_PROGRESS:
                    //显示发送进度
                    int s_send_progress = msg.arg1;   //进度
                    String s_c_ip = msg.obj.toString();
                    //int clientNo=msg.arg2;  //是第几个用户
                    //String s_phoneName=msg.obj.toString();
                    //设置该client的IP
                    setMyClientProgressNum(s_c_ip, s_send_progress);
                    // circleProgress1.setProgress(s_send_progress);
                    break;
                case MsgValue.S_REVFINISH:
                    //接收成功
                    //String tempDataPath = msg.obj.toString();
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case MsgValue.S_REV_ERROR_FILELEN:
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case MsgValue.S_SOCET_END_FLAG:
                    String socket_ip = msg.obj.toString();

                    clearClientProgress(socket_ip);
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

                if (server_client.equals(Constant.isClient) && mTcpClient.getSocket_flag()) {
                    //若已经是客户端，则告知服务端已关闭
                    mTcpClient.disconnectServer();
                } else if (server_client.equals(Constant.isServer) && mTCPServer.getServerSocketState()) {
                    //若已经是服务端，则告知客户端已关闭
                    mTCPServer.CloseServer();
                }

                // 如果热点已经打开，需要先关闭热点
                if (mAPHelper.isApEnabled()) {
                    mAPHelper.setWifiApEnabled(null, false);
                    //发送关闭信息，并关闭监听端口
                    mTCPServer.CloseServer();
                    mTCPServer.stopListening();
                }
                mWifiAdmin.resetWifi();
                //将文件选择器的开始目录写入文件
                MyFileUtils.writeToFile(myFolderPath, ConstantValue.START_SELECT_PATH, startPath.getBytes());
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
            showSettingDialog();
            return true;
        } else if (id == R.id.action_description) {
            /**菜单中“软件描述”选项的弹出对话框*/
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("参数说明")
                    .setMessage("N: 生成编码文件的个数\nK: 编码后需要几个文件可以解码\nSFN: 每次发送的编码文件个数")
                    .setPositiveButton("确定", null)
                    .show();
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
                intent.putExtra("data_path", myFolderPath);
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
                        .setMessage("这是一个利用网络编码安全方案的文件共享项目。\n网址：https://github.com/WangLei20167/NCFSS")
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


    // 设置弹窗

    /**
     * 获取设置弹窗中的值
     */
    private void showSettingDialog() {
        final SettingDialog settingDialog = new SettingDialog(MainActivity.this);
        settingDialog.initNum(N, K, SFN);
        settingDialog.setOnPositiveListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int n = settingDialog.getEt_N();
                int k = settingDialog.getEt_K();
                int sfn = settingDialog.getEt_SFN();
                //dosomething youself
                if (n > 10) {
                    Toast.makeText(MainActivity.this, "N值取值范围1-10", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (k > n) {
                    Toast.makeText(MainActivity.this, "K值不可大于N", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (sfn > k) {
                    Toast.makeText(MainActivity.this, "SFN值不可大于K", Toast.LENGTH_SHORT).show();
                    return;
                }
                //赋值
                N = n;
                K = k;
                SFN = sfn;

                settingDialog.dismiss();
            }
        });
        settingDialog.setOnNegativeListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingDialog.dismiss();
            }
        });
        settingDialog.show();
    }

    /**
     * 选择文件弹窗
     */
    private void showSelectFileDialog() {
        final SelectFileDialog selectFileDialog = new SelectFileDialog(MainActivity.this, myTempPath);

        selectFileDialog.setOnPositiveListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String folderName = selectFileDialog.getResult_folder();

                if (folderName.equals("")) {
                    selectFileDialog.dismiss();
                    openSelectFile();
                } else {
                    selectFileDialog.dismiss();
                    for (EncodeFile encodeFile : myEncodeFiles) {
                        if (encodeFile.getFolderName().equals(folderName)) {
                            myEncodeFile = encodeFile;
                            break;
                        }
                    }
                    mTCPServer.setLocalData(myEncodeFile);
                    String fileName = myEncodeFile.getFileName();
                    tv_fileName.setText("文件名：" + fileName);
                    //设置文件数目信息
                    int curNum = myEncodeFile.getCurrentSmallPieceNum();
                    int totalNum = myEncodeFile.getTotalSmallPieceNum();
                    SendMessage(MsgValue.SET_CUR_TOTAL_TV, curNum, totalNum, null);
                }
            }
        });
        selectFileDialog.setOnNegativeListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFileDialog.dismiss();
            }
        });
        selectFileDialog.show();
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
        CircleProgress circleProgress1 = (CircleProgress) findViewById(R.id.circle_progress1);
        TextView tv_phoneName1 = (TextView) findViewById(R.id.tv_phone_name1);

        CircleProgress circleProgress2 = (CircleProgress) findViewById(R.id.circle_progress2);
        TextView tv_phoneName2 = (TextView) findViewById(R.id.tv_phone_name2);

        CircleProgress circleProgress3 = (CircleProgress) findViewById(R.id.circle_progress3);
        TextView tv_phoneName3 = (TextView) findViewById(R.id.tv_phone_name3);

//        circleProgress4 = (CircleProgress) findViewById(R.id.circle_progress4);
//        tv_phoneName4 = (TextView) findViewById(R.id.tv_phone_name4);

        //把圆形进度对象添加至list   注意circleProgress与textView是配合使用的
        client_progress_list.add(circleProgress1);
        client_progress_list.add(circleProgress2);
        client_progress_list.add(circleProgress3);
//        client_progress_list.add(circleProgress4);

        tv_client_phoneName_list.add(tv_phoneName1);
        tv_client_phoneName_list.add(tv_phoneName2);
        tv_client_phoneName_list.add(tv_phoneName3);
//        tv_client_phoneName_list.add(tv_phoneName4);


    }

    /**
     * 开始水波纹
     *
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
        } else if (type.equals("isServer")) {
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
     *
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

        } else if (type.equals("isServer")) {
            ripple_server.stopRippleAnimation();

            ripple_server.setVisibility(View.GONE);
            circleProgress0.setVisibility(View.GONE);
            tv_phoneName0.setVisibility(View.GONE);

            //隐藏控件
            for (CircleProgress circleProgress : client_progress_list) {
                circleProgress.setVisibility(View.GONE);
            }

            for (TextView textView : tv_client_phoneName_list) {
                textView.setVisibility(View.GONE);
            }
        } else {

        }
    }

    /**
     * 检查wav的状态
     *
     * @return
     */
    public String checkWavState() {
        if (ripple_client.isRippleAnimationRunning()) {
            return "isClient";
        } else if (ripple_server.isRippleAnimationRunning()) {
            return "isServer";
        } else {
            return "";
        }
    }


    /**
     * 添加client的进度球
     *
     * @param cp_name
     * @param ip
     */
    public void setClientProgress(String cp_name, String ip) {
        CircleProgress circleProgress = null;
        TextView tv_cp_name = null;
        //现在myClientProgressList中查找
        for (int k = 0; k < myClientProgressList.size(); ++k) {
            if (myClientProgressList.get(k).getIp().equals(ip)) {
                circleProgress = myClientProgressList.get(k).getCircleProgress();
                tv_cp_name = myClientProgressList.get(k).getTextView();
                addCirclePro(circleProgress, tv_cp_name, cp_name);
                return;
            }
        }

        int i = 0;
        for (i = 0; i < client_progress_list.size(); ++i) {
            if (client_progress_list.get(i).getVisibility() == View.GONE) {
                circleProgress = client_progress_list.get(i);
                tv_cp_name = tv_client_phoneName_list.get(i);
                break;
            }
        }
        if (i == client_progress_list.size()) {
            //证明4个位置已经用完
            Toast.makeText(this, "超过" + i + "个不可再显示CircleProgress", Toast.LENGTH_SHORT).show();
            return;
        }
        MyCircleProgress myCircleProgress = new MyCircleProgress();
        myCircleProgress.setCircleProgress(circleProgress);
        myCircleProgress.setTextView(tv_cp_name);
        myCircleProgress.setIp(ip);
        myCircleProgress.setPhoneName(cp_name);

        myClientProgressList.add(myCircleProgress);


        addCirclePro(circleProgress, tv_cp_name, cp_name);


    }

    public void setMyClientProgressNum(String ip, int progress) {
        //在list中查找
        for (int k = 0; k < myClientProgressList.size(); ++k) {
            if (myClientProgressList.get(k).getIp().equals(ip)) {
                CircleProgress circleProgress = myClientProgressList.get(k).getCircleProgress();
                circleProgress.setProgress(progress);
                return;
            }
        }
    }

    /**
     * 删除指定IP的进度球
     *
     * @param ip
     */
    public void clearClientProgress(String ip) {
        MyCircleProgress myCircleProgress = null;
        CircleProgress circleProgress = null;
        TextView textView = null;
        int i = 0;
        for (i = 0; i < myClientProgressList.size(); ++i) {
            if (myClientProgressList.get(i).getIp().equals(ip)) {
                myCircleProgress = myClientProgressList.get(i);
                break;
            }
        }

//        if (i == myClientProgressList.size()) {
//            //没找到进度球
//            return;
//        }
        if (myCircleProgress != null) {
            circleProgress = myCircleProgress.getCircleProgress();
            textView = myCircleProgress.getTextView();

            //删除
            deleteCirPro(circleProgress, textView);
            //从列表删除
            myClientProgressList.remove(myCircleProgress);
        }

    }

    /**
     * 添加圆形进度控件
     *
     * @param circleProgress
     * @param tv
     * @param phoneName
     */
    public void addCirclePro(CircleProgress circleProgress, TextView tv, String phoneName) {
        circleProgress.setProgress(0);
        tv.setText(phoneName);
        circleProgress.setVisibility(View.VISIBLE);
        tv.setVisibility(View.VISIBLE);
    }

    /**
     * 删除指定的圆形进度控件
     *
     * @param circleProgress
     * @param tv
     */
    public void deleteCirPro(CircleProgress circleProgress, TextView tv) {
        circleProgress.setVisibility(View.GONE);
        tv.setVisibility(View.GONE);
    }

    void SendMessage(int what, int arg1, int arg2, Object obj) {
        if (handler != null) {
            Message.obtain(handler, what, arg1, arg2, obj).sendToTarget();
        }
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
