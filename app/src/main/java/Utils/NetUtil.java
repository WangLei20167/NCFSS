package Utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;

/**
 * Created by Administrator on 2017/4/27 0027.
 * 检查指定端口是否被占用
 */

public class NetUtil {

    private boolean wifiState;
    private boolean mobileDataSatus;
    private Context context;

    /**
     * 检查WiFi和移动数据是否可用
     */
    public void checkNet() {
        ConnectivityManager con = (ConnectivityManager) context.getSystemService(Activity.CONNECTIVITY_SERVICE);
        wifiState = con.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();
        mobileDataSatus = con.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting();
    }

    /**
     * 恢复网络
     */
    public void resetNet(){

    }
}
