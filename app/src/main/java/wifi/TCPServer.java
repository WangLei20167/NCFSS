package wifi;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    public TCPServer(Context context,String TempPath,String FileRevPath) {
        //把活动对象传入
        this.context = context;
        this.TempPath=TempPath;
        this.FileRevPath=FileRevPath;
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
     * 向所用已连接节点发送数据
     * @param sentData
     * @param row 按行发送行数
     */
    public void SendFile(byte[][] sentData,int row){
        //先发送文件名和文件长度
        //向所有已连接节点发送数据
        for (int p = 0; p < mList.size(); p++) {
            Socket s = mList.get(p);
            try {
                DataOutputStream out = new DataOutputStream(s.getOutputStream());//发送
                for(int i=0;i<row;++i){
                    out.write(sentData[i]);
                }
            } catch (IOException e) {
                e.printStackTrace();
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
                server = new ServerSocket(Constant.TCP_ServerPORT);
                server.setReuseAddress(true);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                //监听端口开启失败
                serverSocketState = false;
                Message FOpen_Listener = new Message();
                FOpen_Listener.what = FOPEN_LISTENER;
                handler.sendMessage(FOpen_Listener);
                System.out.println("S2: Error");
                e1.printStackTrace();
                return;
            }

            //监听端口开启成功
            serverSocketState = true;
            Message SOpen_Listener = new Message();
            SOpen_Listener.what = SOPEN_LISTENER;
            handler.sendMessage(SOpen_Listener);

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
                //in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                in=new DataInputStream(socket.getInputStream());     //接收
                out = new DataOutputStream(socket.getOutputStream());//发送
                //提示哪个IP已连接
                Message ClientOnLine = new Message();
                ClientOnLine.what = CLIENTONLINE;
                ClientOnLine.obj = socket.getInetAddress();
                handler.sendMessage(ClientOnLine);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        //接收来自client的文件
        @Override
        public void run() {
            byte[] getBytes = new byte[255];
            while (kk) {
                if (socket.isConnected()) {
                    if (!socket.isOutputShutdown()) {
                        try {
                            if (in.read(getBytes, 0, 255) > -1) {
                                int length = getBytes[0];
                                byte[] b = new byte[length];
                                for (int i = 0; i < length; ++i) {
                                    b[i] = getBytes[i + 1];
                                }
                                String s = new String(b);
                                Message getClientMsg = new Message();
                                getClientMsg.what = GETCLIENTMSG;
                                getClientMsg.obj = s;
                                handler.sendMessage(getClientMsg);
                            }
                        } catch (IOException e) {
                            System.out.println("close");
                            kk = false;
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
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
            byte[] b="这是一个Server发给Client的测试文本".getBytes();
            byte[] b1=new byte[b.length+1];
            b1[0]=(byte)b.length;
            for(int i=0;i<b.length;++i){
                b1[i+1]=b[i];
            }
            try {
                out.write(b1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static final int FOPEN_LISTENER = 1;
    public static final int SOPEN_LISTENER = 2;
    public static final int CLIENTONLINE = 3;
    public static final int GETCLIENTMSG=4;

    //定义消息处理
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FOPEN_LISTENER:
                    Toast.makeText(context, "开启端口失败", Toast.LENGTH_SHORT).show();
                    break;
                case SOPEN_LISTENER:
                    Toast.makeText(context, "开启端口成功", Toast.LENGTH_SHORT).show();
                    break;
                case CLIENTONLINE:
                    Toast.makeText(context, msg.obj + "已连接", Toast.LENGTH_SHORT).show();
                    break;
                case GETCLIENTMSG:
                    Toast.makeText(context, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    void SendMessage(int what, Object obj){
        if (handler != null){
            Message.obtain(handler, what, obj).sendToTarget();
        }
    }
}
