package wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by Vision on 15/6/24.<br>
 * Email:Vision.lsm.2012@gmail.com
 */
public class APHelper {

    private static final String TAG = APHelper.class.getName();

    public static final int WIFI_AP_STATE_ENABLING = 12;
    public static final int WIFI_AP_STATE_ENABLED = 13;
    public static final int WIFI_AP_STATE_FAILED = 14;
    private final WifiManager mWifiManager;
    private Context context;

    public APHelper(Context context) {
        this.context = context;
        mWifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);

    }

    public boolean isApEnabled() {
        int state = getWifiApState();
        return WIFI_AP_STATE_ENABLING == state || WIFI_AP_STATE_ENABLED == state;
    }

    public int getWifiApState() {
        try {
            Method method = mWifiManager.getClass().getMethod("getWifiApState");
            int i = (Integer) method.invoke(mWifiManager);
            return i;
        } catch (Exception e) {
            Log.i(TAG, "Cannot get WiFi AP state" + e);
            return WIFI_AP_STATE_FAILED;
        }
    }


    //可用于打开或关闭热点
    public boolean setWifiApEnabled(WifiConfiguration wifiConfig, boolean enabled) {
        boolean result = false;
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
        try {
            Method method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            result = (boolean) (Boolean) method.invoke(mWifiManager, wifiConfig, enabled);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return result;
    }

    //配置wifi
    public static WifiConfiguration createWifiCfg() {
        WifiConfiguration wifiCfg = new WifiConfiguration();

//        //获取时间加在ssid后面保证ssid唯一
//        SimpleDateFormat format = new SimpleDateFormat("HHmmss");
//        Date curDate = new Date();
//        String str_time = format.format(curDate);


        wifiCfg.SSID = Constant.HOST_SPOT_SSID ;
        wifiCfg.preSharedKey = Constant.HOST_SPOT_PASS_WORD;
//        wifiCfg.SSID = "\"" + ssid + "\"";
       // wifiCfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        wifiCfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        wifiCfg.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        //用WPA密码方式保护
        wifiCfg.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wifiCfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wifiCfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wifiCfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wifiCfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wifiCfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);


        return wifiCfg;
    }


}

