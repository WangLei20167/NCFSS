package utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.administrator.ncfss.R;


/**
 * 用于弹出设置对话框
 * Created by Administrator on 2017/6/4 0004.
 */

public class SettingDialog extends Dialog {
    private Button positiveButton, negativeButton;
    private TextView title;
    private EditText et_N;
    private EditText et_K;
    private EditText et_SFN;

    public SettingDialog(Context context) {
        super(context, R.style.dialog);
        setSettingDialog();
    }

    private void setSettingDialog() {
        View mView = LayoutInflater.from(getContext()).inflate(R.layout.setting_dialog, null);
        title = (TextView) mView.findViewById(R.id.title);
        et_N=(EditText)mView.findViewById(R.id.editText_N);
        et_K = (EditText) mView.findViewById(R.id.editText_K);
        et_SFN=(EditText) mView.findViewById(R.id.editText_SFN);

        positiveButton = (Button) mView.findViewById(R.id.positiveButton);
        negativeButton = (Button) mView.findViewById(R.id.negativeButton);
        super.setContentView(mView);
    }


    /**
     * 得到输入框中的数值
     * @return
     */
    public int getEt_N(){
        String s=et_N.getText().toString();
        int result=0;
        try {
            result=Integer.parseInt(s);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }finally {
            return result;
        }
    }

    public int getEt_K(){
        String s=et_K.getText().toString();
        int result=0;
        try {
            result=Integer.parseInt(s);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }finally {
            return result;
        }
    }

    public int getEt_SFN(){
        String s=et_SFN.getText().toString();
        int result=0;
        try {
            result=Integer.parseInt(s);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }finally {
            return result;
        }
    }

    /**
     * 读取程序中现在的值
     * @param N
     * @param K
     * @param SFN
     */
    public void initNum(int N,int K,int SFN){
        et_N.setText(N+"");
        et_K.setText(K+"");
        et_SFN.setText(SFN+"");
    }



    @Override
    public void setContentView(int layoutResID) {
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
    }

    @Override
    public void setContentView(View view) {
    }

    /**
     * 确定键监听器
     * @param listener
     */
    public void setOnPositiveListener(View.OnClickListener listener){
        positiveButton.setOnClickListener(listener);
    }
    /**
     * 取消键监听器
     * @param listener
     */
    public void setOnNegativeListener(View.OnClickListener listener){
        negativeButton.setOnClickListener(listener);
    }
}
