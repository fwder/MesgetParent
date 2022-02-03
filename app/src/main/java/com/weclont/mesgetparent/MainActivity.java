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
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Timer timer = new Timer();
    private TimerTask task;
    private OutputStream outputStream;
    public String longi = "";
    public String lati = "";
    public Socket socket = MainApplication.socket;
    private boolean isShowed = false;
    ProgressDialog p;//显示正在加载的对话框

    //创建一个Handler的子类对象,目的是重写Handler的处理消息的方法(handleMessage())
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1: // 接受Child端的返回信息
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("提示")//设置标题
                            .setMessage((CharSequence) msg.obj)//提示消息
                            .setIcon(R.drawable.sysu)//设置图标
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                //点击确定按钮执行的事件
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).create()//创建对话框
                            .show();//显示对话框
                    break;
                case 2: // getLocation返回数据处理
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("提示")//设置标题
                            .setMessage((CharSequence) msg.obj)//提示消息
                            .setIcon(R.drawable.sysu)//设置图标
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                //点击确定按钮执行的事件
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .setNegativeButton("查看地图", new DialogInterface.OnClickListener() {
                                //点击确定按钮执行的事件
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(MainActivity.this, MapActivity.class);
                                    intent.putExtra("Longitude", longi);
                                    intent.putExtra("Latitude", lati);
                                    startActivity(intent);
                                }
                            })
                            .create()//创建对话框
                            .show();//显示对话框
                    break;
                case 3: // 连接错误并重新连接
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("提示")//设置标题
                            .setMessage((CharSequence) msg.obj)//提示消息
                            .setCancelable(false)
                            .setIcon(R.drawable.sysu)//设置图标
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                //点击确定按钮执行的事件
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    logerror("服务器连接错误,即将重新连接");
                                    //回到LoginActivity
                                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                    finish();//关闭页面
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
        setContentView(R.layout.activity_main);

        // ListView初始化
        initlv();

        // 心跳数据检查连接是否正常
        sendBeatData();

    }


    //设置页面
    private void initlv() {

        List<Map<String, Object>> datalist = new ArrayList<Map<String, Object>>();
        ListView lv = (ListView) findViewById(R.id.lv_list);

        //设置listview分割线为空
        lv.setDivider(null);

        //添加Item项目
        Map<String, Object> map1 = new HashMap<String, Object>();
        map1.put("image", R.drawable.hello_world);
        map1.put("text", "Hello World!");
        Map<String, Object> map2 = new HashMap<String, Object>();
        map2.put("image", R.drawable.device_common_msg);
        map2.put("text", "设备基本信息");
        Map<String, Object> map3 = new HashMap<String, Object>();
        map3.put("image", R.drawable.screen_state);
        map3.put("text", "屏幕点亮状态");
        Map<String, Object> map4 = new HashMap<String, Object>();
        map4.put("image", R.drawable.location_on);
        map4.put("text", "获取定位数据");
        Map<String, Object> map5 = new HashMap<String, Object>();
        map5.put("image", R.drawable.network_state);
        map5.put("text", "获取网络状态");
        Map<String, Object> map6 = new HashMap<String, Object>();
        map6.put("image", R.drawable.screen_locked);
        map6.put("text", "模拟亮锁屏");
        Map<String, Object> map7 = new HashMap<String, Object>();
        map7.put("image", R.drawable.app_list);
        map7.put("text", "读取应用列表");
        Map<String, Object> map8 = new HashMap<String, Object>();
        map8.put("image", R.drawable.message_pic);
        map8.put("text", "远程提示框");
        Map<String, Object> map9 = new HashMap<String, Object>();
        map9.put("image", R.drawable.file_browser);
        map9.put("text", "远程文件浏览器");
        Map<String, Object> map10 = new HashMap<String, Object>();
        map10.put("image", R.drawable.volume);
        map10.put("text", "音量调节器");

        datalist.add(map1);
        datalist.add(map2);
        datalist.add(map3);
        datalist.add(map4);
        datalist.add(map5);
        datalist.add(map6);
        datalist.add(map7);
        datalist.add(map8);
        datalist.add(map9);
        datalist.add(map10);

        lv.setAdapter(new SimpleAdapter(this, datalist, R.layout.tv_item, new String[]{"image", "text"}, new int[]{R.id.image, R.id.text}));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            //设置每个list的点击事件
            @Override
            public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
                switch (p3) {
                    case 0: // Hello World 测试信息
                        onCommonSendMsg("helloworld");
                        break;
                    case 1: // 设备基本信息
                        onCommonSendMsg("getDeviceCommonMsg");
                        break;
                    case 2: // 检查屏幕状态
                        onCommonSendMsg("getScreenState");
                        break;
                    case 3: // 获取定位数据
                        onCommonSendMsg("getLocation");
                        break;
                    case 4: // 获取网络状态
                        onCommonSendMsg("getNetWorkState");
                        break;
                    case 5: // 模拟亮锁屏
                        onCommonSendMsg("setScreenState");
                        break;
                    case 6: // 读取应用列表
                        onCommonSendMsg("getAppList");
                        break;
                    case 7: // 远程提示框
                        EditText et = new EditText(MainActivity.this);
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("请输入你要发送的信息")//设置标题
                                .setView(et)
                                .setIcon(R.drawable.sysu)//设置图标
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    //点击确定按钮执行的事件
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (et.getText().toString().equals("")) {
                                            new AlertDialog.Builder(MainActivity.this)
                                                    .setTitle("提示")//设置标题
                                                    .setMessage("你什么都还没有输入呢~")
                                                    .setIcon(R.drawable.sysu)//设置图标
                                                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                        //点击确定按钮执行的事件
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                        }
                                                    }).create()//创建对话框
                                                    .show();//显示对话框
                                            return;
                                        }
                                        onCommonSendMsg("<Type:setTooltip><Msg:" + et.getText().toString() + ">");
                                    }
                                }).create()//创建对话框
                                .show();//显示对话框
                        break;
                    case 8: // 远程文件浏览器
                        Intent intent = new Intent(MainActivity.this, FileBrowserActivity.class);
                        startActivity(intent);
                        break;
                    case 9: // 音量调节器
                        Intent intent2 = new Intent(MainActivity.this, VolumeControlStreamActivity.class);
                        startActivity(intent2);
                        break;
                }
            }
        });
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
                        logerror("连接断开，正在重连");
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
                                    onHandlerMsg(3, "Child客户端" + MainApplication.mcname + "与服务器断开了连接！");
                                    hideLoading();//隐藏加载框
                                    return;
                                }
                                // 获取到定位数据
                                if (stringMatch("Type", response1).equals("getLocation")) {
                                    String State = stringMatch("State", response1);
                                    String Longitude = stringMatch("Longitude", response1);
                                    String Latitude = stringMatch("Latitude", response1);
                                    String LocationTimeSharp = stringMatch("LocationTime", response1);
                                    String LocationTime = "null";
                                    try {
                                        LocationTime = getDateSharpToDate(LocationTimeSharp);
                                    } catch (Exception e) {
                                        LocationTime = "null";
                                        Log.e(TAG, "run: 读取LocationTime失败！");
                                    }
                                    String Nowtime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                                    String LocationType = stringMatch("LocationType", response1);
                                    String LocationAccuracy = stringMatch("LocationAccuracy", response1);
                                    longi = Longitude;
                                    lati = Latitude;
                                    logerror(response1);
                                    if (State.equals("true")) {
                                        //定位成功
                                        onHandlerMsg(2, "定位成功！\n经度：" + Longitude + "\n纬度：" + Latitude + "\n定位时间：" + LocationTime + "\n当前时间：" + Nowtime + "\n结果来源：" + LocationType + "\n精度：" + LocationAccuracy);
                                        hideLoading();//隐藏加载框
                                    } else {
                                        //定位失败
                                        onHandlerMsg(2, "定位失败！\n下面数据为最后一次成功定位获取到数据：\n经度：" + Longitude + "\n纬度：" + Latitude + "\n定位时间：" + LocationTime + "\n当前时间：" + Nowtime + "\n结果来源：" + LocationType + "\n精度：" + LocationAccuracy);
                                        hideLoading();//隐藏加载框
                                    }
                                    return;
                                }


                                //普通情况就直接输出就行
                                //获取到信息时自动把<br>转换成\n
                                response1 = getBRTon(response1);

                                logerror(response1);
                                onHandlerMsg(1, response1);
                                hideLoading();//隐藏加载框
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

    public void showToast(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*释放资源并重连*/
    private void releaseSocket() {

        if (outputStream != null) {
            try {
                outputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = null;
        }

        if (socket != null) {
            try {
                socket.close();
                MainApplication.socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
            MainApplication.socket = null;
        }

        //取消定时任务
        timer.cancel();

        //连接异常，重新连接
        onHandlerMsg(3, "连接异常，设备可能不在线，请重新连接");

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

    private String getBRTon(String s) {
        return s.replace("<br>", "\n");
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

    //时间戳转换为日期
    private String getDateSharpToDate(String times) {
        long time = Long.parseLong(times);
        Date date = new Date(time);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(date);
    }

}
