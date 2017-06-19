package nc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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
                FileInputStream stream = new FileInputStream(files.get(0));
                stream.read(b);
                //读入一行系数矩阵
                stream.read(CoefficientMatrix[i]);
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



    public static boolean haveUse

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

        int fileLen = (int) (file.length());
        int file_piece_len = 10 * 1024 * 1024;  //若是大于10M的文件，对文件进行分片，每片10M
        int piece_num = fileLen / file_piece_len + (fileLen % file_piece_len != 0 ? 1 : 0);
        encodeFile.setTotalPiecesNum(piece_num);
        encodeFile.setPiecesNum(piece_num);   //设置文件片数
        byte[][] _10m_file_data = new byte[piece_num - 1][fileLen];  //每片10M的数据
        int rest_len = fileLen - file_piece_len * (piece_num - 1);  //最后一片的长度
        byte[] rest_file_data = new byte[rest_len];          //最后一片数据
        InputStream in;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            //读文件出错
            e.printStackTrace();
            return;
        }

        //读取文件到数组
        for (int i = 0; i < piece_num; ++i) {
            if (i == (piece_num - 1)) {
                try {
                    in.read(rest_file_data, 0, rest_len);    //读取文件中的内容到b[]数组
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    //参数0 file_piece_len 含义是从文件读取file_piece_len个字节，
                    // 放在byte[]数组0到file_piece_len-1处
                    //注意：连续读取时，这两个参数必需指定，不然下次再读失败
                    in.read(_10m_file_data[i], 0, file_piece_len);    //读取文件中的内容到b[]数组
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //编码数据   针对每一片在进行K次分割
        for (int i = 0; i < piece_num; ++i) {

            if (i == (piece_num - 1)) {
                //创建存储路径
                PieceFile pieceFile = new PieceFile(storagePath, i+1, K);

                int perLen = rest_len / K + (rest_len % K != 0 ? 1 : 0);
                int col = 1 + K + perLen;
                byte[][] data = new byte[K][col];
                int iFlag = 0;   //作为数组下标标识
                //对数据封装 K+单位矩阵+数据
                for (int m = 0; m < K; ++m) {
                    data[m][0] = (byte) K;
                    data[m][m + 1] = 1;       //在此其实是存入一个单位矩阵
                    for (int n = K + 1; n < col; ++n) {
                        data[m][n] = rest_file_data[iFlag];
                        ++iFlag;
                        //数据已读完
                        if (iFlag == rest_len) {
                            break;
                        }
                    }
                    //此处写入文件   作为一个编码数据
                    MyFileUtils.writeToFile(pieceFile.getPieceEncodeFilePath(), (i+1) + "_" + (m+1) + ".nc", data[m]);
                }
                pieceFile.setPieceFileNum(K);
                pieceFile.setHaveNeedFile(true);
                pieceFile.setPiecesEncodeFiles();
                pieceFile.setCoefMatrix(getUnitMatrix(K));
                pieceFile.setPieceDecoded(true);
                re_encode_file(pieceFile);    //设置再编码文件用以发送
                pieceFile.setJson_pfile_config();
                //添加到总目录
                encodeFile.add2myPiecesFiles(pieceFile);

            } else {
                //创建存储路径
                PieceFile pieceFile = new PieceFile(storagePath, i+1, K);

                int perLen = file_piece_len / K + (file_piece_len % K != 0 ? 1 : 0);
                int col = 1 + K + perLen;
                byte[][] data = new byte[K][col];
                int iFlag = 0;
                //对数据封装 K+单位矩阵+数据
                for (int m = 0; m < K; ++m) {
                    data[m][0] = (byte) K;
                    data[m][m + 1] = 1;       //在此其实是存入一个单位矩阵
                    for (int n = K + 1; n < col; ++n) {
                        data[m][n] = _10m_file_data[i][iFlag];
                        ++iFlag;
                        //数据已读完
                        if (iFlag == file_piece_len) {
                            break;
                        }
                    }
                    //此处写入文件   作为一个编码数据

                    MyFileUtils.writeToFile(pieceFile.getPieceEncodeFilePath(), (i+1) + "_" + (m+1) + ".nc", data[m]);
                }
                pieceFile.setPieceFileNum(K);
                pieceFile.setHaveNeedFile(true);
                pieceFile.setPiecesEncodeFiles();
                pieceFile.setCoefMatrix(getUnitMatrix(K));
                pieceFile.setPieceDecoded(true);
                re_encode_file(pieceFile);    //设置再编码文件用以发送
                pieceFile.setJson_pfile_config();

                //添加到总目录
                encodeFile.add2myPiecesFiles(pieceFile);

            }
        }
        encodeFile.setFileDecode(true);
        encodeFile.setHaveAllNeedFile(true);
        encodeFile.setJson_config();
        //将配置写入文件
        encodeFile.setJson_file();

    }



    /**
     * 再编码文件   只生成一个再编码文件
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
        reEncodeData = Reencode(fileData, 1, fileLen);
        NCUtil.unlocked();


        //写入sendFilePath文件夹中
        String ready_to_send_path = pieceFile.getReady_to_send_path();
        int pieceNo=pieceFile.getPieceNo();
        String fileName=pieceNo+"_re.nc";
        File file=MyFileUtils.writeToFile(ready_to_send_path,fileName,reEncodeData[0]);
        pieceFile.setFile_ready_to_send(file);

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

        MyFileUtils.writeToFile(pieceFile.getPieceFilePath(), pieceFile.getPieceNo()+".decode", originData);
        pieceFile.setPieceDecoded(true);   //标识已经解码
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
    public static native byte[][] Reencode(byte[][] buffer, int nPart, int nLength);

    //解码函数
    public static native byte[][] Decode(byte[][] buffer, int nPart, int nLength);

    //求秩函数
    public static native int getRank(byte[][] matrix, int nRow, int nCol);
}
