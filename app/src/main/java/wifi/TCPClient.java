package wifi;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

import Utils.FileUtils;
import Utils.IntAndBytes;
import msg.MsgValue;

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

    public TCPClient(Context context,String TempPath,String FileRevPath,Handler handler){
        this.context=context;
        this.TempPath=TempPath;
        this.FileRevPath=FileRevPath;

        this.handler=handler;
    }

    //连接SocketServer
    public boolean connectServer() {
        try {
            socket = new Socket(Constant.TCP_ServerIP, Constant.TCP_ServerPORT);
            in=new DataInputStream(socket.getInputStream());     //接收
            out = new DataOutputStream(socket.getOutputStream());//发送
        } catch (IOException e) {
            e.printStackTrace();
            //连接失败
            return false;
        }
        //连接成功，启动接收线程
        receiveFile();
        return true;
    }
    //断开连接
    public void disconnectServer(){
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
                byte[] getBytes=new byte[4069];
                //待接收的文件长度和数目
                int fileLen=0;
                int fileNum=0;
                int restFileNum=0;
                int fileNameCount=1;
                String tempFilePath="";
                int readBytesNum=-1;
                boolean isFirstMsg=true;
                while (true) {
                    if (socket.isConnected()) {
                        if (!socket.isInputShutdown()) {
                            try {
                                if((readBytesNum=in.read(getBytes,0,255))> -1){
                                    //???需要先做接收文件名和文件长度的处理
                                    if (isFirstMsg) {
                                        isFirstMsg=false;
                                        byte[] lenBytes = new byte[4];
                                        for (int i = 0; i < 4; i++) {
                                            lenBytes[i] = getBytes[i];
                                        }
                                        fileLen = IntAndBytes.byte2int(lenBytes);
                                        fileNum=getBytes[4];
                                        restFileNum=fileNum;
                                        tempFilePath= FileUtils.creatTimeFolder(TempPath);
                                    }
                                    //将socket中的内容写入文件
                                    String fileName=fileNameCount+".nc";
                                    fileNameCount++;
                                    File file= FileUtils.creatFile(tempFilePath,fileName);
                                    FileOutputStream fos = new FileOutputStream(file);
//                                    fos.write(getBytes,0,fileLen);
                                    int readBytes=0;   //已经读取的字节数
                                    while(readBytes<fileLen){
                                        fos.write(getBytes,0,readBytesNum);
                                        readBytes+=readBytesNum;   //记录已经写入的文件个数
                                        if(readBytes<fileLen){
                                            readBytesNum=in.read(getBytes,0,255);
                                        }
                                    }
                                    fos.flush();
                                    fos.close();
                                    --restFileNum;
                                    if(restFileNum==0){
                                        //代表接收数据完毕
                                        isFirstMsg=true;
                                        fileNameCount=1;

                                        SendMessage(MsgValue.REVFINISH,fileNum,0,tempFilePath);
                                    }

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
    public void sendFile() {
        //需要先发送文件名和文件长度
        byte[] b="这是一个Client发给Server的测试文本".getBytes();
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


    void SendMessage(int what, int arg1,int arg2,Object obj){
        if (handler != null){
            Message.obtain(handler, what,arg1, arg2,obj).sendToTarget();
        }
    }
}
