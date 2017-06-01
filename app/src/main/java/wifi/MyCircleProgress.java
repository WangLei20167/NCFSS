package wifi;

import android.widget.TextView;

import com.github.lzyzsd.circleprogress.CircleProgress;

/**
 * 用于把ip和圆形进度球绑定
 * Created by Administrator on 2017/5/29 0029.
 */

public class MyCircleProgress {
    private String ip;     //对应client的wifi

    private String phoneName;   //手机名
    private CircleProgress circleProgress;    //显示进度的控件
    private TextView textView;   //显示手机名字的控件

    public String getIp() {
        return ip;
    }

    public TextView getTextView() {
        return textView;
    }

    public void setTextView(TextView textView) {
        this.textView = textView;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public CircleProgress getCircleProgress() {
        return circleProgress;
    }

    public void setCircleProgress(CircleProgress circleProgress) {
        this.circleProgress = circleProgress;
    }

    public String getPhoneName() {
        return phoneName;
    }

    public void setPhoneName(String phoneName) {
        this.phoneName = phoneName;
    }
}
