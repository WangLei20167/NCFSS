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
import nc.EncodeFile;
import nc.MyEncodeFile;
import nc.NCUtil;
import nc.PieceFile;
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

    private EncodeFile localData = null;
    private EncodeFile sendData = null;

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

    public EncodeFile getLocalData() {
        return localData;
    }

    public void setLocalData(EncodeFile localData) {
        this.localData = localData;
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

                byte[] bt_send = Constant.END_STRING.getBytes();
                int len = bt_send.length;
                out.write(IntAndBytes.send_instruction_len(Constant.END_FALG, len));
                out.write(bt_send);
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

                //在此发送本机型号

                String phoneName = LocalInfor.getPhoneModel();
                byte[] send_phoneName = phoneName.getBytes();
                int len = send_phoneName.length;
                out.write(IntAndBytes.send_instruction_len(Constant.PHONE_NAME, len));   //发送指令长度
                out.write(send_phoneName);

                //发送配置文件json_file
                byte[] bt_json_file = MyFileUtils.readFile(localData.getFolderPath(), "json.txt");
                int file_len = bt_json_file.length;
                out.write(IntAndBytes.send_instruction_len(Constant.JSON_FILE, file_len));
                out.write(bt_json_file);

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
                    byte[] getBytes = new byte[Constant.BUFFER_SIZE];
                    //待接收的文件长度和数目
                    String phoneName = "";    //获取手机名称

                    int pieceNo = 0;
                    String pieceFileName = "";
                    while (socket_flag) {
                        if (socket.isConnected() && (!socket.isInputShutdown())) {
                            try {
                                if ((in.read(getBytes, 0, 5)) > -1) {
                                    //根据首字节判断该执行哪段代码
                                    byte instruction = getBytes[0];
                                    //获取需读取的长度
                                    byte[] bt_len = new byte[4];
                                    for (int i = 0; i < 4; ++i) {
                                        bt_len[i] = getBytes[i + 1];
                                    }
                                    int readLen = IntAndBytes.byte2int(bt_len);
                                    switch (instruction) {
                                        //接收手机名称
                                        case Constant.PHONE_NAME:
                                            in.read(getBytes, 0, readLen);
                                            phoneName = new String(getBytes, 0, readLen);
                                            //向ui线程发送消息
                                            SendMessage(MsgValue.CP_NAME, 0, 0, phoneName + "#" + socket_ip);
                                            break;
                                        //接收配置文件 配置文件名字为json.txt
                                        case Constant.JSON_FILE:
                                            int limitLen = readLen;
                                            if (readLen > Constant.BUFFER_SIZE) {
                                                limitLen = Constant.BUFFER_SIZE;
                                            }
                                            File json_file = MyFileUtils.creatFile(TempPath, "json.txt");
                                            FileOutputStream fos1 = new FileOutputStream(json_file);
                                            int bytes = 0;
                                            while (true) {
                                                int len=in.read(getBytes, 0, limitLen);
                                                fos1.write(getBytes, 0, len);
                                                bytes += len;   //记录已经写入的文件个数
                                                //设置接收进度
                                                limitLen = readLen - bytes;
                                                if (limitLen > Constant.BUFFER_SIZE) {
                                                    limitLen = Constant.BUFFER_SIZE;
                                                } else if (limitLen <= 0) {
                                                    break;
                                                }
                                            }
                                            fos1.close();
                                            //解析文件，查看自己是否有对方有用的数据，以便向对方发送
                                            ArrayList<File> files = getSendFiles(json_file);
                                            SendFile(socket_ip, out, files);
                                            //重新编码文件
                                            if(!(files==null||files.size()==0)) {
                                                localData.reEncodeFile();
                                            }
                                            break;
                                        //pieceNo和接收编码文件的名字属于哪个分片
                                        case Constant.PIECE_FILE_NAME:
                                            in.read(getBytes, 0, readLen);
                                            pieceFileName = new String(getBytes, 0, readLen);
                                            String s_no = pieceFileName.substring(0, pieceFileName.indexOf("."));
                                            pieceNo = Integer.parseInt(s_no);

                                            break;
                                        //接收编码文件
                                        case Constant.PIECE_FILE:
                                            int limitReadNum = 0;
                                            if (readLen < Constant.BUFFER_SIZE) {
                                                limitReadNum = readLen;
                                            } else {
                                                limitReadNum = Constant.BUFFER_SIZE;
                                            }
                                            PieceFile pieceFile = null;
                                            boolean flag_new = false;
                                            for (PieceFile pieceFile1 : localData.getMyPiecesFiles()) {
                                                if (pieceFile1.getPieceNo() == pieceNo) {
                                                    pieceFile = pieceFile1;
                                                    break;
                                                }
                                            }
                                            if (pieceFile == null) {
                                                pieceFile = new PieceFile(localData.getFolderPath(), pieceNo, localData.getnK());
                                                flag_new = true;
                                            }
                                            //将socket中的内容写入文件
                                            File file = MyFileUtils.creatFile(pieceFile.getPieceEncodeFilePath(), pieceFileName);
                                            FileOutputStream fos = new FileOutputStream(file);

                                            int readBytes = 0;   //已经读取的字节数
                                            while (true) {
                                                //从缓存区读取数据放入文件
                                                int len = in.read(getBytes, 0, limitReadNum);  //限制最大读取长度
                                                fos.write(getBytes, 0, len);
                                                readBytes += len;   //记录已经写入的文件个数
                                                //设置接收进度
                                                SendMessage(MsgValue.S_SET_REV_PROGRESS, (int) ((readBytes / (float) readLen) * 100), 0, phoneName);
                                                limitReadNum = readLen - readBytes;
                                                if (limitReadNum > Constant.BUFFER_SIZE) {
                                                    limitReadNum = Constant.BUFFER_SIZE;
                                                } else if (limitReadNum <= 0) {
                                                    break;
                                                }
                                            }
                                            //关闭流
                                            fos.close();
                                            pieceFile.addToPiecesEncodeFiles(file);
                                            pieceFile.setPieceFileNum(pieceFile.getPieceFileNum() + 1);
                                            pieceFile.setCoefMatrix();

                                            if (pieceFile.getPieceFileNum() == pieceFile.getnK()) {
                                                pieceFile.setHaveNeedFile(true);
                                                NCUtil.decode_file(pieceFile);

                                            }
                                            pieceFile.setJson_pfile_config();
                                            if (flag_new) {
                                                localData.add2myPiecesFiles(pieceFile);
                                                localData.setPiecesNum(localData.getPiecesNum() + 1);
                                            } else {
                                                ArrayList<PieceFile> pieceFiles = localData.getMyPiecesFiles();
                                                PieceFile pieceFile0 = null;
                                                for (PieceFile pieceFile1 : pieceFiles) {
                                                    if (pieceFile1.getPieceNo() == pieceNo) {
                                                        pieceFile0 = pieceFile1;
                                                        break;
                                                    }
                                                }
                                                //删除原有的信息
                                                if (pieceFile0 != null) {
                                                    pieceFiles.remove(pieceFile0);
                                                    pieceFiles.add(pieceFile);
                                                    localData.setMyPiecesFiles(pieceFiles);
                                                }
                                            }
                                            localData.setCurrentSmallPieceNum(localData.getCurrentSmallPieceNum() + 1);

                                            //尝试解码
                                            if (localData.getCurrentSmallPieceNum() == localData.getTotalSmallPieceNum()) {
                                                localData.setHaveAllNeedFile(true);
                                                if (localData.try2decode()) {
                                                    // localData.getFileName()解码成功
                                                }
                                            }
                                            localData.setJson_config();

                                            //文件接收完毕
                                            SendMessage(MsgValue.S_REVFINISH, 0, 0, "接收" + pieceFileName + "成功");
                                            break;
                                        //接收退出标志
                                        case Constant.END_FALG:
                                            in.read(getBytes, 0, readLen);
                                            String s = new String(getBytes, 0, readLen);
                                            if (s.equals(Constant.END_STRING)) {
                                                socket_flag = false;
                                                //关掉圆形进度球
                                                SendMessage(MsgValue.S_SOCET_END_FLAG, 0, 0, socket_ip);
                                                //关闭流
                                                in.close();
                                                out.close();
                                                socket.close();
                                            }
                                            break;
                                        default:
                                            break;
                                    }


                                }
                            } catch (IOException e) {
                                e.printStackTrace();
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

    //检查本地是否有对方需要的数据
    public ArrayList<File> getSendFiles(File json_file) {
        EncodeFile encodeFile = EncodeFile.parse_JSON_File(json_file);
        String fileName = encodeFile.getFileName();
        String folderName = encodeFile.getFolderName();
        int nK = encodeFile.getnK();

        //查找本地是否有数据
//        EncodeFile localData = null;
//        ArrayList<File> folders = MyFileUtils.getListFolders(TempPath);
//        for (File folder : folders) {
//            if (folder.getName().equals(folderName)) {
//                //在本地找到数据 恢复对本地数据控制
//                String json_file_path = TempPath + File.separator + folderName + File.separator + "json.txt";
//                File file = new File(json_file_path);
//                localData = EncodeFile.parse_JSON_File(file);
//                localData.getLocalData();
//                break;
//            }
//        }
        if (localData == null || !(localData.getFolderName().equals(folderName))) {
            //在此构造接收目录
            localData = new EncodeFile(TempPath, fileName, nK);
            localData.setTotalPiecesNum(encodeFile.getTotalPiecesNum());
            localData.setTotalSmallPieceNum(encodeFile.getTotalSmallPieceNum());
            localData.setJson_config();
            ArrayList<File> one_file = new ArrayList<File>();
            one_file.add(localData.getJson_file());
            //发送json配置文件
            //    SendFile(one_file);
            return null;
        } else {
            if (encodeFile.isHaveAllNeedFile()) {
                return null;
            } else {
                ArrayList<File> files = new ArrayList<File>();
                for (final PieceFile lD_pieceFile : localData.getMyPiecesFiles()) {
                    int pieceNo = lD_pieceFile.getPieceNo();
                    byte[][] coefMatrix = lD_pieceFile.getCoefMatrix();

                    boolean haveThisPiece = false;
                    for (PieceFile pieceFile : encodeFile.getMyPiecesFiles()) {
                        if (pieceFile.getPieceNo() == pieceNo) {
                            if (pieceFile.isHaveNeedFile()) {
                                //本片不再需要数据
                                haveThisPiece = true;
                                break;
                            }
                            byte[][] Rev = pieceFile.getCoefMatrix();
                            int nRow = pieceFile.getPieceFileNum();
                            int nCol = pieceFile.getnK();
                            int row = lD_pieceFile.getPieceFileNum();  //本地
                            if (NCUtil.havaUsefulData(Rev, nRow, nCol, coefMatrix, row)) {
                                File file = lD_pieceFile.getReady_to_send_file();
                                if (file.exists()) {
                                    files.add(file);
                                    lD_pieceFile.setSend_or_no(true);
                                }
                            }
                            haveThisPiece = true;
                            break;
                        }
                    }
                    if (!haveThisPiece) {
                        File file = lD_pieceFile.getReady_to_send_file();
                        if (file.exists()) {
                            lD_pieceFile.setSend_or_no(true);
                            files.add(file);
                        }
                    }

                }
                return files;
            }

        }

    }


    /**
     * 发送文件
     * <p>
     * out 指定发送流
     *
     * @param fileList 文件列表
     */
    public void SendFile(String ip, DataOutputStream out, ArrayList<File> fileList) {
        if (fileList == null || fileList.size() == 0) {
            return;
        }
        int total_file_len = 0;
        for (File file : fileList) {
            total_file_len += file.length();
        }


        int already_send_len = 0;    //用以记录已经发送的字节数


        for (File file : fileList) {
            // File file = Utils.getFileForUri(uri);
            try {
                InputStream input = null;
                //发送文件名
                byte[] bt_fileName = file.getName().getBytes();
                int fileName_len = bt_fileName.length;
                out.write(IntAndBytes.send_instruction_len(Constant.PIECE_FILE_NAME, fileName_len));
                out.write(bt_fileName);
                //发送文件的长度
                int fileLen = (int) file.length();
                out.write(IntAndBytes.send_instruction_len(Constant.PIECE_FILE, fileLen));
                //读取文件的内容发送
                input = new FileInputStream(file);
                byte[] data = new byte[Constant.BUFFER_SIZE];

                //int already_send_data=0;
                int limitRead = 0;
                if (fileLen > Constant.BUFFER_SIZE) {
                    limitRead = Constant.BUFFER_SIZE;
                } else {
                    limitRead = fileLen;
                }
                int readLen = 0;
                while ((input.read(data, 0, limitRead)) != -1) {
                    out.write(data, 0, limitRead);
                    already_send_len += limitRead;   //总体进度
                    readLen += limitRead;   //单个文件的进度
                    SendMessage(MsgValue.S_SET_SENT_PROGRESS, (int) ((already_send_len / (float) total_file_len) * 100), 0, ip);// 改变进度球
                    limitRead = fileLen - readLen;
                    if (limitRead > Constant.BUFFER_SIZE) {
                        limitRead = Constant.BUFFER_SIZE;
                    } else if (limitRead <= 0) {
                        //发送完成
                        break;
                    }
                }

                //关闭输入输出流
                //out.close();//若是关闭，无法再次接收
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
                //文件发送异常
                return;
            }
        }


    }


    void SendMessage(int what, int arg1, int arg2, Object obj) {
        if (handler != null) {
            Message.obtain(handler, what, arg1, arg2, obj).sendToTarget();
        }
    }
}
