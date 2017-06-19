package nc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import utils.MyFileUtils;

/**
 * 此类用于管理编码文件
 * Created by Administrator on 2017/6/16 0016.
 */


public class EncodeFile {
    private String fileName;
    private boolean haveAllNeedFile = false;
    private int totalPiecesNum;   //一共多少片
    private int piecesNum;        //已有多少片
    private int nK;       //每片需要的编码文件的个数
    private boolean fileDecode = false;
    private String FolderName;
    private String FolderPath;
    private ArrayList<PieceFile> myPiecesFiles = new ArrayList<PieceFile>();   //需注意初始化问题

    private JSONObject json_config = new JSONObject();
    private File json_file;       //用来存储配置文件  其中是一个json变量  文件名为json.txt

    private String originFilePath;  //用以存放解码出来的原始文件

    public EncodeFile(String path, String fileName, int nK) {
        this.fileName = fileName;
        this.nK = nK;
        FolderName = fileName.substring(0, fileName.lastIndexOf("."));   //获取不含后缀的文件名,作为文件夹名字
        FolderPath = MyFileUtils.creatFolder(path, FolderName);
        originFilePath = FolderPath + File.separator + fileName;

    }

    //此方法做接收json文件后，获取信息用，不创建文件夹、文件
    public EncodeFile() {

    }


    /**
     * 尝试解码文件
     */
    private boolean try2decode() {
        //已经解码
        if (fileDecode) {
            return false;
        }
        //文件不够不能解码
        if (piecesNum < totalPiecesNum) {
            return false;
        }
        String[] partFiles = new String[totalPiecesNum];
        for (PieceFile pieceFile : myPiecesFiles) {
            int pieceNo = pieceFile.getPieceNo();
            if (pieceFile.isPieceDecoded()) {
                partFiles[pieceNo] = pieceFile.getPiece_recover_file_path();
                continue;
            }
            if (pieceFile.getPieceFileNum() < nK) {
                //文件数目不够  不能解码
                return false;
            }
            NCUtil.decode_file(pieceFile);
            partFiles[pieceNo] = pieceFile.getPiece_recover_file_path();
        }
        //合并文件
        MyFileUtils.mergeFiles(originFilePath,partFiles);
        return true;
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFolderPath() {
        return FolderPath;
    }

    public void setFolderPath(String folderPath) {
        FolderPath = folderPath;
    }

    public JSONObject getJson_config() {
        return json_config;
    }

    public void setJson_config() {
        try {
            json_config.put("fileName", fileName);
            json_config.put("haveAllNeedFile", haveAllNeedFile);
            json_config.put("totalPiecesNum", totalPiecesNum);
            json_config.put("piecesNum", piecesNum);
            json_config.put("nK", nK);
            json_config.put("fileDecode", fileDecode);
            //存入每片数据的信息
            JSONArray jsonArray = new JSONArray();
            for (PieceFile pieceFile : myPiecesFiles) {
                //放入
                jsonArray.put(pieceFile.getJson_pfile_config());
            }
            json_config.put("pieceFileInfor", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }




    }

    public File getJson_file() {
        return json_file;
    }

    public void setJson_file() {
        //写入文件
        String s = json_config.toString();
        json_file = MyFileUtils.writeToFile(FolderPath, "json.txt", s.getBytes());
    }

    public ArrayList<PieceFile> getMyPiecesFiles() {
        return myPiecesFiles;
    }

    public void setMyPiecesFiles(ArrayList<PieceFile> myPiecesFiles) {
        this.myPiecesFiles = myPiecesFiles;
    }

    public void add2myPiecesFiles(PieceFile pieceFile) {
        //添加到list
        myPiecesFiles.add(pieceFile);
    }


    public int getnK() {
        return nK;
    }

    public void setnK(int nK) {
        this.nK = nK;
    }

    public int getPiecesNum() {
        return piecesNum;
    }

    public void setPiecesNum(int piecesNum) {
        this.piecesNum = piecesNum;
    }

    public int getTotalPiecesNum() {
        return totalPiecesNum;
    }

    public void setTotalPiecesNum(int totalPiecesNum) {
        this.totalPiecesNum = totalPiecesNum;
    }

    public boolean isHaveAllNeedFile() {
        return haveAllNeedFile;
    }

    public void setHaveAllNeedFile(boolean haveAllNeedFile) {
        this.haveAllNeedFile = haveAllNeedFile;
    }

    public boolean isFileDecode() {
        return fileDecode;
    }

    public void setFileDecode(boolean fileDecode) {
        this.fileDecode = fileDecode;
    }

    public String getOriginFilePath() {
        return originFilePath;
    }

    public void setOriginFilePath(String originFilePath) {
        this.originFilePath = originFilePath;
    }

    /**
     * 解析json文件   获取编解码信息
     *
     * @return
     */
    public static EncodeFile parse_JSON_File(String filePath) {
        File file_json=new File(filePath);
        if(!file_json.exists()){
            //文件不存在
            return null;
        }

        String sets = new String(MyFileUtils.readFile(file_json));
        JSONObject json_config = null;
        try {
            json_config = new JSONObject(sets);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String fileName = null;
        boolean haveAllNeedFile = false;
        int totalPiecesNum = 0;
        int piecesNum = 0;
        int nK = 0;
        //文件片的信息
        JSONArray jsonArray = new JSONArray();
        boolean fileDecode = false;
        try {
            fileName = json_config.getString("fileName");
            haveAllNeedFile = json_config.getBoolean("haveAllNeedFile");
            totalPiecesNum = json_config.getInt("totalPiecesNum");
            piecesNum = json_config.getInt("piecesNum");
            nK = json_config.getInt("nK");
            fileDecode = json_config.getBoolean("fileDecode");
            jsonArray = json_config.getJSONArray("pieceFileInfor");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //恢复发送端编码文件的配置信息
        EncodeFile encodeFile = new EncodeFile();
        encodeFile.setFileName(fileName);
        encodeFile.setHaveAllNeedFile(haveAllNeedFile);
        encodeFile.setTotalPiecesNum(totalPiecesNum);
        encodeFile.setPiecesNum(piecesNum);
        encodeFile.setnK(nK);
        encodeFile.setFileDecode(fileDecode);
        //encodeFile.setJson_config();

        //解析文件片信息
        for (int i = 0; i < piecesNum; ++i) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int pieceNo = jsonObject.getInt("pieceNo");
                boolean haveNeedFile = jsonObject.getBoolean("haveNeedFile");
                int pieceFileNum = jsonObject.getInt("pieceFileNum");
                boolean pieceDecoded = jsonObject.getBoolean("pieceDecoded");
                JSONArray jsonArray_coef = jsonObject.getJSONArray("coefMatrix");

                byte[][] coefMatrix = new byte[pieceFileNum][nK];
                for (int m = 0; m < pieceFileNum; ++m) {
                    JSONArray row_jsonArray=jsonArray_coef.getJSONArray(m);
                    for (int n = 0; n < nK; ++n) {
                        coefMatrix[m][n] = (byte) row_jsonArray.getInt(n);
                    }
                }
                PieceFile pieceFile = new PieceFile();
                pieceFile.setPieceNo(pieceNo);
                pieceFile.setHaveNeedFile(haveNeedFile);
                pieceFile.setPieceFileNum(pieceFileNum);
                pieceFile.setCoefMatrix(coefMatrix);
                pieceFile.setnK(nK);
                pieceFile.setPieceDecoded(pieceDecoded);
                encodeFile.add2myPiecesFiles(pieceFile);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return encodeFile;
    }
}
