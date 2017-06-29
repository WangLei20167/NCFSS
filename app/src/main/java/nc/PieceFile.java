package nc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import utils.LocalInfor;
import utils.MyFileUtils;

/**
 * 管理一个分片内的文件
 * Created by Administrator on 2017/6/18 0018.
 */

public class PieceFile {
    //需要nK个文件本片才能解码
    private int nK;
    //这个分片文件的路径
    private String pieceFilePath;
    private boolean haveNeedFile = false;
    //其下的两个文件夹
    //用来存储现有的文件夹
    private String pieceEncodeFilePath = "";   //此用来存储编码文件

    private int pieceNo = 0;             //第几片  也是文件片名
    private int pieceFileNum = 0;        //用来记录一片中编码文件的个数
    private byte[][] coefMatrix = null;     //编码系数矩阵
    private JSONObject json_pfile_config = new JSONObject();   //用以存储文件个数和编码系数矩阵

    //对现有编码后用于发送的文件夹
    private String ready_to_send_path;    //其中包含将要发送的文件

    private File piece_recover_file;
    private ArrayList<File> piecesEncodeFiles = new ArrayList<File>();
    private File ready_to_send_file = null;


    //初次编码时存数据调用
    public PieceFile(String path, int pieceNo, int nK) {
        this.nK = nK;
        //构建pieceFile文件目录
        this.pieceNo = pieceNo;
        pieceFilePath = MyFileUtils.creatFolder(path, pieceNo + "");

        //创建二级目录
        pieceEncodeFilePath = MyFileUtils.creatFolder(pieceFilePath, "pieceEncodeFile");
        ready_to_send_path = MyFileUtils.creatFolder(pieceFilePath, "ready_to_send");

    }

    //恢复对本地数据的控制
    public void controlLocalData() {
        piecesEncodeFiles = MyFileUtils.getList_1_files(pieceEncodeFilePath);
        piece_recover_file = new File(pieceFilePath + File.separator + pieceNo + ".decode");
        ArrayList<File> files = MyFileUtils.getList_1_files(ready_to_send_path);
        if (files.size() == 0) {
            ready_to_send_file = null;
        } else {
            ready_to_send_file = files.get(0);
        }
        //恢复出来的文件
        File file = new File(pieceFilePath + File.separator + pieceNo + ".decode");
        if (file.exists()) {
            piece_recover_file = file;
        }
    }

    //用于获取配置信息
    public PieceFile() {

    }


    public byte[][] getCoefMatrix() {
        return coefMatrix;
    }

    public void setCoefMatrix(byte[][] coefMatrix) {
        //直接写入系数矩阵
        this.coefMatrix = coefMatrix;
    }


    public void setCoefMatrix() {
        //从文件读取系数矩阵
        coefMatrix = new byte[pieceFileNum][nK];
        coefMatrix = NCUtil.getCoefficientMatrix(piecesEncodeFiles);
    }


    public JSONObject getJson_pfile_config() {
        return json_pfile_config;
    }

    public void setJson_pfile_config() {
        //在此设置这一片文件的配置信息
        //json_pfile_config=new JSONObject();
        try {
            json_pfile_config.put("pieceNo", pieceNo);
            json_pfile_config.put("haveNeedFile", haveNeedFile);
            json_pfile_config.put("pieceFileNum", pieceFileNum);

            //用以存入编码系数矩阵
            JSONArray jA_coef = new JSONArray();
            for (int i = 0; i < pieceFileNum; ++i) {
                JSONArray jsonArray = new JSONArray();
                for (int j = 0; j < nK; ++j) {
                    jsonArray.put(coefMatrix[i][j]);
                }
                jA_coef.put(i, jsonArray);
            }
            //存入系数矩阵
            json_pfile_config.put("coefMatrix", jA_coef);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setJson_pfile_config(JSONObject json_pfile_config) {
        this.json_pfile_config = json_pfile_config;
    }

    public String getPieceEncodeFilePath() {
        return pieceEncodeFilePath;
    }

    public void setPieceEncodeFilePath(String pieceEncodeFilePath) {
        this.pieceEncodeFilePath = pieceEncodeFilePath;
    }

    public int getPieceFileNum() {
        return pieceFileNum;
    }

    public void setPieceFileNum(int pieceFileNum) {
        this.pieceFileNum = pieceFileNum;
    }

    public String getPieceFilePath() {
        return pieceFilePath;
    }

    public void setPieceFilePath(String pieceFilePath) {
        this.pieceFilePath = pieceFilePath;
    }

    public int getPieceNo() {
        return pieceNo;
    }

    public void setPieceNo(int pieceNo) {
        this.pieceNo = pieceNo;
    }

    public ArrayList<File> getPiecesEncodeFiles() {
        piecesEncodeFiles = MyFileUtils.getList_1_files(pieceEncodeFilePath);
        return piecesEncodeFiles;
    }

    public void setPiecesEncodeFiles(ArrayList<File> piecesEncodeFiles) {
        this.piecesEncodeFiles = piecesEncodeFiles;
    }

    public void addToPiecesEncodeFiles(File file) {
        piecesEncodeFiles.add(file);

    }

    public void setPiecesEncodeFiles() {
        //设置文件list，方便调用
        piecesEncodeFiles = MyFileUtils.getListFiles(pieceEncodeFilePath);
    }

    public String getReady_to_send_path() {
        return ready_to_send_path;
    }

    public void setReady_to_send_path(String ready_to_send_path) {
        this.ready_to_send_path = ready_to_send_path;
    }

    public boolean isHaveNeedFile() {
        return haveNeedFile;
    }

    public void setHaveNeedFile(boolean haveNeedFile) {
        this.haveNeedFile = haveNeedFile;
    }

    public int getnK() {
        return nK;
    }

    public void setnK(int nK) {
        this.nK = nK;
    }


    public File getReady_to_send_file() {
        return ready_to_send_file;
    }

    public void setReady_to_send_file(File ready_to_send_file) {
        this.ready_to_send_file = ready_to_send_file;
    }

    public File getPiece_recover_file() {
        return piece_recover_file;
    }

    public void setPiece_recover_file(File piece_recover_file) {
        this.piece_recover_file = piece_recover_file;
    }

}
