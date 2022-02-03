package com.weclont.mesgetparent;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VolumeControlStreamActivity extends AppCompatActivity {

    private SeekBar skbar_call;
    private SeekBar skbar_system;
    private SeekBar skbar_ring;
    private SeekBar skbar_music;
    private SeekBar skbar_alarm;
    private String TAG = "VolumeControlStreamActivity";
    private Timer timer = new Timer();
    private TimerTask task;
    private OutputStream outputStream;
    public Socket socket = MainApplication.socket;
    private boolean isShowed = false;
    ProgressDialog p;//显示正在加载的对话框

    //创建一个Handler的子类对象,目的是重写Handler的处理消息的方法(handleMessage())
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0: // 获取到当前设备音量，执行init();
                    init();
                    break;
                case 1: // 成功修改音量，返回信息处理
                    Toast.makeText(VolumeControlStreamActivity.this,"成功",Toast.LENGTH_SHORT).show();
                    break;
                case 999: // 连接异常，跳回MainActivity
                    new AlertDialog.Builder(VolumeControlStreamActivity.this)
                            .setTitle("提示")//设置标题
                            .setMessage("连接异常，请检查连接")//提示消息
                            .setCancelable(false)
                            .setIcon(R.drawable.sysu)//设置图标
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                //点击确定按钮执行的事件
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(VolumeControlStreamActivity.this, MainActivity.class);
                                    startActivity(intent);
                                }
                            }).create()//创建对话框
                            .show();//显示对话框
                    break;
            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volume_control_stream);

        initskbar();

        // 修改标题栏
        setTitle("音量调节器");
        // 检查远程设备权限是否给予
        onCommonSendMsg("checkNowVolume");

    }

    private void init() {
        // 心跳数据检查连接是否正常
        sendBeatData();
    }

    /*定时发送数据*/
    public void sendBeatData() {
        if (timer == null) {
            timer = new Timer();
        }

        if (task == null) {
            task = new TimerTask() {
                @Override
                public void run() {
                    try {
                        logerror("发送心跳数据...");
                        outputStream = socket.getOutputStream();
                        outputStream.write(("BeatData\n").getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    } catch (Exception e) {

                        /*发送失败说明socket断开了或者出现了其他错误*/
                        logerror("连接断开");
                        /*重连*/
                        releaseSocket();
                        e.printStackTrace();


                    }
                }
            };
        }

        timer.schedule(task, 0, 2000);
    }

    public void onCommonSendMsg(String msg1) {
        //向Child发送信息
        if (!isShowed) {
            showLoading();//显示加载框
        }
        onSendMsg(msg1);
    }

    /*发送并接受数据*/
    public void onSendMsg(String msg1) {
        if (socket != null && socket.isConnected()) {
            /*发送指令*/
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (socket.isConnected() && socket != null) {
                        try {
                            //发送请求
                            socket.setSoTimeout(Integer.MAX_VALUE);
                            outputStream = socket.getOutputStream();
                            if (outputStream != null) {
                                outputStream.write((msg1 + "\n").getBytes(StandardCharsets.UTF_8));
                                outputStream.flush();
                            }

                            //发送即接受
                            BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream(),
                                    StandardCharsets.UTF_8));
                            String response1 = inputStream.readLine();
                            if (response1 != null) {
                                // 特殊情况特殊处理

                                // 当连接失败时
                                if (response1.equals("ChildConnectionIsFailed")) {
                                    logerror(response1);
                                    onHandlerMsg(999, "");
                                    hideLoading();//隐藏加载框
                                    return;
                                }

                                // 返回设备当前音量值大小
                                if (stringMatch("Type", response1).equals("checkNowVolume")) {
                                    logerror(response1);
                                    //修改5个SeekBar的最大值和当前值
                                    skbar_call.setMax(Integer.parseInt(stringMatch("MAX_STREAM_VOICE_CALL", response1)));
                                    skbar_call.setProgress(Integer.parseInt(stringMatch("CURRENT_STREAM_VOICE_CALL", response1)));
                                    skbar_system.setMax(Integer.parseInt(stringMatch("MAX_STREAM_SYSTEM", response1)));
                                    skbar_system.setProgress(Integer.parseInt(stringMatch("CURRENT_STREAM_SYSTEM", response1)));
                                    skbar_ring.setMax(Integer.parseInt(stringMatch("MAX_STREAM_RING", response1)));
                                    skbar_ring.setProgress(Integer.parseInt(stringMatch("CURRENT_STREAM_RING", response1)));
                                    skbar_music.setMax(Integer.parseInt(stringMatch("MAX_STREAM_MUSIC", response1)));
                                    skbar_music.setProgress(Integer.parseInt(stringMatch("CURRENT_STREAM_MUSIC", response1)));
                                    skbar_alarm.setMax(Integer.parseInt(stringMatch("MAX_STREAM_ALARM", response1)));
                                    skbar_alarm.setProgress(Integer.parseInt(stringMatch("CURRENT_STREAM_ALARM", response1)));
                                    onHandlerMsg(0, "");
                                    hideLoading();//隐藏加载框
                                    return;
                                }

                                // 音量更改返回
                                if (stringMatch("Type", response1).equals("setVolume")) {
                                    logerror(response1);
                                    onHandlerMsg(1, response1);//在Handler中修改UI页面
                                    hideLoading();//隐藏加载框
                                    return;
                                }

                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            logerror("服务器连接错误,即将重新连接");
                            releaseSocket();
                        }
                    }
                }
            }).start();
        } else {
            logerror("服务器连接错误,即将重新连接");
            releaseSocket();
        }
    }

    // 当连接异常，直接跳回MainActivity
    private void releaseSocket() {

        //取消定时任务
        timer.cancel();

        //连接异常，重新连接
        onHandlerMsg(999, "");

    }

    /**
     * 显示加载的进度框
     */
    public void showLoading() {
        isShowed = true;
        p = ProgressDialog.show(this, "", "发送中...");
    }


    /**
     * 隐藏加载的进度框
     */
    public void hideLoading() {
        if (isShowed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    p.hide();
                    p.dismiss();
                    isShowed = false;
                }
            });

        }
    }

    public void onHandlerMsg(int i, String s) {
        Message msg = new Message();
        msg.what = i;
        msg.obj = s;
        handler.sendMessage(msg);
    }

    private void logerror(String s) {
        Log.e(TAG, "logerror: " + s);
    }

    //切割字符串
    public static String stringMatch(String type, String s) {
        try {
            List<String> results = new ArrayList<String>();
            Pattern p = Pattern.compile("<" + type + ":([\\w/\\.]*)>");
            Matcher m = p.matcher(s);
            while (!m.hitEnd() && m.find()) {
                results.add(m.group(1));
            }
            return results.get(0);
        } catch (Exception e) {
            return "";
        }
    }

    private void initskbar() {

        //初始化5个SeekBar
        skbar_call = (SeekBar) findViewById(R.id.skbar_call);
        skbar_call.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                                  @Override
                                                  public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                                                  }

                                                  @Override
                                                  public void onStartTrackingTouch(SeekBar seekBar) {
                                                  }

                                                  @Override
                                                  public void onStopTrackingTouch(SeekBar seekBar) {
                                                      onCommonSendMsg("<Type:setCallVolume><Level:" + seekBar.getProgress() + ">");
                                                  }
                                              }

        );
        skbar_system = (SeekBar) findViewById(R.id.skbar_system);
        skbar_system.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                                  @Override
                                                  public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                                                  }

                                                  @Override
                                                  public void onStartTrackingTouch(SeekBar seekBar) {
                                                  }

                                                  @Override
                                                  public void onStopTrackingTouch(SeekBar seekBar) {
                                                      onCommonSendMsg("<Type:setSystemVolume><Level:" + seekBar.getProgress() + ">");
                                                  }
                                              }

        );
        skbar_ring = (SeekBar) findViewById(R.id.skbar_ring);
        skbar_ring.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                                    @Override
                                                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                                                    }

                                                    @Override
                                                    public void onStartTrackingTouch(SeekBar seekBar) {
                                                    }

                                                    @Override
                                                    public void onStopTrackingTouch(SeekBar seekBar) {
                                                        onCommonSendMsg("<Type:setRingVolume><Level:" + seekBar.getProgress() + ">");
                                                    }
                                                }

        );
        skbar_music = (SeekBar) findViewById(R.id.skbar_music);
        skbar_music.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                                  @Override
                                                  public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                                                  }

                                                  @Override
                                                  public void onStartTrackingTouch(SeekBar seekBar) {
                                                  }

                                                  @Override
                                                  public void onStopTrackingTouch(SeekBar seekBar) {
                                                      onCommonSendMsg("<Type:setMusicVolume><Level:" + seekBar.getProgress() + ">");
                                                  }
                                              }

        );
        skbar_alarm = (SeekBar) findViewById(R.id.skbar_alarm);
        skbar_alarm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                                   @Override
                                                   public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                                                   }

                                                   @Override
                                                   public void onStartTrackingTouch(SeekBar seekBar) {
                                                   }

                                                   @Override
                                                   public void onStopTrackingTouch(SeekBar seekBar) {
                                                       onCommonSendMsg("<Type:setAlarmVolume><Level:" + seekBar.getProgress() + ">");
                                                   }
                                               }

        );
    }

}
