package com.example.administrator.ncfss;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import Utils.FileUtils;

public class FilesListViewActivity extends AppCompatActivity {

    private String dataPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_list_view);


        Intent intent=getIntent();
        dataPath=intent.getStringExtra("data_path");

        setTitle(dataPath);
        final ArrayList<File> files= FileUtils.getListFiles(dataPath);

        final String[] fileList=new String[files.size()+1];
        fileList[0]="共"+files.size()+"个对象";
        int iFileList=1;
        for(File file: files){
            fileList[iFileList]=file.getName();
            ++iFileList;
        }

        ArrayAdapter<String> adapter=new ArrayAdapter<String>(FilesListViewActivity.this,R.layout.file_item,fileList);

        ListView listView=(ListView)findViewById(R.id.list_view);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position==0){
                    //返回
                    return;
                }
                File file=files.get(position-1);
                long fileLen=file.length();
                String str_length;
                DecimalFormat format = new DecimalFormat("###.##");
                if(fileLen>Math.pow(2,30)){
                    str_length=format.format((float)fileLen/Math.pow(2,30))+"GB";
                }else if(fileLen>Math.pow(2,20)){
                    str_length=format.format((float)fileLen/Math.pow(2,20))+"MB";
                }else if(fileLen>Math.pow(2,10)){
                    str_length=format.format((float)fileLen/Math.pow(2,10))+"KB";
                }else{
                    str_length=fileLen+"B";
                }
                long time=file.lastModified();
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(time);
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                new AlertDialog.Builder(FilesListViewActivity.this)
                        .setTitle(file.getName())
                        .setMessage("Length: "+str_length+"\n"+"LastModified: \n"+formatter.format(cal.getTime()))
                        .setPositiveButton("确定", null)
                        .show();
            }
        });
    }
}
