package com.yeapao.brake.serialport;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.friendlyarm.AndroidSDK.HardwareControler;
import com.scottfu.sflibrary.util.LogUtil;
import com.scottfu.sflibrary.util.ToastManager;

import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by fujindong on 2017/8/25.
 */

public class SerialProtDataUtil {

    private static final String TAG = "SerialPort";

    //读卡器  读卡顺序 1438 激活卡 1439读卡
    private static final String READ_CARD_1 = "AA660002101000";
    private static final String READ_CARD_2 = "AA66000214382C";
    private static final String READ_CARD_3 = "AA660009143901FFFFFFFFFFFF2C";

    private final int MAXLINES = 200;
    private StringBuilder remoteData = new StringBuilder(256 * MAXLINES);

    private String devName = "/dev/s3c2410_serial3";
    private int speed = 9600;
    private int dataBits = 8;
    private int stopBits = 1;
    private int devfd = -1;

    private Context mContext;

    private OutputAccountListener mOutputListener;


    private static final int ACRIVITE_CARD = 1;
    private static final int READ_CARD = 2;
    private int step = ACRIVITE_CARD;


    public SerialProtDataUtil(Context context) {
        mContext = context;
    }

    public void initSerialProt() {
        openSerial();
    }

    public void setSerialPortDataListener(OutputAccountListener outputAccountListener) {
        mOutputListener = outputAccountListener;
    }


    /**
     * aa990000102020202020202020202020203830323309
     * @param string
     */
    private void decodeAccount(String string) {
//        aa990000102020202020202020202020203830323309
        int length = string.length();
        String status = string.substring(4, 6);


        if (status.equals("00")) {
            String content = string.substring(10, length - 2);
            LogUtil.e("status", status + "----" + content);
            boolean flag = CHexConver.checkHexStr(content);
            if (flag) {
                String string1 = CHexConver.hexStr2Str(content);
                String string2 = string1.replace(" ", "");
                String length1 = String.valueOf(string2.length());
                LogUtil.e("hextostr", length1);
                LogUtil.e("hextostr", string2);

                mOutputListener.printAccount(string2);


            } else {
                LogUtil.e("hextostr", "fail to hextostr");
                mOutputListener.onFail();
            }
            step = ACRIVITE_CARD;
        } else {
            LogUtil.e("account", "获取失败");
            mOutputListener.onFail();
            step = ACRIVITE_CARD;
        }

    }

    private void checkStatus(String string) {
        int length = string.length();
        if (length > 10) {
            String status = string.substring(4, 6);
            if (status.equals("00")) {
                LogUtil.e("checkStatus", "检测到卡" + string);
                sendHexStr(READ_CARD_3);
                step = READ_CARD;
            } else {
                LogUtil.e("checkStatus", "未检测到卡");
            }
        }
    }


    private final int BUFSIZE = 512;
    private byte[] buf = new byte[BUFSIZE];
    private Timer timer = new Timer();

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (HardwareControler.select(devfd, 0, 0) == 1) {

                        int retSize = HardwareControler.read(devfd, buf, BUFSIZE);
                        if (retSize > 0) {
                            LogUtil.e("retSize", String.valueOf(retSize));
                            String str = bytesToHexString(buf);
                            LogUtil.e("remoteData", str);
                            LogUtil.e("remoteData", str.substring(0, retSize * 2));
                            remoteData.append(str.substring(0, retSize * 2));

                            if (step == ACRIVITE_CARD) {
                                checkStatus(remoteData.toString());
                            } else {
                                decodeAccount(remoteData.toString());
                            }
                        }
                    }
                    remoteData.setLength(0);
                    break;
            }
            super.handleMessage(msg);
        }
    };


    /**
     * 外层直接调用读卡，外部不做任何处理
     */
    public void readCard() {
        if (step == READ_CARD) {

        } else {
            sendHexStr(READ_CARD_2);
        }
    }


    public void sendHexStr(String hex) {
        byte[] bytes = hexStringToByteArray(hex);
        int ret = HardwareControler.write(devfd, bytes);
        if (ret > 0) {
            LogUtil.e("sendHexStr", hex);
        } else {
            ToastManager.showToast(mContext, "Fail  to send!");
        }
    }


    private TimerTask task = new TimerTask() {
        public void run() {
            Message message = new Message();
            message.what = 1;
            handler.sendMessage(message);
        }
    };

    private void openSerial() {
        devfd = HardwareControler.openSerialPort(devName, speed, dataBits, stopBits);
        if (devfd >= 0) {
            timer.schedule(task, 0, 500);//
        } else {
            devfd = -1;
            ToastManager.showToast(mContext, "Fail to open " + devName + "!");
        }
    }


    /**
     * byte[]数组转换为16进制的字符串
     *
     * @param bytes 要转换的字节数组
     * @return 转换后的结果
     */
    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 16进制表示的字符串转换为字节数组
     *
     * @param s 16进制表示的字符串
     * @return byte[] 字节数组
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] b = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            // 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个字节
            b[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
                    .digit(s.charAt(i + 1), 16));
        }
        return b;
    }

}
