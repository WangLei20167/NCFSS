package nc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import utils.LocalInfor;
import utils.MyFileUtils;


/**
 * 此类用于网络编码相关操作
 * 对JNI函数需要互斥访问，因此需要开锁与解锁
 * Created by Administrator on 2017/6/14 0014.
 */

public class NCUtil {
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    //实现锁
    private static boolean jni_can_visit = true;


    /**
     * 从文件中取出系数矩阵
     * 文件的格式是 K+K个编码系数+编码数据
     *
     * @param files
     * @return
     */
    public static byte[][] getCoefficientMatrix(ArrayList<File> files) {
        int fileNum = files.size();
        if (fileNum == 0) {
            //没有文件
            return null;
        }

        int nK = 0;
        try {
            //从文件读取nK值，在第一个字节
            FileInputStream stream = new FileInputStream(files.get(0));
            byte[] b = new byte[1];
            stream.read(b, 0, 1);
            nK = (int) b[0];
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[][] CoefficientMatrix = new byte[fileNum][nK];
        byte[] b = new byte[1];
        for (int i = 0; i < fileNum; ++i) {
            try {
                //从文件读取nK值，在第一个字节
                FileInputStream stream = new FileInputStream(files.get(i));
                stream.read(b, 0, 1);
                //读入一行系数矩阵
                stream.read(CoefficientMatrix[i], 0, 4);
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return CoefficientMatrix;
    }

    //此锁针对互斥访问jni
    //检查锁，等待进入，锁上
    public static void locked() {
        while (!jni_can_visit) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        jni_can_visit = false;
    }

    //解锁
    public static void unlocked() {
        NCUtil.jni_can_visit = true;
    }


    /**
     * 根据矩阵的秩判断是否有需要的数据
     *
     * @param RevMatrix 接收到的编码系数
     * @param localCoef 本地编码系数
     * @return
     */
    public static boolean havaUsefulData(byte[][] RevMatrix, int Row, int nK, byte[][] localCoef, int row) {
        int rank_origin = Row;    //原有数据为行满秩矩阵
        byte[][] test_matrix = new byte[Row + 1][nK];

        for (int i = 0; i < Row; ++i) {
            for (int j = 0; j < nK; ++j) {
                test_matrix[i][j] = RevMatrix[i][j];
            }
        }
        //按行检查数据是否使现有矩阵秩增加
        for (int i = 0; i < row; ++i) {
            for (int j = 0; j < nK; ++j) {
                test_matrix[Row][j] = localCoef[i][j];
            }
            locked();
            int rank = getRank(test_matrix, Row + 1, nK);
            unlocked();
            if (rank == (rank_origin + 1)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对文件进行编码
     * 注意：此方法不访问jni，是用单位矩阵进行拼接编码数据
     * 当文件大于10M时，对文件进行拆分
     *
     * @param file
     * @param encodeFile 管理编码文件
     */
    public static void encode_file(File file, EncodeFile encodeFile) {
        // int N = 4;
        // int K = 4;
        int K = encodeFile.getnK();
        String storagePath = encodeFile.getFolderPath();

        String dataTempPath = encodeFile.getDataTempPath();

        int fileLen = (int) (file.length());
        int file_piece_len = 10 * 1024 * 1024;  //若是大于10M的文件，对文件进行分片，每片10M
        int piece_num = fileLen / file_piece_len + (fileLen % file_piece_len != 0 ? 1 : 0);
        encodeFile.setTotalPiecesNum(piece_num);
        encodeFile.setPiecesNum(piece_num);   //设置文件片数
        encodeFile.setTotalSmallPieceNum(piece_num * K);
        encodeFile.setCurrentSmallPieceNum(piece_num * K);
        //创建piece_num个文件用来暂存数据
        ArrayList<File> temp_pFiles = new ArrayList<File>();
        int rest_len = fileLen - file_piece_len * (piece_num - 1);  //最后一片的长度
        InputStream in;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            //读文件出错
            e.printStackTrace();
            return;
        }

        //读取文件到片文件
        for (int i = 0; i < piece_num; ++i) {
            if (i == (piece_num - 1)) {
                //创建一个文件用于写入这一片数据
                File piece_file = MyFileUtils.splitFile(in, dataTempPath, (i + 1) + ".piece", rest_len);
                temp_pFiles.add(piece_file);
                try {
                    //关闭文件流
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            } else {
                //创建一个文件用于写入这一片数据
                File piece_file = MyFileUtils.splitFile(in, dataTempPath, (i + 1) + ".piece", file_piece_len);
                temp_pFiles.add(piece_file);
            }
        }

        //编码数据   针对每一片在进行K次分割
        for (int i = 0; i < piece_num; ++i) {
            if (i == (piece_num - 1)) {
                //创建存储路径
                PieceFile pieceFile = new PieceFile(storagePath, i + 1, K);

                int perLen = rest_len / K + (rest_len % K != 0 ? 1 : 0);
                File origin_file = temp_pFiles.get(i);
                InputStream inputStream;
                try {
                    inputStream = new FileInputStream(origin_file);
                } catch (FileNotFoundException e) {
                    //读文件出错
                    e.printStackTrace();
                    return;
                }
                String path = pieceFile.getPieceEncodeFilePath();
                //对数据封装 K+单位矩阵+数据
                for (int m = 0; m < K; ++m) {
                    byte[] b = new byte[1 + K];
                    b[0] = (byte) K;
                    b[m + 1] = 1;
                    String fileName = (i + 1) + "_" + (m + 1) + ".nc";
                    File piece_file = MyFileUtils.creatFile(path, fileName);
                    try {
                        FileOutputStream fos = new FileOutputStream(piece_file);
                        fos.write(b);    //写入文件
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    MyFileUtils.splitFile(inputStream, path, fileName,perLen);
                }
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                pieceFile.setPieceFileNum(K);
                pieceFile.setHaveNeedFile(true);
                pieceFile.setPiecesEncodeFiles();
                pieceFile.setCoefMatrix(getUnitMatrix(K));
                re_encode_file(pieceFile);    //设置再编码文件用以发送
                pieceFile.setJson_pfile_config();
                //添加到总目录
                encodeFile.add2myPiecesFiles(pieceFile);

            } else {
                //创建存储路径
                //创建存储路径
                PieceFile pieceFile = new PieceFile(storagePath, i + 1, K);

                int perLen = file_piece_len / K + (file_piece_len % K != 0 ? 1 : 0);
                File origin_file = temp_pFiles.get(i);
                InputStream inputStream;
                try {
                    inputStream = new FileInputStream(origin_file);
                } catch (FileNotFoundException e) {
                    //读文件出错
                    e.printStackTrace();
                    return;
                }
                String path = pieceFile.getPieceEncodeFilePath();
                //对数据封装 K+单位矩阵+数据
                for (int m = 0; m < K; ++m) {
                    byte[] b = new byte[1 + K];
                    b[0] = (byte) K;
                    b[m + 1] = 1;
                    String fileName = (i + 1) + "_" + (m + 1) + ".nc";
                    File piece_file = MyFileUtils.creatFile(path, fileName);
                    try {
                        FileOutputStream fos = new FileOutputStream(piece_file);
                        fos.write(b);    //写入文件
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    MyFileUtils.splitFile(inputStream, path, fileName,perLen);
                }
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                pieceFile.setPieceFileNum(K);
                pieceFile.setHaveNeedFile(true);
                pieceFile.setPiecesEncodeFiles();
                pieceFile.setCoefMatrix(getUnitMatrix(K));
                re_encode_file(pieceFile);    //设置再编码文件用以发送
                pieceFile.setJson_pfile_config();
                //添加到总目录
                encodeFile.add2myPiecesFiles(pieceFile);
            }
        }
        //删除缓存的数据
        MyFileUtils.deleteAllFile(dataTempPath,false);
        encodeFile.setHaveAllNeedFile(true);
        //设置json变量 并将配置写入文件
        encodeFile.setJson_config();

    }


    /**
     * 再编码文件   只生成一个再编码文件
     *
     * @param pieceFile
     */
    public static void re_encode_file(PieceFile pieceFile) {

        int fileNum = pieceFile.getPieceFileNum();
        ArrayList<File> files = pieceFile.getPiecesEncodeFiles();

        if (fileNum == 0) {
            //没有文件
            return;
        }

        int fileLen = (int) (files.get(0).length());  //注意：用于再编码的文件长度必定都是一样的
        //用于存文件数组
        byte[][] fileData = new byte[fileNum][fileLen];

        for (int i = 0; i < fileNum; ++i) {
            File file = files.get(i);
            try {
                InputStream in = new FileInputStream(file);
                //b = new byte[fileLen];
                in.read(fileData[i]);    //读取文件中的内容到b[]数组
                in.close();
            } catch (IOException e) {
                //Toast.makeText(this, "读取文件异常", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return;
            }

        }

        NCUtil.locked();
        //存再编码结果
        byte[][] reEncodeData = new byte[1][fileLen];
        reEncodeData = Reencode(fileData, fileNum, fileLen, 1);
        NCUtil.unlocked();


        //写入sendFilePath文件夹中
        String ready_to_send_path = pieceFile.getReady_to_send_path();
        //删除之前的再编码文件
        MyFileUtils.deleteAllFile(ready_to_send_path, false);
        int pieceNo = pieceFile.getPieceNo();
        String fileName = pieceNo + "." + LocalInfor.getCurrentTime("MMddHHmmss") + ".nc"; //pieceNo.time.re  //格式
        File file = MyFileUtils.writeToFile(ready_to_send_path, fileName, reEncodeData[0]);
        pieceFile.setReady_to_send_file(file);

    }


    /**
     * 解码数据   恢复一片文件的原始数据
     *
     * @param pieceFile
     * @return
     */
    public static void decode_file(PieceFile pieceFile) {
        ArrayList<File> files = pieceFile.getPiecesEncodeFiles();
        int fileNum = files.size();
        if (fileNum == 0) {
            //没有文件
            return;
        }
        int nK = pieceFile.getnK();

        int fileLen = (int) (files.get(0).length());  //注意：用于再编码的文件长度必定都是一样的
        //用于存文件数组
        byte[][] fileData = new byte[nK][fileLen];   //如果文件很多，也只需nK个文件

        for (int i = 0; i < nK; ++i) {
            File file = files.get(i);
            try {
                InputStream in = new FileInputStream(file);
                //b = new byte[fileLen];
                in.read(fileData[i]);    //读取文件中的内容到b[]数组
                in.close();
            } catch (IOException e) {
                //Toast.makeText(this, "读取文件异常", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return;
            }

        }

        NCUtil.locked();
        //存再编码结果
        int col = fileLen - 1 - nK;
        byte[][] origin_data = new byte[nK][col];
        origin_data = NCUtil.Decode(fileData, nK, fileLen);
        NCUtil.unlocked();


        //二维转化为一维
        int origin_file_len = nK * col;
        byte[] originData = new byte[origin_file_len];
        int ii = 0;
        for (int i = 0; i < nK; ++i) {
            for (int j = 0; j < col; ++j) {
                originData[ii] = origin_data[i][j];
                ++ii;
            }
        }

        //写入
        File file = MyFileUtils.writeToFile(pieceFile.getPieceFilePath(), pieceFile.getPieceNo() + ".decode", originData);
        pieceFile.setPiece_recover_file(file);
    }

    /**
     * 获取一个单位矩阵
     *
     * @param n
     * @return
     */
    public static byte[][] getUnitMatrix(int n) {
        byte[][] unitMatrix = new byte[n][n];
        for (int i = 0; i < n; ++i) {
            unitMatrix[i][i] = 1;
        }
        return unitMatrix;
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //编码函数
    public static native byte[][] Encode(byte[] buffer_, int N, int K, int nLen);

    //再编码函数,nLength为编码文件的总长（1+K+len)
    public static native byte[][] Reencode(byte[][] buffer, int nPart, int nLength, int outputNum);

    //解码函数
    public static native byte[][] Decode(byte[][] buffer, int nPart, int nLength);

    //求秩函数
    public static native int getRank(byte[][] matrix, int nRow, int nCol);
}
