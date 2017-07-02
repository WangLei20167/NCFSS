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
import java.net.Socket;
import java.util.ArrayList;

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
 * Created by Administrator on 2017/4/28 0028.
 * 用于连接socket服务器
 */

public class TCPClient {
    private Socket socket = null;
    private DataInputStream in = null;   //接收
    private DataOutputStream out = null; //发送
    private Context context;

    private Handler handler = null;

    //作为接收的缓存目录
    private String TempPath;
    private String FileRevPath;

    //
    private EncodeFile localData = null;


    //用作判断socket是否已经建立
    private boolean socket_flag = false;

    public TCPClient(Context context, String TempPath, String FileRevPath, Handler handler) {
        this.context = context;
        this.TempPath = TempPath;
        this.FileRevPath = FileRevPath;

        this.handler = handler;
    }

    //连接SocketServer
    public boolean connectServer() {
        try {
            socket = new Socket(Constant.TCP_ServerIP, Constant.TCP_ServerPORT);
            socket.setTcpNoDelay(true);
            in = new DataInputStream(socket.getInputStream());     //接收
            out = new DataOutputStream(socket.getOutputStream());//发送
        } catch (IOException e) {
            e.printStackTrace();
            //连接失败
            SendMessage(MsgValue.CONNECT_SF, 0, 0, "连接失败");
            return false;
        }
        //连接成功，启动接收线程
        socket_flag = true;
        //在此发送本机型号
        String phoneName = LocalInfor.getPhoneModel();
        byte[] send_phoneName = phoneName.getBytes();
        int len = send_phoneName.length;
        try {
            out.write(IntAndBytes.send_instruction_len(Constant.PHONE_NAME, len));   //发送指令长度
            out.write(send_phoneName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        receiveFile();
        return true;
    }

    public boolean getSocket_flag() {
        return socket_flag;
    }

    //断开连接
    public void disconnectServer() {
        try {
            //关闭前向服务端发送信息
            byte[] bt_end_string = Constant.END_STRING.getBytes();
            out.write(IntAndBytes.send_instruction_len(Constant.END_FALG, bt_end_string.length));
            out.write(bt_end_string);

            //关闭流
            out.close();
            in.close();
            //关闭Socket
            socket.close();
            socket_flag = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //接收来自Server的文件
    public void receiveFile() {
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
                                        SendMessage(MsgValue.SET_SERVER_CIRPRO, 0, 0, phoneName);
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
                                            int len = in.read(getBytes, 0, limitLen);
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
                                        SendFile(files);
                                        if(!(files==null||files.size()==0)) {
                                            localData.reEncodeFile();
                                        }
                                        //SendMessage(MsgValue.C_PARSE_JSON_FILR, 0, 0, "");
                                        break;
                                    //pieceNo和接收编码文件的名字属于哪个分片
                                    case Constant.PIECE_FILE_NAME:
                                        in.read(getBytes, 0, readLen);
                                        pieceFileName = new String(getBytes, 0, readLen);
                                        String s_no = pieceFileName.substring(0, pieceFileName.indexOf("."));
                                        try {
                                            pieceNo = Integer.parseInt(s_no);
                                        } catch (NumberFormatException e) {
                                            //转换错误
                                            e.printStackTrace();
                                        }
                                        break;
                                    //接收编码文件
                                    case Constant.PIECE_FILE:
                                        int limitReadNum = 0;
                                        if (readLen < Constant.BUFFER_SIZE) {
                                            limitReadNum = readLen;
                                        } else {
                                            limitReadNum = Constant.BUFFER_SIZE;
                                        }

                                        //在本地编码文件夹中查找存储位置
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
                                            int len = in.read(getBytes, 0, limitReadNum);  //限制最多读取的字节数
                                            fos.write(getBytes, 0, len);
                                            readBytes += len;   //记录已经写入的文件个数
                                            //设置接收进度
                                            SendMessage(MsgValue.SET_REV_PROGRESS, (int) ((readBytes / (float) readLen) * 100), 0, phoneName);
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
                                        //尝试解码
                                        if (pieceFile.getPieceFileNum() >= pieceFile.getnK()) {
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
                                        SendMessage(MsgValue.C_REVFINISH, 0, 0, "接收" + pieceFileName + "成功");
                                        break;
                                    //接收退出标志
                                    case Constant.END_FALG:
                                        in.read(getBytes, 0, readLen);
                                        String s = new String(getBytes, 0, readLen);
                                        if (s.equals(Constant.END_STRING)) {
                                            socket_flag = false;
                                            //关掉圆形进度球
                                            SendMessage(MsgValue.C_SOCKET_END_FLAG, 0, 0, null);
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


    //检查本地是否有对方需要的数据
    public ArrayList<File> getSendFiles(File json_file) {
        EncodeFile encodeFile = EncodeFile.parse_JSON_File(json_file);
        String fileName = encodeFile.getFileName();
        String folderName = encodeFile.getFolderName();
        int nK = encodeFile.getnK();

        //查找本地是否有数据
        localData = null;
        ArrayList<File> folders = MyFileUtils.getListFolders(TempPath);
        for (File folder : folders) {
            if (folder.getName().equals(folderName)) {
                //在本地找到数据 恢复对本地数据控制
                String json_file_path = TempPath + File.separator + folderName + File.separator + "json.txt";
                File file = new File(json_file_path);
                if (file.exists()) {
                    localData = EncodeFile.parse_JSON_File(file);
                    localData.getLocalData();
                }
                break;
            }
        }
        if (localData == null || !(localData.getFolderName().equals(folderName))) {
            //在此构造接收目录
            localData = new EncodeFile(TempPath, fileName, nK);
            localData.setTotalPiecesNum(encodeFile.getTotalPiecesNum());
            localData.setTotalSmallPieceNum(encodeFile.getTotalSmallPieceNum());
            localData.setJson_config();
            //发送配置文件json_file
            byte[] bt_json_file = MyFileUtils.readFile(localData.getFolderPath(), "json.txt");
            int file_len = bt_json_file.length;
            try {
                out.write(IntAndBytes.send_instruction_len(Constant.JSON_FILE, file_len));
                out.write(bt_json_file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        } else {
            //发送配置文件json_file
            byte[] bt_json_file = MyFileUtils.readFile(localData.getFolderPath(), "json.txt");
            int file_len = bt_json_file.length;
            try {
                out.write(IntAndBytes.send_instruction_len(Constant.JSON_FILE, file_len));
                out.write(bt_json_file);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                                    lD_pieceFile.setSend_or_no(true);
                                    files.add(file);
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

    //向Server发送文件
    public void SendFile(ArrayList<File> fileList) {
        if (fileList == null || fileList.size() == 0) {
            return;
        }
        //统计所有文件长度
        int total_file_len = 0;
        int already_send_len = 0;
        for (File file : fileList) {
            total_file_len += file.length();
        }

        for (File file : fileList) {
            InputStream input = null;
            try {
                //发送文件名
                byte[] bt_fileName = file.getName().getBytes();
                int fileName_len = bt_fileName.length;
                out.write(IntAndBytes.send_instruction_len(Constant.PIECE_FILE_NAME, fileName_len));
                out.write(bt_fileName);
                //发送文件的长度
                int fileLen = (int) file.length();
                out.write(IntAndBytes.send_instruction_len(Constant.PIECE_FILE, fileLen));
                //读取文件内容发送
                input = new FileInputStream(file);
                byte[] data = new byte[Constant.BUFFER_SIZE];

                int limitRead = 0;
                if (fileLen > Constant.BUFFER_SIZE) {
                    limitRead = Constant.BUFFER_SIZE;
                } else {
                    limitRead = fileLen;
                }
                int readLen = -1;
                while ((input.read(data, 0, limitRead)) != -1) {
                    out.write(data, 0, limitRead);
                    already_send_len += limitRead;
                    readLen += limitRead;
                    SendMessage(MsgValue.SET_SEND_PROGRESS, (int) ((already_send_len / (float) total_file_len) * 100), 0, "");
                    //算出剩余的长度
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
            }
        }
    }


    void SendMessage(int what, int arg1, int arg2, Object obj) {
        if (handler != null) {
            Message.obtain(handler, what, arg1, arg2, obj).sendToTarget();
        }
    }
}
