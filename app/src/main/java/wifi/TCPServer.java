package wifi;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import msg.MsgValue;
import nc.ConstantValue;
import nc.MyEncodeFile;
import utils.IntAndBytes;
import utils.LocalInfor;
import utils.MyFileUtils;

/**
 * Created by Administrator on 2017/4/27 0027.
 * 建立一个TCP socket服务器
 */

public class TCPServer {
    //用于连接子节点
    private List<Socket> mList = new ArrayList<Socket>();
    private volatile ServerSocket server = null;
    private ExecutorService mExecutorService = null;   //线程池
    private volatile boolean flag_thread = true; //线程标志位
    private Context context;

    //获取服务端口状态
    public boolean getServerSocketState() {
        return serverSocketState;
    }

    private boolean serverSocketState = false;  //判断监听端口是否开启
    ServerThread serverThread = new ServerThread();

    //作为接收的缓存目录
    private String TempPath;
    private String FileRevPath;


    private Handler handler = null;

    public TCPServer(Context context, String TempPath, String FileRevPath, Handler handler) {
        //把活动对象传入
        this.handler = handler;
        this.context = context;
        this.TempPath = TempPath;
        this.FileRevPath = FileRevPath;
    }

    //开启socket服务
    public void StartServer() {
        serverThread.start();
    }

    //此处只是把socket中的client全都清除，不关闭ServerSocket监听端口
    public void CloseServer() {
        if (!serverSocketState) {
            //如果监听端口本就没开启，则返回
            return;
        }
        try {
//            flag_thread = false;
//            server.close();
//            serverSocketState = false;
            for (int p = 0; p < mList.size(); p++) {
                Socket s = mList.get(p);
                //关闭前向客户端发送信息
                DataInputStream in = new DataInputStream(s.getInputStream());     //接收
                DataOutputStream out = new DataOutputStream(s.getOutputStream());//发送
                String str_flag = Constant.END_FLAG + "#" + "" + "#";
                out.write(str_flag.getBytes());
                //关闭
                in.close();
                out.close();
                s.close();
            }
            mList.clear();
            mExecutorService.shutdownNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //关闭监听端口
    public void stopListening() {
        //关闭监听端口
        try {
            flag_thread = false;
            server.close();
            serverSocketState = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发送文件
     *
     * @param fileList 文件列表
     */
    public void SendFile(ArrayList<File> fileList) {

        int total_file_len = 0;
        for (File file : fileList) {
            // File file = Utils.getFileForUri(uri0);
//            if(file.length()>Integer.MAX_VALUE){
//                //文件过长
//                file.getName();
//            }
            total_file_len += file.length();
        }

        //按socket
        for (int p = 0; p < mList.size(); p++) {
            Socket s = mList.get(p);
            String ip = s.getInetAddress().toString();
            int already_send_len = 0;    //用以记录已经发送的字节数
            DataOutputStream out = null;
            InputStream input = null;
            for (File file : fileList) {
                // File file = Utils.getFileForUri(uri);
                try {
                    //设置非延迟发送
                    s.setTcpNoDelay(true);
                    out = new DataOutputStream(s.getOutputStream());//发送
                    String fileName_Len_totalLen = file.getName() + "#" + file.length() + "#" + total_file_len + "#";
                    //long fileLen = file.length();
//                    if (fileLen > Integer.MAX_VALUE) {
//                        //文件过大 超4G
//                        //break;
//                    }
                    //写入文件名字和长度
                    byte[] send_len_name = new byte[255];
                    byte[] len_name = fileName_Len_totalLen.getBytes();
                    int len_name_len = len_name.length;
                    for (int i = 0; i < len_name_len; i++) {
                        send_len_name[i] = len_name[i];
                    }

                    out.write(send_len_name);

                    //暂停40ms用来防止小包 粘包
//                    try {
//                        Thread.sleep(40);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    //读取文件内容发送
                    input = new FileInputStream(file);
                    byte[] data = new byte[1024];
                    int len = -1;
                    //int already_send_data=0;
                    while ((len = input.read(data)) != -1) {
                        out.write(data, 0, len);
                        already_send_len += len;
                        SendMessage(MsgValue.S_SET_SENT_PROGRESS, (int) ((already_send_len / (float) total_file_len) * 100), p + 1, ip);//p+1的意思是这是第几个client
                    }
                    //关闭输入输出流
                    //out.close();//若是关系，无法再次接收
                    input.close();

                    //暂停40ms用来防止小包 粘包
                    //文件与文件之间让其暂停100ms   防止粘包
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    //文件发送异常
                }
            }
        }


    }

    /**
     * 发送指定数目的文件到指定的IP
     *
     * @param ip           当ip为空时，发送给所有socket
     * @param myEncodeFile
     * @param sfn
     */
    public void SendFile(String ip, MyEncodeFile myEncodeFile, int sfn) {
        if (mList.size() == 0) {
            //没用户返回
            return;
        }

        String fileFolderName = myEncodeFile.getFileFolderName();
        String path = myEncodeFile.getSendFilePath();
        int sendFlag = myEncodeFile.getSendFlag();
        int sendFileNum = myEncodeFile.getRecode_file_num();
        if(sendFileNum==0){
            return;
        }
        //此标志位用来告知client自己是否还需要编码文件
        int haveAllNeedFile=0;
        if( myEncodeFile.getBl_decode()){
            haveAllNeedFile=1;
        }

        String origin_file_name = myEncodeFile.getFileName();
        //若是设置的SFN值大于已有文件数目
        if (sfn > sendFileNum) {
            sfn = sendFileNum;
        }

        //获取要发送的文件列表
        ArrayList<File> fileList = MyFileUtils.getListFiles(path);
        int total_file_len = 0;

        int sfn_flag = 0;
        for (File file : fileList) {
            // File file = Utils.getFileForUri(uri0);
//            if(file.length()>Integer.MAX_VALUE){
//                //文件过长
//                file.getName();
//            }
            total_file_len += file.length();
            ++sfn_flag;
            if (sfn_flag == sfn) {
                break;
            }
        }

        //按socket
        for (int p = 0; p < mList.size(); p++) {
            Socket s = mList.get(p);
            String _ip = s.getInetAddress().toString();
            if (ip.equals("")) {

            } else {
                if (!_ip.equals(ip)) {
                    continue;
                }
            }
            int already_send_len = 0;   //记录已经发送的字节数
            DataOutputStream out = null;
            InputStream input = null;
            for (int n = 0; n < sfn; ++n) {
                //发送数目
                int sf = (sendFlag + n) % sendFileNum;
                File file = fileList.get(sf);
                // File file = Utils.getFileForUri(uri);
                try {
                    //设置非延迟发送
                    s.setTcpNoDelay(true);
                    out = new DataOutputStream(s.getOutputStream());//发送
                    String fileName_Len_totalLen = file.getName() + "#" + file.length() + "#" + total_file_len + "#" + fileFolderName + "#" + origin_file_name + "#"+haveAllNeedFile +"#";
                    //long fileLen = file.length();
//                    if (fileLen > Integer.MAX_VALUE) {
//                        //文件过大 超4G
//                        //break;
//                    }
                    //写入文件名字和长度
                    byte[] send_len_name = new byte[255];
                    byte[] len_name = fileName_Len_totalLen.getBytes();
                    int len_name_len = len_name.length;
                    for (int i = 0; i < len_name_len; i++) {
                        send_len_name[i] = len_name[i];
                    }


                    out.write(send_len_name);

                    //暂停40ms用来防止小包 粘包
//                    try {
//                        Thread.sleep(40);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    //读取文件内容发送
                    input = new FileInputStream(file);
                    byte[] data = new byte[1024];
                    int len = -1;
                    //int already_send_data=0;
                    while ((len = input.read(data)) != -1) {
                        out.write(data, 0, len);
                        already_send_len += len;
                        //already_send_data+=len;
                        SendMessage(MsgValue.S_SET_SENT_PROGRESS, (int) ((already_send_len / (float) total_file_len) * 100), p + 1, _ip);//p+1的意思是这是第几个client
                    }
                    //关闭输入输出流
                    //out.close();//若是关闭，无法再次接收
                    input.close();

                } catch (IOException e) {
                    e.printStackTrace();
                    //文件发送异常
                }
            }
            //使连续两次发送的数据不一样
            myEncodeFile.setSendFlag((sendFlag + sfn) % sendFileNum);
//            for (File file:fileList) {
//
//            }
            // break;
        }

    }


    //Server端的主线程
    class ServerThread extends Thread {
        //停止监听端口
        //应加入把所有已经连接的节点出队的操作
        public void stopServer() {
            try {
                if (server != null) {
                    server.close();
                    System.out.println("close task successed");
                }
            } catch (IOException e) {
                System.out.println("close task failded");
            }
        }

        //开启监听端口，并通知哪个节点已经连接
        public void run() {
            try {
                server = new ServerSocket(Constant.TCP_ServerPORT, 10);  //连接请求队列的长度，超过则拒绝
                server.setReuseAddress(true);   //设置可重复绑定端口，指的是关闭后还没释放的那段时间
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                //监听端口开启失败
                serverSocketState = false;
                SendMessage(MsgValue.SFOPEN_LISTENER, 0, 0, "开启端口失败");

                System.out.println("S2: Error");
                e1.printStackTrace();
                return;
            }

            //监听端口开启成功
            serverSocketState = true;
            SendMessage(MsgValue.SFOPEN_LISTENER, 0, 0, "开启端口成功");

            mExecutorService = Executors.newCachedThreadPool();  //创建一个线程池
            System.out.println("服务器已启动...");
            Socket client = null;
            while (flag_thread) {
                try {
                    System.out.println("S3: Error");
                    client = server.accept();
                    //不让自己连本机端口
//                    if(client.getInetAddress().equals("/127.0.0.1")){
//                        continue;
//                    }
                    System.out.println("S4: Error");
                    //把客户端放入客户端集合中
                    mList.add(client);
                    //在此调用Service类，处理client连接与发送接收
                    mExecutorService.execute(new Service(client)); //启动一个新的线程来处理连接
                } catch (IOException e) {
                    System.out.println("S1: Error");
                    e.printStackTrace();
                }
            }
        }
    }

    //处理与client对话的线程
    class Service implements Runnable {
        private volatile boolean kk = true;
        private Socket socket;
        //private BufferedReader in = null;
        // private InputStream in;
        private DataInputStream in = null;   //接收
        private DataOutputStream out = null; //发送
        private String msg = "";

        private boolean socket_flag = true;
        private String socket_ip;

        public Service(Socket socket) {
            this.socket = socket;
            socket_ip = socket.getInetAddress().toString();
            try {
                socket.setTcpNoDelay(true); //设置直接发送
                socket.setKeepAlive(true);   //检测服务器是否处于活动状态

                //in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                in = new DataInputStream(socket.getInputStream());     //接收
                out = new DataOutputStream(socket.getOutputStream());//发送
                //提示哪个IP已连接
//                Message ClientOnLine = new Message();
//                ClientOnLine.what = CLIENTONLINE;
//                ClientOnLine.obj = socket.getInetAddress();
//                handler.sendMessage(ClientOnLine);
                //在此发送本机型号
                byte[] send_phoneName = new byte[255];
                String phoneName = LocalInfor.getPhoneModel() + "#" + "" + "#";

                byte[] bt_phoneName = phoneName.getBytes();
                int len = bt_phoneName.length;
                for (int i = 0; i < len; i++) {
                    send_phoneName[i] = bt_phoneName[i];
                }

                out.write(send_phoneName);

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("设置socket输入输出流出错");
            }

        }


        //接收来自client的文件
        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] getBytes = new byte[255];
                    //待接收的文件长度和数目
                    String phoneName = "";  //得到对方手机名

                    //int fileNum = 0;
                    //int restFileNum = 0;
                    String fileName = "";
                    int fileLen = 0;
                    int total_file_len = 0;
                    int already_rev_len = 0;
                    // int fileNameCount=1;
                    //String tempFilePath="";
                    boolean getCPInfor = true;    //得到socketSever的手机信息
                    int readBytesNum = -1;
                    //限制读取字节数，防止接收端出现的粘包现象
                    int limit_readNum = getBytes.length;
                    boolean isFirstMsg = true;
                    //用来接收
                    String revPath="";
                    String folderName="";
                    String origin_file_name="";
                    while (socket_flag) {
                        if (socket.isConnected()) {
                            if (!socket.isInputShutdown()) {
                                try {
                                    if ((readBytesNum = in.read(getBytes, 0, limit_readNum)) > -1) {

                                        if (getCPInfor) {
                                            //获得SP_Name
                                            getCPInfor = false;

                                            String str = new String(getBytes, 0, readBytesNum);
                                            String[] split = str.split("#");
                                            int flag = 1;
                                            for (String val : split) {
                                                if (flag == 1) {
                                                    //文件名
                                                    phoneName = val;
                                                    ++flag;
                                                }
                                            }
                                            String phoneName_ip = phoneName + "#" + socket_ip;

                                            SendMessage(MsgValue.CP_NAME, 0, 0, phoneName_ip);

                                            //结束这次循环
                                            continue;
                                        }
                                        if (isFirstMsg) {
                                            //先处理文件名字和长度信息
                                            isFirstMsg = false;

                                            String fileName_Len = new String(getBytes, 0, readBytesNum);
                                            String[] split = fileName_Len.split("#");
                                            int flag = 1;
                                            String newFolderName="";

                                            for (String val : split) {
                                                if (flag == 1) {
                                                    //文件名
                                                    fileName = val;
                                                    ++flag;
                                                } else if (flag == 2) {
                                                    if (val.equals("")) {
                                                        break;
                                                    }
                                                    //文件长度
                                                    try {
                                                        fileLen = Integer.parseInt(val);
                                                    } catch (NumberFormatException e) {
                                                        e.printStackTrace();
                                                        //接收文件长度失败   不是数字转化为int出错
                                                        SendMessage(MsgValue.S_REV_ERROR_FILELEN, 0, 0, "接收文件长度失败");
                                                    }
                                                    ++flag;

                                                } else if (flag == 3) {
                                                    try {
                                                        total_file_len = Integer.parseInt(val);
                                                    } catch (NumberFormatException e) {
                                                        e.printStackTrace();
                                                        //接收文件总长度失败   不是数字转化为int出错
                                                        SendMessage(MsgValue.S_REV_ERROR_FILELEN, 0, 0, "接收文件总长度失败");
                                                    }
                                                    ++flag;

                                                } else if (flag == 4) {
                                                    //folderName=val;
                                                    newFolderName = val;

                                                    ++flag;

                                                } else if (flag == 5) {
                                                    if (!val.equals("")) {
                                                        //获取原文件名
                                                        origin_file_name = val;
                                                    }
                                                    ++flag;

                                                } else if (flag == 6) {
                                                    //证明不是第一次收到此文件夹的数据  0代表还需数据  1带代表不需
                                                    int haveAllNeedFile = 0;
                                                    haveAllNeedFile = Integer.parseInt(val);

                                                    if (folderName.equals(newFolderName)) {
                                                        haveAllNeedFile = 1;
                                                    } else {
                                                        folderName = newFolderName;
                                                    }

                                                    revPath = TempPath + File.separator + folderName + File.separator + ConstantValue.ENCODE_FILE_FOLDER;

                                                    //去构造接收目录
                                                    SendMessage(MsgValue.S_CREATE_ENCODE_FILE_FOLDER, haveAllNeedFile, 0, folderName + "#" + origin_file_name);
                                                    ++flag;
                                                    //跳出循环
                                                    break;
                                                }
                                            }
                                            //处理结束标志
                                            if (fileName.equals(Constant.END_FLAG) && (flag==2)) {
                                                socket_flag = false;
                                                //关掉圆形进度球
                                                SendMessage(MsgValue.S_SOCET_END_FLAG, 0, 0, socket_ip);
                                                closeSocket();
                                                break;
                                            }
                                            if (fileLen < getBytes.length) {
                                                limit_readNum = fileLen;
                                            }
                                            //结束这次循环
                                            continue;
                                        }
                                        //将socket中的内容写入文件
                                        File file = MyFileUtils.creatFile(revPath, fileName);
                                        FileOutputStream fos = new FileOutputStream(file);
//                                    fos.write(getBytes,0,fileLen);
                                        int readBytes = 0;   //已经读取的字节数

                                        while (readBytes < fileLen) {
                                            fos.write(getBytes, 0, readBytesNum);
                                            readBytes += readBytesNum;   //记录已经写入的文件个数
                                            already_rev_len += readBytesNum;  //记录已经接收的字节数
                                            //设置接收进度
                                            SendMessage(MsgValue.S_SET_REV_PROGRESS, (int) ((already_rev_len / (float) total_file_len) * 100), 0, phoneName);
                                            if (readBytes < fileLen) {
                                                int rest_len = fileLen - readBytes;
                                                //防止多读取字节
                                                if (rest_len < 255) {
                                                    readBytesNum = in.read(getBytes, 0, rest_len);
                                                } else {
                                                    readBytesNum = in.read(getBytes, 0, 255);
                                                }

                                            }
                                        }


                                        fos.close();
                                        //用于下次接收
                                        isFirstMsg = true;
                                        limit_readNum = getBytes.length;
                                        //代表所有文件已经接收完毕
                                        if (already_rev_len == total_file_len) {
                                            already_rev_len = 0;
                                            total_file_len = 0;
                                        }

                                        //文件接收完毕
                                        SendMessage(MsgValue.S_REVFINISH, 0, 0, "接收" + fileName + "成功");


                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }).start();
        }

        //向客户端发送文件
        public void sendmsg(String msg) {
//            System.out.println(msg);
//            PrintWriter pout = null;
//            try {
//                pout = new PrintWriter(new BufferedWriter(
//                        new OutputStreamWriter(socket.getOutputStream())), true);
//                pout.println(msg);
//                pout.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            byte[] b = "这是一个Server发给Client的测试文本".getBytes();
            byte[] b1 = new byte[b.length + 1];
            b1[0] = (byte) b.length;
            for (int i = 0; i < b.length; ++i) {
                b1[i + 1] = b[i];
            }
            try {
                out.write(b1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //处理一个client的退出
        public void closeSocket() {
            try {
                out.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mList.remove(socket);
        }
    }


    void SendMessage(int what, int arg1, int arg2, Object obj) {
        if (handler != null) {
            Message.obtain(handler, what, arg1, arg2, obj).sendToTarget();
        }
    }
}
