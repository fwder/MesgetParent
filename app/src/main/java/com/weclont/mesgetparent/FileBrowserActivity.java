package com.weclont.mesgetparent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileBrowserActivity extends AppCompatActivity {

    private static final String TAG = "FileBrowserActivity";
    private Timer timer = new Timer();
    private TimerTask task;
    private OutputStream outputStream;
    public Socket socket = MainApplication.socket;
    private boolean isShowed = false;
    public String thisdir = "";
    private TextView tv_dir;
    ProgressDialog p;//显示正在加载的对话框

    //创建一个Handler的子类对象,目的是重写Handler的处理消息的方法(handleMessage())
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case -1: // 权限不足
                    new AlertDialog.Builder(FileBrowserActivity.this)
                            .setTitle("提示")//设置标题
                            .setMessage("权限不足，请检查权限")//提示消息
                            .setIcon(R.drawable.sysu)//设置图标
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                //点击确定按钮执行的事件
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(FileBrowserActivity.this, MainActivity.class);
                                    startActivity(intent);
                                }
                            }).create()//创建对话框
                            .show();//显示对话框
                    break;
                case 0: // 权限正常，执行init();
                    init();
                    break;
                case 1: // getFiles()，成功接收到返回信息，并在这里修改UI页面

                    /**
                     * String file_list 格式：
                     * <Type:getFiles>
                     * <Dir:/storage/emulated/0/>
                     * <Sum:4>
                     * <File1:..><isFile1:false>
                     * <File2:wenjianjia><isFile2:false>
                     * <File3:a.txt><isFile3:true>
                     * <File4:b.txt><isFile4:true>
                     */

                    //设置UI页面
                    List<Map<String, Object>> datalist = new ArrayList<Map<String, Object>>();
                    ListView lv = (ListView) findViewById(R.id.lv_list_file_browser);

                    //设置listview分割线为空
                    lv.setDivider(null);

                    //获取数据
                    String file_list = (String) msg.obj;
                    String file_dir = stringMatch("Dir", file_list);
                    int summ = Integer.parseInt(stringMatch("Sum", file_list));

                    for (int i = 1; i <= summ; i++) {
                        //读取数据
                        String text = stringMatch("File" + i, file_list);
                        String isDir = stringMatch("isFile" + i, file_list);

                        //添加Item项目
                        Map<String, Object> map = new HashMap<String, Object>();
                        if (isDir.equals("true")) {
                            map.put("image", R.drawable.file_browser);
                            map.put("context", "File");
                        } else {
                            map.put("image", R.drawable.folder);
                            map.put("context", "Folder");
                        }
                        map.put("text", text);
                        datalist.add(map);
                    }

                    lv.setAdapter(new SimpleAdapter(FileBrowserActivity.this, datalist, R.layout.tv_item_file_browser, new String[]{"image", "text", "context"}, new int[]{R.id.image, R.id.text, R.id.context}));
                    //设置点击每个list的事件
                    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
                            //获取点击的文件名称
                            TextView ttv = p2.findViewById(R.id.text);
                            TextView ttv_context = p2.findViewById(R.id.context);

                            // 根据context来判断是文件还是文件夹
                            if (ttv_context.getText().equals("File")) { // 根据比较显示的图像是否为File来确定是否为文件，如果是文件就阻止切换到下一级
                                new AlertDialog.Builder(FileBrowserActivity.this)
                                        .setTitle("提示")//设置标题
                                        .setMessage("您点击了一个文件！")//提示消息
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

                            String file_name = (String) ttv.getText();
                            if (file_name.equals("..")) {
                                String dir = file_dir;
                                String[] file_dir_split = dir.split("/");

                                //测试
                                Log.e(TAG, "onItemClick: " + Arrays.toString(file_dir_split));

                                //当前路径长度 - 最后一个文件名长度 - "/"的长度
                                int endIndex = dir.length() - file_dir_split[file_dir_split.length - 1].length() - 1;
                                dir = dir.substring(0, endIndex);

                                if (dir.equals("/storage/emulated/") || dir.equals("/storage/emulated")) {//当前路径为根目录的上层目录
                                    new AlertDialog.Builder(FileBrowserActivity.this)
                                            .setTitle("提示")//设置标题
                                            .setMessage("这里是根目录了哦~")//提示消息
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
                                getFiles(dir);
                                return;
                            }
                            String file_dir_now = file_dir + file_name + "/";
                            getFiles(file_dir_now);
                            return;
                        }

                    });
                    //设置长按每个list的事件
                    lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent, View view,
                                                       int position, long id) {



                            return true;
                        }

                    });

                    break;
                case 999: // 连接异常，跳回MainActivity
                    new AlertDialog.Builder(FileBrowserActivity.this)
                            .setTitle("提示")//设置标题
                            .setMessage("连接异常，请检查连接")//提示消息
                            .setIcon(R.drawable.sysu)//设置图标
                            .setCancelable(false)
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                //点击确定按钮执行的事件
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(FileBrowserActivity.this, MainActivity.class);
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
        setContentView(R.layout.activity_file_browser);

        // tv_dir初始化
        tv_dir = (TextView) findViewById(R.id.tv_dir);

        // 修改标题栏
        setTitle("远程文件浏览器");
        // 检查远程设备权限是否给予
        onCommonSendMsg("checkPermission_WRITE_EXTERNAL_STORAGE");

    }

    private void init() {
        // ListView初始化 - 访问/storage/emulated/0/
        getFiles("/storage/emulated/0/");

        // 心跳数据检查连接是否正常
        sendBeatData();
    }

    // 发送请求列出文件
    private void getFiles(String file_dir) {

        //修改当前路径
        thisdir = file_dir;

        //修改UI路径显示
        tv_dir.setText(file_dir);

        //发送查询信息，并弹出加载框
        onCommonSendMsg("<Type:getFiles><Dir:" + file_dir + ">");

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

                                // 权限检查指令返回
                                if (stringMatch("Type", response1).equals("checkPermission_WRITE_EXTERNAL_STORAGE")) {
                                    logerror(response1);
                                    if (stringMatch("State", response1).equals("true")) {
                                        onHandlerMsg(0, "");
                                    } else if (stringMatch("State", response1).equals("false")) {
                                        onHandlerMsg(-1, "");
                                    }
                                    hideLoading();//隐藏加载框
                                    return;
                                }

                                // 文件列表返回
                                if (stringMatch("Type", response1).equals("getFiles")) {
                                    logerror("接受到文件列表：" + response1);
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
        p = ProgressDialog.show(this, "", "读取中...");
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
            Pattern p = Pattern.compile("<" + type + ":(.*?)>");
            Matcher m = p.matcher(s);
            while (!m.hitEnd() && m.find()) {
                results.add(m.group(1));
            }
            Log.e(TAG, "stringMatch: 切割结果：" + results.get(0));
            return results.get(0);
        } catch (Exception e) {
            Log.e(TAG, "stringMatch: 字符串切割错误！");
            return "";
        }
    }

    @Override
    public void onBackPressed() {

        // 判断是否为根目录
        String dir = thisdir;
        String[] file_dir_split = dir.split("/");

        //当前路径长度 - 最后一个文件名长度 - "/"的长度
        int endIndex = dir.length() - file_dir_split[file_dir_split.length - 1].length() - 1;
        dir = dir.substring(0, endIndex);

        if (dir.equals("/storage/emulated/") || dir.equals("/storage/emulated")) {//当前路径为根目录的上层目录
            finish();
            return;
        }
        getFiles(dir);

    }

}