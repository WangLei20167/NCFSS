package wifi;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.nononsenseapps.filepicker.Utils;

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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import msg.MsgValue;
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
    private boolean serverSocketState = false;  //判断监听端口是否开启
    ServerThread serverThread = new ServerThread();

    //作为接收的缓存目录
    private String TempPath;
    private String FileRevPath;

    private Handler handler = null;

    public TCPServer(Context context, String TempPath, String FileRevPath, Handler handler) {
        //把活动对象传入
        this.handler=handler;
        this.context = context;
        this.TempPath = TempPath;
        this.FileRevPath = FileRevPath;
    }

    //开启socket服务
    public void StartServer() {
        serverThread.start();
    }

    //关闭socket服务
    public void CloseServer() {
        if (!serverSocketState) {
            //如果监听端口本就没开启，则返回
            return;
        }
        try {
            flag_thread = false;
            server.close();
            serverSocketState = false;
            for (int p = 0; p < mList.size(); p++) {
                Socket s = mList.get(p);
                s.close();
            }
            mExecutorService.shutdownNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送文件
     *
     * @param files 文件列表
     */
    public void SendFile(List<Uri> files) {
        //按文件
        //先发送文件名和文件长度
        //向所有已连接节点发送数据
//        for (Uri uri : files) {
//            File file = Utils.getFileForUri(uri);
//
//
//            for (int p = 0; p < mList.size(); p++) {
//                Socket s = mList.get(p);
//
//                DataOutputStream out = null;
//                InputStream input = null;
//                try {
//                    s.setTcpNoDelay(true);
//                    out = new DataOutputStream(s.getOutputStream());//发送
//                    String fileName_Len = file.getName() + "#" + file.length() + "#";
//                    long fileLen = file.length();
////                    if (fileLen > Integer.MAX_VALUE) {
////                        //文件过大 超4G
////                        //break;
////                    }
//                    byte[] send_len_name = new byte[255];
//                    byte[] len_name = fileName_Len.getBytes();
//                    int len_name_len = len_name.length;
//                    for (int i = 0; i < len_name_len; i++) {
//                        send_len_name[i] = len_name[i];
//                    }
//                    //Arrays.fill(send_len_name,(byte)0);
////                    byte[] bt_len= IntAndBytes.int2byte((int)file.length());
////                    for(int i=0;i<4;i++){
////                        send_len_name[i]=bt_len[i];
////                    }
////                    byte[] bt_name=file.getName().getBytes();
////                    byte name_len=(byte)bt_name.length;
////                    send_len_name[4]=name_len;
////                    for(int i=5;i<5+name_len;i++){
////                        send_len_name[i]=bt_name[i-5];
////                    }
//
//
//                    out.write(send_len_name);
//
//                    //暂停40ms用来防止小包 粘包
////                    try {
////                        Thread.sleep(40);
////                    } catch (InterruptedException e) {
////                        e.printStackTrace();
////                    }
//                    //读取文件内容发送
//                    input = new FileInputStream(file);
//                    byte[] data = new byte[1024];
//                    int len;
//                    int already_send_data = 0;
//                    while ((len = input.read(data)) != -1) {
//                        // out.write(data, 0, len);
//                        out.write(data, 0, len);
//                        already_send_data += len;
//                        SendMessage(MsgValue.S_SET_SENT_PROGRESS, (int) ((already_send_data / (float) fileLen) * 100), p + 1, "");//p+1的意思是这是第几个client
//                        //Arrays.fill(data,(byte)0);
//                    }
//                    //关闭输入输出流
//                    //out.close();//若是关系，无法再次接收
//                    input.close();
//
//                    //暂停40ms用来防止小包 粘包
//                    //文件与文件之间让其暂停100ms   防止粘包
////                    try {
////                        Thread.sleep(100);
////                    } catch (InterruptedException e) {
////                        e.printStackTrace();
////                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    //文件发送异常
//                }
//            }
//        }


        //发送给client

        //按socket
        for (int p = 0; p < mList.size(); p++) {
            Socket s = mList.get(p);

            DataOutputStream out = null;
            InputStream input = null;
            for (Uri uri : files) {
                File file = Utils.getFileForUri(uri);
                try {
                    s.setTcpNoDelay(true);
                    out = new DataOutputStream(s.getOutputStream());//发送
                    String fileName_Len = file.getName() + "#" + file.length() + "#";
                    long fileLen = file.length();
//                    if (fileLen > Integer.MAX_VALUE) {
//                        //文件过大 超4G
//                        //break;
//                    }
                    //写入文件名字和长度
                    byte[] send_len_name = new byte[255];
                    byte[] len_name = fileName_Len.getBytes();
                    int len_name_len = len_name.length;
                    for (int i = 0; i < len_name_len; i++) {
                        send_len_name[i] = len_name[i];
                    }
                    //Arrays.fill(send_len_name,(byte)0);
//                    byte[] bt_len= IntAndBytes.int2byte((int)file.length());
//                    for(int i=0;i<4;i++){
//                        send_len_name[i]=bt_len[i];
//                    }
//                    byte[] bt_name=file.getName().getBytes();
//                    byte name_len=(byte)bt_name.length;
//                    send_len_name[4]=name_len;
//                    for(int i=5;i<5+name_len;i++){
//                        send_len_name[i]=bt_name[i-5];
//                    }


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
                    int len;
                    int already_send_data=0;
                    while ((len = input.read(data)) != -1) {
                       // out.write(data, 0, len);
                        out.write(data, 0, len);
                        already_send_data+=len;
                        SendMessage(MsgValue.S_SET_SENT_PROGRESS,(int)((already_send_data/(float)fileLen)*100),p+1,"");//p+1的意思是这是第几个client
                        //Arrays.fill(data,(byte)0);
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

    //释放资源
    protected void destroy() {

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
                server = new ServerSocket(Constant.TCP_ServerPORT, 5);  //连接请求队列的长度，超过则拒绝
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

        public Service(Socket socket) {
            this.socket = socket;
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
                byte[] send_phoneName=new byte[255];
                String phoneName=LocalInfor.getPhoneModel()+"#";
                byte[] bt_phoneName= phoneName.getBytes();
                int len=bt_phoneName.length;
                for(int i=0;i<len;i++){
                    send_phoneName[i]=bt_phoneName[i];
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
                    String phoneName="";  //得到对方手机名
                    int fileLen = 0;
                    int fileNum = 0;
                    int restFileNum = 0;
                    String fileName = "";
                    // int fileNameCount=1;
                    //String tempFilePath="";
                    boolean getCPInfor=true;    //得到socketSever的手机信息
                    int readBytesNum = -1;
                    //限制读取字节数，防止接收端出现的粘包现象
                    int limit_readNum=getBytes.length;
                    boolean isFirstMsg = true;
                    while (true) {
                        if (socket.isConnected()) {
                            if (!socket.isInputShutdown()) {
                                try {
                                    if ((readBytesNum = in.read(getBytes,0, limit_readNum)) > -1) {

                                        if(getCPInfor){
                                            //获得SP_Name
                                            getCPInfor=false;

                                            String str=new String(getBytes,0,readBytesNum);
                                            String[] split =str.split("#");
                                            int flag = 1;
                                            for (String val : split) {
                                                if (flag == 1) {
                                                    //文件名
                                                    phoneName = val;
                                                    ++flag;
                                                }
                                            }

                                            SendMessage(MsgValue.CP_NAME, 0, 0, phoneName);
                                            //设置进度球
                                            SendMessage(MsgValue.S_SET_CLIENT_CIRPRO,mList.size(),0,phoneName);  //arg1位置存的是client的个数
                                            //结束这次循环
                                            continue;
                                        }
                                        if (isFirstMsg) {
                                            //先处理文件名字和长度信息
                                            isFirstMsg = false;
                                            // 格式：len+fileName # fileLen
//                                        int len_name_len=getBytes[0];
//                                        byte[] name_len=new byte[len_name_len];
//                                        for(int i=0;i<len_name_len;++i){
//                                            name_len[i]=getBytes[i+1];
//                                        }

                                            String fileName_Len = new String(getBytes,0,readBytesNum);
                                            String[] split = fileName_Len.split("#");
                                            int flag = 1;
                                            for (String val : split) {
                                                if (flag == 1) {
                                                    //文件名
                                                    fileName = val;
                                                    ++flag;
                                                } else if (flag == 2) {
                                                    //文件长度
                                                    try {
                                                        fileLen = Integer.parseInt(val);
                                                    } catch (NumberFormatException e) {
                                                        e.printStackTrace();
                                                        //接收文件长度失败   不是数字转化为int出错
                                                        SendMessage(MsgValue.S_REV_ERROR_FILELEN, 0, 0, "接收文件长度失败");
                                                    }
                                                    ++flag;
                                                    //跳出循环
                                                    break;
                                                }else{

                                                }
                                            }
                                            if(fileLen<getBytes.length){
                                                limit_readNum=fileLen;
                                            }
                                            //结束这次循环
                                            continue;
                                        }
                                        //将socket中的内容写入文件
                                        File file = MyFileUtils.creatFile(FileRevPath, fileName);
                                        FileOutputStream fos = new FileOutputStream(file);
//                                    fos.write(getBytes,0,fileLen);
                                        int readBytes = 0;   //已经读取的字节数

                                        while (readBytes < fileLen) {
                                            fos.write(getBytes, 0, readBytesNum);
                                            readBytes += readBytesNum;   //记录已经写入的文件个数
                                            //设置接收进度
                                            SendMessage(MsgValue.S_SET_REV_PROGRESS,(int)((readBytes/(float)fileLen)*100),0,phoneName);
                                            if (readBytes < fileLen) {
                                                int rest_len=fileLen-readBytes;
                                                //防止多读取字节
                                                if(rest_len<255){
                                                    readBytesNum = in.read(getBytes,0,rest_len);
                                                }else {
                                                    readBytesNum = in.read(getBytes,0,255);
                                                }

                                            }
                                        }

//                                        do{
//                                            fos.write(getBytes, 0, readBytesNum);
//                                            readBytes += readBytesNum;   //记录已经写入的文件个数
//                                            //设置接收进度
//                                            SendMessage(MsgValue.S_SET_REV_PROGRESS,(int)((readBytes/(float)fileLen)*100),0,phoneName);
//                                        }while((readBytesNum  = in.read(getBytes))!= -1&&(readBytes < fileLen));


                                        fos.close();
                                        isFirstMsg = true;

                                        //重新定义缓存流

                                        //in.reset();
                                        in = new DataInputStream(socket.getInputStream());     //接收
                                        limit_readNum=getBytes.length;

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
    }





    void SendMessage(int what, int arg1, int arg2, Object obj) {
        if (handler != null) {
            Message.obtain(handler, what, arg1, arg2, obj).sendToTarget();
        }
    }
}
