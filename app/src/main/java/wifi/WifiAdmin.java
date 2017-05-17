package wifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/4/25 0025.
 */

public class WifiAdmin {
    //定义WifiManager对象
    private WifiManager mWifiManager;
    //定义WifiInfo对象
    private WifiInfo mWifiInfo;
    private Context context;


    //定义一个WiFiLock
    WifiManager.WifiLock mWifiLock;


    //构造器
    public WifiAdmin(Context context) {
        this.context = context;
        //取得WifiManager对象
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        //取得WifiInfo对象
        mWifiInfo = mWifiManager.getConnectionInfo();
    }


    //检查当前WIFI状态
    public int checkState() {
        return mWifiManager.getWifiState();
    }

    //打开WIFI
    public void openWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    //关闭WIFI
    public void closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }

    //查看指定SSID的网络是否存在
    public WifiConfiguration IsExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = new ArrayList<WifiConfiguration>();
        existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }


    //查看扫描结果
//    public StringBuilder LookUpScan() {
//        mWifiManager.startScan();
//        //得到扫描结果
//        mWifiList = mWifiManager.getScanResults();
//
//        StringBuilder stringBuilder = new StringBuilder();
//        for (int i = 0; i < mWifiList.size(); i++) {
//            stringBuilder.append("Index_" + new Integer(i + 1).toString() + ":");
//            //将ScanResult信息转换成一个字符串包
//            //其中包括：BSSID、SSID、capabilities、frequency、level
//            stringBuilder.append(mWifiList.get(i).toString());
//            stringBuilder.append("\n");
//        }
//        return stringBuilder;
//    }

    //查看扫描结果
    public List<String> LookUpScan(){
        mWifiManager.startScan();
        //得到扫描结果
        List<ScanResult> mWifiList = mWifiManager.getScanResults();
        List<String> SSID_List=new ArrayList<>();
        for(int i=0;i<mWifiList.size();++i){
            SSID_List.add(mWifiList.get(i).SSID);
        }
        return SSID_List;
    }

    //得到WifiInfo的所有信息包
    public String GetWifiInfo() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.toString();
    }

    //连接指定的wifi
    public void connectAppointedNet() {
        //等待找到指定WiFi
//        while(IsExsits(Constant.HOST_SPOT_SSID)==null){
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        //连接指定wifi
        //检查wifi状态,已打开则向下进行,否则等待
//        while(!mWifiManager.isWifiEnabled()){
//
//        }
//        List<String> SSID_List=LookUpScan();

        WifiConfiguration config = new WifiConfiguration();
        //config.SSID = "\"fileSharing\"";
        config.SSID = Constant.HOST_SPOT_SSID;
        // config.preSharedKey = null;//非加密wifi
        //config.preSharedKey = "\"123456789\"";//加密wifi
        config.preSharedKey = Constant.HOST_SPOT_PASS_WORD;//加密wifi
        config.hiddenSSID = true;
        config.status = WifiConfiguration.Status.ENABLED;
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        // config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);//WPA_PSK  NONE（非加密）
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);//加密
        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

        //连接到配置好的网络
        int netId = mWifiManager.addNetwork(config);
        boolean b = mWifiManager.enableNetwork(netId, true);
    }

    //得到IP地址
    public int GetNetworkID(){
        return (mWifiInfo==null)?0:mWifiInfo.getIpAddress();
    }
}
