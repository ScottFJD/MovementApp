package com.yeapao.brake;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.volley.VolleyError;
import com.friendlyarm.AndroidSDK.GPIOEnum;
import com.friendlyarm.AndroidSDK.HardwareControler;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.FakeTimeLimiter;
import com.google.gson.Gson;
import com.scottfu.sflibrary.net.CloudClient;
import com.scottfu.sflibrary.net.JSONResultHandler;
import com.scottfu.sflibrary.util.LogUtil;
import com.scottfu.sflibrary.util.ToastManager;
import com.yeapao.brake.api.ConstantBrake;
import com.yeapao.brake.api.NetImpl;
import com.yeapao.brake.bean.AccountMessage;
import com.yeapao.brake.bean.CheckInOrOutModel;
import com.yeapao.brake.bean.ScreenModel;
import com.yeapao.brake.serialport.OutputAccountListener;
import com.yeapao.brake.serialport.SerialProtDataUtil;
import com.yeapao.brake.vbar.Vbar;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by fujindong on 2017/7/7.
 */

public class BrakePresenter implements BrakeContract.Presenter {


    private static final String TAG = "BrakePresenter";

//读卡器  读卡顺序 1438 激活卡 1439读卡
    private static final String READ_CARD_1 = "AA660002101000";
    private static final String READ_CARD_2 = "AA66000214382C";
    private static final String READ_CARD_3 = "AA660009143901FFFFFFFFFFFF2C";

    private BrakeContract.View mView;
    private Context mContext;
    private Gson gson = new Gson();


//    二维码
    private String decodeStr = "";
    private boolean vBarStatus = false;
    private Vbar vbar = new Vbar();

    //串口定义
    private SerialProtDataUtil serialProtDataUtil;


//    GPIO
    static int STEP_INIT_GPIO_DIRECTION = 1;
    static int STEP_CLOSE_ALL_LED = 2;
    static int STEP_INIT_VIEW = 3;

    private boolean flag = false;

    private Timer timer = new Timer();
    private int step = 0;
    private int led_gpio_base = 79;


    private boolean checkAccountStatus = true;

    public BrakePresenter(Context context, BrakeContract.View view) {
        mContext = context;
        mView = view;
        mView.setPresenter(this);
    }


    private void initSerialProt() {
        serialProtDataUtil = new SerialProtDataUtil(mContext);
        serialProtDataUtil.initSerialProt();
        serialProtDataUtil.setSerialPortDataListener(new OutputAccountListener() {
            @Override
            public void printAccount(String Account) {
                LogUtil.e(TAG,"Account="+Account);
                checkAccountStatus = false;
                getData(Account);

            }

            @Override
            public void onFail() {
                LogUtil.e("onFail","信息错误，请至前台");
//                ToastManager.showToast(mContext,"信息错误，请至前台");
                checkAccountStatus = true;
            }
        });

        startReadCard();

    }


    private void startReadCard() {
        // 读卡 间隔500ms
        Thread t = new Thread(){
            @Override
            public void run() {
                // TODO Auto-generated method stub
                super.run();
                while(true)
                {
                    ((Activity)mContext).runOnUiThread(new Runnable() {
                        public void run() {


                            if (checkAccountStatus) {
                                serialProtDataUtil.readCard();
                            } else {

                            }


                        }
                    });
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        };
        t.start();
    }


    private void initVbar() {
        vBarStatus = vbar.vbarOpen("127.0.0.1", 0);
        if (vBarStatus) {
            ToastManager.showToast(mContext, "扫码设备打开成功");
        } else {
            ToastManager.showToast(mContext,"扫码设备打开失败");
        }
// 扫码线程 间隔500ms
        Thread t = new Thread(){
            @Override
            public void run() {
                // TODO Auto-generated method stub
                super.run();
                while(true)
                {
                    final String str = vbar.vbarScan();
                    ((Activity)mContext).runOnUiThread(new Runnable() {
                    public void run() {
                        if(str != null)
                        {
//                            decodeStr=(str + "\r\n");
                            decodeStr = str;
                            ToastManager.showToast(mContext,decodeStr);
                        }
                    }
                });
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        };
        t.start();


    }


    private void openBrake() {
        Thread t = new Thread(){
            @Override
            public void run() {
                // TODO Auto-generated method stub
                super.run();
                HardwareControler.PWMPlay(40000);
                    try {
                        Thread.sleep(1000);
                        HardwareControler.PWMStop();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

            }
        };
        t.start();
    }

    @Override
    public void start() {
        initVbar();
        initSerialProt();
    }



    @Override
    public void getData(String userId) {
        CloudClient.doHttpRequestV4(mContext, ConstantBrake.GET_USER_MESSAGE, NetImpl.getInstance().setUserParams(userId), null, new JSONResultHandler() {
            @Override
            public void onSuccess(String jsonString) {
                Log.e("success", jsonString);
//                TODO {"rs":"没有查询到会员信息"}
                try {
                CheckInOrOutModel accountMessage = gson.fromJson(jsonString, CheckInOrOutModel.class);
                if (accountMessage.getFlag().equals("CHECKIN") || accountMessage.getFlag().equals("CHECKOUT")) {
//                    HardwareControler.PWMPlay(40000);
//                    HardwareControler.PWMStop();
                    openBrake();
                    mView.showResult(accountMessage);
                    checkAccountStatus = true;
                    getScreenData(accountMessage.getUser().getId());
                } else {
                    LogUtil.e("getData","获取用户信息失败");
                    checkAccountStatus = true;
                }
                } catch (Exception e) {
                    e.printStackTrace();
                    LogUtil.e("getData","获取用户信息失败");
                    ToastManager.showToast(mContext,"用户信息错误，请至前台处理");
                    checkAccountStatus = true;

                }
            }

            @Override
            public void onError(VolleyError errorMessage) {
                checkAccountStatus = true;
                Log.e("error", errorMessage.toString());
                ToastManager.showToast(mContext,errorMessage.toString());
            }
        });
    }


    private void getScreenData(String uid) {
        CloudClient.doHttpRequestV4(mContext, ConstantBrake.GET_USER_SCREEN, NetImpl.getInstance().getScreenData(uid), null, new JSONResultHandler() {
            @Override
            public void onSuccess(String jsonString) {
                LogUtil.e("ScreenData", jsonString);
                ScreenModel screenModel = gson.fromJson(jsonString, ScreenModel.class);
                if (screenModel.getRs().equals("Y")) {
                    mView.showScreenResult(screenModel);
                }
            }

            @Override
            public void onError(VolleyError errorMessage) {

            }
        });
    }

    @Override
    public void readCard() {
//        serialProtDataUtil.sendHexStr(READ_CARD_3);
        serialProtDataUtil.readCard();
    }




    @Override
    public void outPutGPIO() {


//        HardwareControler.setLedState(0, 1);
//暂且用PWM代替 GPLO输出控制
        if (!flag) {
            HardwareControler.PWMPlay(40000);
            flag = true;
        } else {
            HardwareControler.PWMStop();
            flag = false;
        }




//        int pin = led_gpio_base+1;
//        if (HardwareControler.exportGPIOPin(pin) == 0) {
//            ToastManager.showToast(mContext, "success");
//            if (HardwareControler.setGPIODirection(pin, GPIOEnum.OUT) == 0) {
//                LogUtil.e("GPIO", "out");
//                if (HardwareControler.setGPIOValue(pin, GPIOEnum.HIGH) == 0) {
//                    LogUtil.e("GPIO", "Low");
//                } else {
//                    LogUtil.e("GPIO", "Failed_Low");
//                }
//            } else {
//                LogUtil.e("GPIO", "failed");
//            }
//        } else {
//            ToastManager.showToast(mContext, "Failed");
//        }

//        step = STEP_INIT_GPIO_DIRECTION;
//        timer.schedule(init_task, 100, 100);
    }


    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    timer.cancel();
                    // Generate list View from ArrayList
//                    displayListView();
                    break;
            }
            super.handleMessage(msg);
        }
    };


    private TimerTask init_task = new TimerTask() {
        public void run() {
            Log.d("GPIO", "init_task " + step);
            if (step == STEP_INIT_GPIO_DIRECTION) {
                for (int i=0; i<1; i++) {
                    if (HardwareControler.setGPIODirection(led_gpio_base+i, GPIOEnum.OUT) == 0) {
                        if (HardwareControler.setGPIOValue(led_gpio_base+i, GPIOEnum.LOW) == 0) {
                            ToastManager.showToast(mContext, "opensuccess");
                        } else {
                            ToastManager.showToast(mContext,"openFailed");
                        }
                    } else {
                        Log.v("TimerTask", String.format("setGPIODirection(%d) failed", led_gpio_base+i));
                    }
                }
//                step ++;
            } else if (step == STEP_CLOSE_ALL_LED) {
                for (int i=0; i<4; i++) {
                    if (HardwareControler.setGPIOValue(led_gpio_base+i, GPIOEnum.HIGH) == 0) {
                    } else {
                        Log.v("GPIO", String.format("setGPIOValue(%d) failed", led_gpio_base+i));
                    }
                }
                step ++;
            } else if (step == STEP_INIT_VIEW) {
                Message message = new Message();
                message.what = 1;
                handler.sendMessage(message);
            }

        }
    };


}
