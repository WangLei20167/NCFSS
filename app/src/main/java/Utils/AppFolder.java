package Utils;


import android.os.Environment;

import java.io.File;

/**
 * Created by Administrator on 2017/5/5 0005.
 * 用来创建app应用目录，一个缓存目录，一个接收目录
 */

public class AppFolder {
    public String FolderPath="";
    //用来存放临时数据
    public String TempPath="";
    //用来存放已接收数据
    public String FileRevPath="";


    /**
     * @param folderName 文件夹名
     * @return 返回文件夹操作路径，后带斜杠
     */
    public boolean createPath(String folderName) {
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            //创建3个文件夹folderName和其子文件夹Temp、FileRev
            String mainPath = Environment.getExternalStorageDirectory().getPath() + File.separator + folderName;
            File destDir = new File(mainPath);
            if (!destDir.exists()) {
                //如果不存在则创建
                destDir.mkdirs();//在根创建了文件夹hello
            }
            //String folderPath = mainPath + File.separator;
            FolderPath=mainPath + File.separator;

            String _tempPath=FolderPath+"Temp";
            File tempDir=new File(_tempPath);
            if(!tempDir.exists()){
                tempDir.mkdir();
            }
            TempPath=_tempPath+File.separator;

            String _FileRev=FolderPath+"FileRev";
            File fileRevDir=new File(_FileRev);
            if(!fileRevDir.exists()){
                fileRevDir.mkdir();
            }
            FileRevPath=_FileRev+File.separator;

            return true;
        }
        return false;
    }

}
