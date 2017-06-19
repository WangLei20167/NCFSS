package nc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import utils.MyFileUtils;

/**
 * 管理一个分片内的文件
 * Created by Administrator on 2017/6/18 0018.
 */

public class PieceFile {
    private int nK;
    //这个分片文件的路径
    private String pieceFilePath;
    private boolean haveNeedFile = false;
    //其下的两个文件夹
    //用来存储现有的文件夹
    private String pieceEncodeFilePath;
    private ArrayList<File> piecesEncodeFiles = new ArrayList<File>();

    private int pieceNo;             //第几片  也是文件片名
    private int pieceFileNum;        //用来记录一片中编码文件的个数
    private byte[][] coefMatrix = null;     //编码系数矩阵
    private JSONObject json_pfile_config = new JSONObject();   //用以存储文件个数和编码系数矩阵


    //对现有编码后用于发送的文件夹
    private String ready_to_send_path;
    private File file_ready_to_send;     //由piecesEncodeFiles随机编码生成

    // private File piece_recover_file;
    private String piece_recover_file_path;
    private boolean pieceDecoded = false;   //标志这个片文件是否已经解码

    //初次编码时存数据调用
    public PieceFile(String path, int pieceNo, int nK) {
        this.nK = nK;
        //构建pieceFile文件目录
        this.pieceNo = pieceNo;
        pieceFilePath = MyFileUtils.creatFolder(path, pieceNo + "");
        piece_recover_file_path = pieceFilePath + File.separator + pieceNo + ".decode";
        //创建二级目录
        pieceEncodeFilePath = MyFileUtils.creatFolder(pieceFilePath, "pieceEncodeFile");
        ready_to_send_path = MyFileUtils.creatFolder(pieceFilePath, "ready_to_send");

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

    public File getFile_ready_to_send() {
        return file_ready_to_send;
    }

    public void setFile_ready_to_send(File file_ready_to_send) {

        this.file_ready_to_send = file_ready_to_send;
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
            json_pfile_config.put("pieceDecoded", pieceDecoded);
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
        return piecesEncodeFiles;
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


    public boolean isPieceDecoded() {
        return pieceDecoded;
    }

    public void setPieceDecoded(boolean pieceDecoded) {
        this.pieceDecoded = pieceDecoded;
    }

    public String getPiece_recover_file_path() {
        return piece_recover_file_path;
    }

    public void setPiece_recover_file_path(String piece_recover_file_path) {
        this.piece_recover_file_path = piece_recover_file_path;
    }
}
