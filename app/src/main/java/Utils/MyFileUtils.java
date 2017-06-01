package utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Created by Administrator on 2017/5/14 0014.
 */

public class MyFileUtils {
    /**
     * 将byte流写入指定文件，文件若是不存在，先创建再写
     * @param path   路径
     * @param fileName 文件名
     * @param inputData
     */
    public static void writeToFile(String path, String fileName,byte[] inputData) {

        File myFile = new File(path+File.separator+fileName);
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
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
        } finally {
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

    /**
     * 从路径文件中读取数据放入byte[]
     * @param path
     * @param fileName
     * @return byte[]
     */
    public static byte[] readFile(String path,String fileName){
        String path_fileName=path+File.separator+fileName;
        File file=new File(path_fileName);
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        long fileSize = file.length();
        if (fileSize > Integer.MAX_VALUE) {
            System.out.println("file too big...");
            return null;
        }

        byte[] buffer = new byte[0];
        try {
            FileInputStream fi = new FileInputStream(file);
            buffer = new byte[(int) fileSize];
            int offset = 0;
            int numRead = 0;
            while (offset < buffer.length
                    && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
                offset += numRead;
            }
            // 确保所有数据均被读取
            if (offset != buffer.length) {
                throw new IOException("Could not completely read file "
                        + file.getName());
            }
            fi.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return buffer;

    }


    /**
     * 在指定路径下创建File，并返回File对象
     *
     * @param path
     * @param fileName
     * @return
     */
    public static File creatFile(String path, String fileName) {
        String toFile = path + File.separator + fileName;
        File myFile = new File(toFile);
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
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
     * 在指定目录下创建文件夹
     *
     * @param path       路径
     * @param folderName 文件夹名
     * @return 返回创建的文件夹路径
     */
    public static String creatFolder(String path, String folderName) {
        String pathFolder = path + File.separator + folderName;
        File tempFolder = new File(pathFolder);
        if (!tempFolder.exists()) {
            //若不存在，则创建
            tempFolder.mkdir();
        }
        return pathFolder;
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

    /**
     *
     */


    /**
     * 把文件复制进指定的路径
     * @param file_source
     * @param targetPath
     */
    public static void copyFile(File file_source,String targetPath){
        String fileName=file_source.getName();

        File file_target = new File(targetPath+File.separator+fileName);
        if (!file_target.exists()) {   //不存在则创建
            try {
                file_target.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        InputStream fis = null;
        OutputStream fos = null;
        try {
            fis = new FileInputStream(file_source);
            fos = new FileOutputStream(file_target);
            byte[] buf = new byte[4096];
            int i;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
