package wifi;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import com.nononsenseapps.filepicker.Utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;

import utils.IntAndBytes;
import msg.MsgValue;
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
        //在此发送本机型号
        byte[] send_phoneName=new byte[255];
        String phoneName=LocalInfor.getPhoneModel()+"#";
        byte[] bt_phoneName= phoneName.getBytes();
        int len=bt_phoneName.length;
        for(int i=0;i<len;i++){
            send_phoneName[i]=bt_phoneName[i];
        }
        try {
            out.write(send_phoneName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //SendMessage(MsgValue.CONNECT_SF, 0, 0, "连接成功");
        receiveFile();
        return true;
    }

    //断开连接
    public void disconnectServer() {
        try {
            //关闭流
            out.close();
            in.close();
            //关闭Socket
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //接收来自Server的文件
    public void receiveFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] getBytes = new byte[255];
                //待接收的文件长度和数目
                String phoneName="";    //获取手机名称
                int fileLen = 0;
                int fileNum = 0;
                int restFileNum = 0;
                String fileName = "";
                // int fileNameCount=1;
                //String tempFilePath="";
                boolean getSPInfor=true;    //得到socketSever的手机信息
                String clientPhoneInfor="";
                int readBytesNum = -1;
                //限制读取字节数，防止接收端出现的粘包现象
                int limit_readNum=getBytes.length;
                boolean isFirstMsg = true;
                while (true) {
                    if (socket.isConnected()) {
                        if (!socket.isInputShutdown()) {
                            try {
                                if ((readBytesNum = in.read(getBytes, 0, limit_readNum)) > -1) {

                                    if(getSPInfor){
                                        //获得SP_Name
                                        getSPInfor=false;
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
                                        SendMessage(MsgValue.SP_NAME, 0, 0, phoneName);
                                        SendMessage(MsgValue.SET_SERVER_CIRPRO,0,0,phoneName);
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
                                                    SendMessage(MsgValue.C_REV_ERROR_FILELEN, 0, 0, "接收文件长度失败");
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
                                        SendMessage(MsgValue.SET_REV_PROGRESS,(int)((readBytes/(float)fileLen)*100),0,phoneName);
                                        if (readBytes < fileLen) {
                                            int rest_len=fileLen-readBytes;
                                            //防止多读取字节
                                            if(rest_len<255){
                                                readBytesNum = in.read(getBytes,0,rest_len);
                                            }else {
                                                readBytesNum = in.read(getBytes,0,255);
                                            }

                                        }
                                        //设置接收进度

                                    }
                                    fos.close();
                                    isFirstMsg = true;

                                    //重新定义缓存流


                                   // in = new DataInputStream(socket.getInputStream());     //接收
                                    limit_readNum=getBytes.length;

                                    //文件接收完毕
                                    SendMessage(MsgValue.C_REVFINISH, 0, 0, "接收" + fileName + "成功");


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

    //向Server发送文件
    public void sendFile(List<Uri> files) {
        //需要先发送文件名和文件长度
        InputStream input = null;
        for (Uri uri : files) {
            File file = Utils.getFileForUri(uri);
            try {
                String fileName_Len = file.getName() + "#" + file.length() + "#";
                long fileLen = file.length();
//                    if (fileLen > Integer.MAX_VALUE) {
//                        //文件过大 超4G
//                        //break;
//                    }
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
                    SendMessage(MsgValue.SET_SEND_PROGRESS,(int)((already_send_data/(float)fileLen)*100),0,"");
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


    void SendMessage(int what, int arg1, int arg2, Object obj) {
        if (handler != null) {
            Message.obtain(handler, what, arg1, arg2, obj).sendToTarget();
        }
    }
}
