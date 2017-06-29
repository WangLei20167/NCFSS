package msg;

/**
 * Created by Administrator on 2017/5/7 0007.
 * 用来存储MainActivity中的msg.what值,为了方便msg的跨类使用
 */

public class MsgValue {
    public static final int APOPENSUCCESS = 0;  //开启热点成功
    public static final int APOPENFAILED = 1;   //开启热点失败
    public static final int TELL_ME_SOME_INFOR=21;  //用于显示提示信息
    public static final int SET_CUR_TOTAL_TV=24;    //设置编码文件个数信息
    /**
     * 处理TCPClient的信息
     */
    public final static int CONNECT_SF=2;
    public final static int C_REVFINISH=3;   //接收完成
    public final static int C_REV_ERROR_FILELEN=4;   //接收文件长度时发生错误
    public final static int SP_NAME=5;   //获取建立的连接的手机名
    public final static int SET_SERVER_CIRPRO=11;  //当连接到服务器时显示server的圆形进度
    public final static int SET_REV_PROGRESS=12;      //设置接收进度
    public final static int SET_SEND_PROGRESS=13;   //设置发送进度
    public final static int C_SOCKET_END_FLAG=18;
    public final static int C_REV_ALL_FINISH=19;   //所有文件接收完毕
    public final static int C_CREATE_ENCODE_FILE_FOLDER=20;//创建接收编码文件的目录
    public final static int C_PARSE_JSON_FILR=23;   //解析发来的json配置文件

    /**
     * 处理TCPServer的信息
     */
    public static final int SFOPEN_LISTENER = 6;
    public static final int CP_NAME = 7;
    public static final int GETCLIENTFILE = 8;
    public static final int S_REV_ERROR_FILELEN=9;
    public final static int S_REVFINISH=10;   //接收完成
  //  public final static int S_SET_CLIENT_CIRPRO=14;  //设置client的圆形进度
    public final static int S_SET_REV_PROGRESS=15;   //设置接收进度
    public final static int S_SET_SENT_PROGRESS=16;   //设置发送进度
    public final static int S_SOCET_END_FLAG=17;     //socket关闭
    public final static int S_CREATE_ENCODE_FILE_FOLDER=22;//创建接收编码文件的目录


    /**
     * 处理WifiAdmin的消息
     */
    //public final static int CONNECT_WIFI_SUCCESS=17;

}
