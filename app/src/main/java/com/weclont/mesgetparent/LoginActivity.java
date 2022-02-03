package com.weclont.mesgetparent;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * 登录界面
 */

public class LoginActivity extends Activity
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "LoginActivity";
    //布局内的控件
    private EditText et_name;
    private EditText et_password;
    private EditText et_mcname;
    private Button mLoginBtn;
    private CheckBox checkBox_password;
    private CheckBox checkBox_login;
    private boolean isShowed = false;
    ProgressDialog p;//显示正在加载的对话框
    public Socket socket;
    private OutputStream outputStream;

    //消息处理者,创建一个Handler的子类对象,目的是重写Handler的处理消息的方法(handleMessage())
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1://成功连接

                    showToast((String) msg.obj);
                    loadCheckBoxState();//记录下当前用户记住密码和自动登录的状态;

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();//关闭页面

                    break;
                case 2://连接失败

                    new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("提示")//设置标题
                            .setMessage((CharSequence) msg.obj)//提示消息
                            .setIcon(R.mipmap.ic_launcher)//设置图标
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                //点击确定按钮执行的事件
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).create()//创建对话框
                            .show();//显示对话框

                    showToast("连接失败：" + msg.obj);

                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        restartable();//传给全局变量
        initViews();
        setupEvents();
        initData();
    }


    /**
     * 连接服务器
     */
    private void login() {

        //先做一些基本的判断，比如输入的用户命为空，密码为空，网络不可用多大情况，都不需要去链接服务器了，而是直接返回提示错误
        if (getAccount().isEmpty()) {
            showToast("你输入的服务器地址为空！");
            return;
        }

        if (getPassword().isEmpty()) {
            showToast("你输入的端口为空！");
            return;
        }

        if (getMCName().isEmpty()) {
            showToast("你输入的设备名称为空！");
            return;
        }

        setLoginBtnClickable(false);//点击登录后，设置登录按钮不可点击状态
        MainApplication.ip = getAccount();
        MainApplication.port = getPassword();
        MainApplication.mcname = getMCName();
        if (!isShowed) {
            showLoading();//显示加载框
        }


        new Thread(new Runnable() {
            @Override
            public void run() {
                if (socket == null) {
                    try {
                        String ip = MainApplication.ip;
                        String port = MainApplication.port;
                        logerror("开始连接服务器：" + ip + ":" + port);
                        socket = new Socket(ip, Integer.parseInt(port));
                        socket.setSoTimeout(Integer.MAX_VALUE);

                        //检查连接
                        if (socket != null && socket.isConnected()) {
                            //发送连接数据
                            logerror("开始发送数据...");
                            outputStream = socket.getOutputStream();
                            outputStream.write(("<MCName:MesgetParent><func:onCreateConnection><type:Parent><ChildName:" + MainApplication.mcname + ">\n").getBytes(StandardCharsets.UTF_8));
                            outputStream.flush();
                            //传入全局变量
                            MainApplication.socket = socket;

                        } else {
                            logerror("连接失败，尝试重连...");

                            releaseSocket();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "连接失败！");
                        if (e instanceof SocketTimeoutException) {
                            releaseSocketWithoutLogin();
                            logerror("连接超时");
                            onHandlerMsg(2, "连接超时");
                            setLoginBtnClickable(true);  //这里解放登录按钮，设置为可以点击
                            hideLoading();//隐藏加载框
                            return;

                        } else if (e instanceof NoRouteToHostException) {
                            releaseSocketWithoutLogin();
                            logerror("该地址不存在，请检查网络。");
                            onHandlerMsg(2, "该地址不存在，请检查网络。");
                            setLoginBtnClickable(true);  //这里解放登录按钮，设置为可以点击
                            hideLoading();//隐藏加载框
                            return;

                        } else if (e instanceof ConnectException) {
                            releaseSocketWithoutLogin();
                            logerror("连接异常或被拒绝");
                            onHandlerMsg(2, "连接异常或被拒绝");
                            setLoginBtnClickable(true);  //这里解放登录按钮，设置为可以点击
                            hideLoading();//隐藏加载框
                            return;

                        }
                        releaseSocketWithoutLogin();
                        logerror("服务器异常");
                        onHandlerMsg(2, "服务器异常");
                        setLoginBtnClickable(true);  //这里解放登录按钮，设置为可以点击
                        hideLoading();//隐藏加载框
                        return;

                    }

                    Log.e(TAG, "onConnectedServer: 已连接到服务器，尝试连接Child设备");
                    if (socket != null && socket.isConnected()) {
                        try {
                            socket.setSoTimeout(Integer.MAX_VALUE);
                            //发送即接受
                            BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream(),
                                    StandardCharsets.UTF_8));
                            String response1;
                            response1 = inputStream.readLine();
                            if (response1 != null) {
                                if (response1.equals("ChildConnectionIsFailed")) {
                                    releaseSocketWithoutLogin();
                                    logerror(response1);
                                    onHandlerMsg(2, "Child客户端" + MainApplication.mcname + "与服务器断开了连接！");
                                    setLoginBtnClickable(true);  //这里解放登录按钮，设置为可以点击
                                    hideLoading();//隐藏加载框
                                    return;
                                }
                                if (response1.equals("Success")) {
                                    logerror(response1);
                                    onHandlerMsg(1, "与设备连接成功");
                                    setLoginBtnClickable(true);  //这里解放登录按钮，设置为可以点击
                                    hideLoading();//隐藏加载框
                                    return;
                                }
                            }
                        } catch (IOException e) {
                            releaseSocketWithoutLogin();
                            e.printStackTrace();
                            logerror("获取返回信息错误，连接失败！");
                            onHandlerMsg(2, "获取返回信息错误，连接失败！");
                            setLoginBtnClickable(true);  //这里解放登录按钮，设置为可以点击
                            hideLoading();//隐藏加载框
                            return;
                        }
                    } else {
                        releaseSocketWithoutLogin();
                        onHandlerMsg(2, "设备可能不在线，连接失败！");
                        setLoginBtnClickable(true);  //这里解放登录按钮，设置为可以点击
                        hideLoading();//隐藏加载框
                    }
                    setLoginBtnClickable(true);  //这里解放登录按钮，设置为可以点击
                    hideLoading();//隐藏加载框


                } else {
                    Log.e(TAG, "onConnectedServer: 服务器Socket已连接");
                    onHandlerMsg(1, "服务器Socket已连接");
                    setLoginBtnClickable(true);  //这里解放登录按钮，设置为可以点击
                    hideLoading();//隐藏加载框
                }

            }
        }).start();

    }

    private void onHandlerMsg(int i, String s) {
        Message msg = new Message();
        msg.what = i;
        msg.obj = s;
        handler.sendMessage(msg);
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

            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        }
        login();
    }

    /*释放资源但不重连*/
    private void releaseSocketWithoutLogin() {

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

            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        }
    }

    private void logerror(String s) {
        Log.e(TAG, "logerror: " + s);
    }

    private void restartable() {
        MainApplication.setLoginActivityContext(this);
    }

    private void initData() {


        //判断用户第一次登陆
        if (firstLogin()) {
            checkBox_password.setChecked(false);//取消记住密码的复选框
            checkBox_login.setChecked(false);//取消自动登录的复选框
        }
        //判断是否记住密码
        if (remenberPassword()) {
            checkBox_password.setChecked(true);//勾选记住密码
            setTextNameAndPassword();//把密码和账号输入到输入框中
        } else {
            setTextName();//把用户账号放到输入账号的输入框中
        }

        //判断是否自动登录
        if (autoLogin()) {
            checkBox_login.setChecked(true);
            login();//去登录就可以

        }
    }

    /**
     * 把本地保存的数据设置数据到输入框中
     */
    public void setTextNameAndPassword() {
        et_name.setText(getLocalName());
        et_password.setText(getLocalPassword());
        et_mcname.setText(getLocalMCName());
    }

    /**
     * 设置数据到输入框中
     */
    public void setTextName() {
        et_name.setText(getLocalName());
    }


    /**
     * 获得保存在本地的用户名
     */
    public String getLocalName() {
        //获取SharedPreferences对象，使用自定义类的方法来获取对象
        SharedPreferencesUtils helper = new SharedPreferencesUtils(this, "setting");
        return helper.getString("name");
    }


    /**
     * 获得保存在本地的密码
     */
    public String getLocalPassword() {
        //获取SharedPreferences对象，使用自定义类的方法来获取对象
        SharedPreferencesUtils helper = new SharedPreferencesUtils(this, "setting");
        return helper.getString("password");
    }

    /**
     * 获得保存在本地的设备名称
     */
    public String getLocalMCName() {
        //获取SharedPreferences对象，使用自定义类的方法来获取对象
        SharedPreferencesUtils helper = new SharedPreferencesUtils(this, "setting");
        return helper.getString("mcname");
    }

    /**
     * 判断是否自动登录
     */
    private boolean autoLogin() {
        //获取SharedPreferences对象，使用自定义类的方法来获取对象
        SharedPreferencesUtils helper = new SharedPreferencesUtils(this, "setting");
        return helper.getBoolean("autoLogin", false);
    }

    /**
     * 判断是否记住密码
     */
    private boolean remenberPassword() {
        //获取SharedPreferences对象，使用自定义类的方法来获取对象
        SharedPreferencesUtils helper = new SharedPreferencesUtils(this, "setting");
        return helper.getBoolean("remenberPassword", false);
    }


    private void initViews() {
        mLoginBtn = (Button) findViewById(R.id.btn_login);
        et_name = (EditText) findViewById(R.id.ett1);
        et_password = (EditText) findViewById(R.id.ett2);
        checkBox_password = (CheckBox) findViewById(R.id.checkBox_password);
        checkBox_login = (CheckBox) findViewById(R.id.checkBox_login);
        et_mcname = (EditText) findViewById(R.id.ett3);
    }

    private void setupEvents() {
        mLoginBtn.setOnClickListener(this);
        checkBox_password.setOnCheckedChangeListener(this);
        checkBox_login.setOnCheckedChangeListener(this);
    }

    /**
     * 判断是否是第一次登陆
     */
    private boolean firstLogin() {
        //获取SharedPreferences对象，使用自定义类的方法来获取对象
        SharedPreferencesUtils helper = new SharedPreferencesUtils(this, "setting");
        boolean first = helper.getBoolean("first", true);
        if (first) {
            //创建一个ContentVa对象（自定义的）设置不是第一次登录，,并创建记住密码和自动登录是默认不选，创建账号和密码为空
            helper.putValues(new SharedPreferencesUtils.ContentValue("first", false),
                    new SharedPreferencesUtils.ContentValue("remenberPassword", false),
                    new SharedPreferencesUtils.ContentValue("autoLogin", false),
                    new SharedPreferencesUtils.ContentValue("name", ""),
                    new SharedPreferencesUtils.ContentValue("password", ""));
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        loadUserName();    //无论如何保存一下用户名
        login(); //登陆
    }

    /**
     * 保存用户账号
     */
    public void loadUserName() {
        if (!getAccount().equals("") || !getAccount().equals("请输入服务器地址")) {
            SharedPreferencesUtils helper = new SharedPreferencesUtils(this, "setting");
            helper.putValues(new SharedPreferencesUtils.ContentValue("name", getAccount()));
        }

    }

    /**
     * 获取账号
     */
    public String getAccount() {
        return et_name.getText().toString().trim();//去掉空格
    }

    /**
     * 获取MCName
     */
    public String getMCName() {
        return et_mcname.getText().toString().trim();//去掉空格
    }

    /**
     * 获取密码
     */
    public String getPassword() {
        return et_password.getText().toString().trim();//去掉空格
    }


    /**
     * 保存用户选择“记住密码”和“自动登陆”的状态
     */
    private void loadCheckBoxState() {
        loadCheckBoxState(checkBox_password, checkBox_login);
    }

    /**
     * 保存按钮的状态值
     */
    public void loadCheckBoxState(CheckBox checkBox_password, CheckBox checkBox_login) {

        //获取SharedPreferences对象，使用自定义类的方法来获取对象
        SharedPreferencesUtils helper = new SharedPreferencesUtils(this, "setting");

        //如果设置自动登录
        if (checkBox_login.isChecked()) {
            //创建记住密码和自动登录是都选择,保存密码数据
            helper.putValues(
                    new SharedPreferencesUtils.ContentValue("remenberPassword", true),
                    new SharedPreferencesUtils.ContentValue("autoLogin", true),
                    new SharedPreferencesUtils.ContentValue("password", getPassword()),
                    new SharedPreferencesUtils.ContentValue("mcname", getMCName()));

        } else if (!checkBox_password.isChecked()) { //如果没有保存密码，那么自动登录也是不选的
            //创建记住密码和自动登录是默认不选,密码为空
            helper.putValues(
                    new SharedPreferencesUtils.ContentValue("remenberPassword", false),
                    new SharedPreferencesUtils.ContentValue("autoLogin", false),
                    new SharedPreferencesUtils.ContentValue("password", ""));
        } else if (checkBox_password.isChecked()) {   //如果保存密码，没有自动登录
            //创建记住密码为选中和自动登录是默认不选,保存密码数据
            helper.putValues(
                    new SharedPreferencesUtils.ContentValue("remenberPassword", true),
                    new SharedPreferencesUtils.ContentValue("autoLogin", false),
                    new SharedPreferencesUtils.ContentValue("password", getPassword()),
                    new SharedPreferencesUtils.ContentValue("mcname", getMCName()));
        }
    }

    /**
     * 是否可以点击登录按钮
     *
     * @param clickable
     */
    public void setLoginBtnClickable(boolean clickable) {
        mLoginBtn.setClickable(clickable);
    }


    /**
     * 显示加载的进度款
     */
    public void showLoading() {
        isShowed = true;
        p = ProgressDialog.show(MainApplication.getLoginActivityContext(), "", "正在连接服务器...");

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


    /**
     * CheckBox点击时的回调方法 ,不管是勾选还是取消勾选都会得到回调
     *
     * @param buttonView 按钮对象
     * @param isChecked  按钮的状态
     */
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == checkBox_password) {  //记住密码选框发生改变时
            if (!isChecked) {   //如果取消“记住密码”，那么同样取消自动登陆
                checkBox_login.setChecked(false);
            }
        } else if (buttonView == checkBox_login) {   //自动登陆选框发生改变时
            if (isChecked) {   //如果选择“自动登录”，那么同样选中“记住密码”
                checkBox_password.setChecked(true);
            }
        }
    }


    /**
     * 监听回退键
     */
    @Override
    public void onBackPressed() {
        if (isShowed) {
            p.hide();
            isShowed = false;
        } else {
            finish();
        }

    }

    /**
     * 页面销毁前回调的方法
     */
    protected void onDestroy() {
        if (isShowed) {
            p.hide();
            p.dismiss();
            isShowed = false;
        }
        super.onDestroy();
    }


    public void showToast(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });

    }

}
