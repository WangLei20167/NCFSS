package nc;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import utils.LocalInfor;
import utils.MyFileUtils;

/**
 * 此类用于管理网络编解码文件
 * Created by Administrator on 2017/6/6 0006.
 */

public class MyEncodeFile {
    private boolean bl_decode = false;   //记录是否已经进行了解码,此位为true，则表明无需再获取数据
    private int fileNum = 0;    //指的是编码数据的个数
    private int needFileNum = 0;  //一共需要多少文件可以解码

    private int sendFlag = 0;   //记录该发送那几个文件  按顺序循环

    private String fileFolderName;
    private String filePath;
    private String encodeFilePath;   //记录的是编码文件夹路径
    private String sendFilePath;     //再编码后等待发送的文件

    private int recode_file_num = 0;   //用来记录用于发送的文件个数（再编码后的文件）

    private String fileName;
    private File fileName_txt;

    //private boolean first_init;//用来标志是第一次使用（true）还是从文件中读入（false）

    public MyEncodeFile(String tempPath, String fileName, boolean first_init,String folderName) {    //这里fileName是带后缀的

        if (!folderName.equals("")) {
            this.fileFolderName = folderName; //尽量做到文件夹名字唯一
        }else {
            String _filename_folder = fileName.substring(0, fileName.lastIndexOf("."));   //获取不含后缀的文件名,作为文件夹名字
            this.fileFolderName = _filename_folder + "_" + LocalInfor.getCurrentTime("HHmmss");  //尽量做到文件夹名字唯一

        }

        this.filePath = MyFileUtils.creatFolder(tempPath, this.fileFolderName);
        this.fileName = fileName;
        //MyFileUtils.writeToFile(this.filePath,"fileName.txt",fileName.getBytes());

        //创造两个二级文件夹
        this.encodeFilePath = MyFileUtils.creatFolder(filePath, ConstantValue.ENCODE_FILE_FOLDER);
        this.sendFilePath = MyFileUtils.creatFolder(filePath, ConstantValue.SEND_FILE_FOLDER);

        if (first_init) {
            MyFileUtils.writeToFile(this.filePath, ConstantValue.FILE_NAME_TXT, fileName.getBytes());
            return;
        }

        //以下是从文件系统读入时需要执行的代码
        ArrayList<File> files = MyFileUtils.getListFiles(encodeFilePath);
        fileNum = files.size();
        if (fileNum != 0) {
            try {
                //从文件读取nK值，在第一个字节
                FileInputStream stream = new FileInputStream(files.get(0));
                byte[] b = new byte[1];
                stream.read(b);
                needFileNum = (int) b[0];
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        recode_file_num = MyFileUtils.getFileNum(sendFilePath);  //获取再编码文件目录下的文件数目
        if (fileNum != 0 && (fileNum == needFileNum)) {
            bl_decode = true; //证明文件数目已经够了，不再需要文件
        }
    }


    public boolean isBl_decode() {
        return bl_decode;
    }

    public void setBl_decode(boolean bl_decode) {
        this.bl_decode = bl_decode;
    }

    public String getEncodeFilePath() {
        return encodeFilePath;
    }


    public int getFileNum() {
        fileNum = MyFileUtils.getFileNum(encodeFilePath);
        return fileNum;
    }

    public void setFileNum(int fileNum) {
        this.fileNum = fileNum;
    }

    public int getNeedFileNum() {
        ArrayList<File> files = MyFileUtils.getListFiles(encodeFilePath);
        fileNum = files.size();
        if (fileNum != 0) {
            try {
                //从文件读取nK值，在第一个字节
                FileInputStream stream = new FileInputStream(files.get(0));
                byte[] b = new byte[1];
                stream.read(b);
                needFileNum = (int) b[0];
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return needFileNum;
    }

    public void setNeedFileNum(int needFileNum) {
        this.needFileNum = needFileNum;
    }

    public String getSendFilePath() {
        return sendFilePath;
    }


    public String getFilePath() {
        return filePath;
    }

    public int getSendFlag() {
        return sendFlag;
    }

    public void setSendFlag(int sendFlag) {
        this.sendFlag = sendFlag;
    }

    public String getFileFolderName() {
        return fileFolderName;
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public int getRecode_file_num() {
        recode_file_num = MyFileUtils.getFileNum(sendFilePath);  //获取再编码文件目录下的文件数目
        return recode_file_num;
    }

    public void setRecode_file_num(int recode_file_num) {
        this.recode_file_num = recode_file_num;
    }

}
