package Utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


/**
 * Created by Administrator on 2017/4/23 0023.
 * 取出选择文件的路径
 */

public class FileUtils {


    //取出选择文件的路径
    public static String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection,null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * 保留文件名及后缀
     */
    public static String getFileNameWithSuffix(String pathandname) {
        int start = pathandname.lastIndexOf("/");
        if (start != -1 ) {
            return pathandname.substring(start + 1);
        } else {
            return null;
        }
    }


    /**
     * @param path 要写入文件的路径,带后斜杠
     * @param fileName 创建的文件名（带扩展名）
     * @param inputData 向文件中写入的内容
     */
    public static void writeToFile(String path,String fileName,byte[] inputData){
        String toFile = path + fileName;
        File myFile = new File(toFile);
        FileOutputStream fos=null;
        BufferedOutputStream bos=null;
        if (!myFile.exists()) {   //不存在则创建
            try {
                myFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        try {
            //传递一个true参数，代表不覆盖已有的文件。并在已有文件的末尾处进行数据续写,false表示覆盖写
            //FileWriter fw = new FileWriter(myFile, false);
            //BufferedWriter bw = new BufferedWriter(fw);
            fos = new FileOutputStream(myFile);
            bos = new BufferedOutputStream(fos);
            bos.write(inputData);
            //bw.write("测试文本");
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (bos != null) {
                try {
                    bos.flush();
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public static File creatFile(String path,String fileName){
        String toFile = path + fileName;
        File myFile = new File(toFile);
        FileOutputStream fos=null;
        BufferedOutputStream bos=null;
        if (!myFile.exists()) {   //不存在则创建
            try {
                myFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return myFile;
    }

    /**
     * 打开指定文件夹
     * @param context 活动
     * @param path    文件夹路径
     */
    public static void openAssignFolder(Context context,String path){

        File file = new File(path);
        if(null==file || !file.exists()){
            return;
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(file), "file/*");
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 在指定路径下以当前时间为名创建文件
     * @param path 指定路径
     * @return 创建的文件夹路径
     */
    public static String creatTimeFolder(String path){
        //SimpleDateFormat myFmt1=new SimpleDateFormat("yyyy-MM-dd  hh:mm:ss");
        SimpleDateFormat myFmt=new SimpleDateFormat("MMdd HH:mm:ss");  //HH 24小时制，hh 12小时制
        Date now=new Date();
        String rq=myFmt.format(now);

        String _timeFolderPath=path+rq;
        File timeDir=new File(_timeFolderPath);
        if(!timeDir.exists()){
            timeDir.mkdir();
        }
        String TimeFolderPath=_timeFolderPath+File.separator;
        return TimeFolderPath;
    }


    /***
     * 获取指定目录下的所有的文件（不包括文件夹），采用了递归
     *
     * @param obj
     * @return
     */
    public static ArrayList<File> getListFiles(Object obj) {
        File directory = null;
        if (obj instanceof File) {
            directory = (File) obj;
        } else {
            directory = new File(obj.toString());
        }
        ArrayList<File> files = new ArrayList<File>();
        if (directory.isFile()) {
            files.add(directory);
            return files;
        } else if (directory.isDirectory()) {
            File[] fileArr = directory.listFiles();
            for (int i = 0; i < fileArr.length; i++) {
                File fileOne = fileArr[i];
                files.addAll(getListFiles(fileOne));
            }
        }
        return files;
    }

}
