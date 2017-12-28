package com.calypso.buetools;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.calypso.bluelib.bean.MessageBean;
import com.calypso.bluelib.listener.OnConnectListener;
import com.calypso.bluelib.listener.OnReceiveMessageListener;
import com.calypso.bluelib.listener.OnSearchDeviceListener;
import com.calypso.bluelib.listener.OnSendMessageListener;
import com.calypso.bluelib.manage.BlueManager;
import com.calypso.bluelib.utils.TypeConversion;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private BlueManager btHelperClient;
    private int progress = 0;
    private TextView textView;
    private TextView contextView;
    private ProgressBar progressBar;
    private StringBuilder stringBuilder;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String message = msg.obj.toString();
            switch (msg.what) {
                case 0:
                    textView.setText(message);
                    break;
                case 1:
                    progress += 4;
                    progressBar.setProgress(progress);
                    break;
                case 2:
                    stringBuilder.append(message + " \n");
                    contextView.setText(message);
                    progress = 100;
                    progressBar.setProgress(progress);
                    break;
                case 3:
                    stringBuilder.delete(0, stringBuilder.length());
                    stringBuilder.append(message);
                    contextView.setText(stringBuilder.toString());
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.content);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        contextView = findViewById(R.id.what);
        btHelperClient = BlueManager.from(MainActivity.this);
        stringBuilder = new StringBuilder();
        btHelperClient.requestEnableBt();
        if (Build.VERSION.SDK_INT >= 23) {
            //判断是否有权限
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_CONTACTS)) {
                    Toast.makeText(MainActivity.this, "shouldShowRequestPermissionRationale", Toast.LENGTH_SHORT).show();
                }
            }
        }
        findViewById(R.id.btn_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                btHelperClient.searchDevices(new OnSearchDeviceListener() {
                    @Override
                    public void onStartDiscovery() {
                        Log.d(TAG, "onStartDiscovery()");
                        Message message = handler.obtainMessage();
                        message.what = 0;
                        message.obj = "搜索设备";
                        handler.sendMessage(message);

                    }

                    @Override
                    public void onNewDeviceFound(BluetoothDevice device) {
                        Log.d(TAG, "new device: " + device.getName() + " " + device.getAddress());
                    }

                    @Override
                    public void onSearchCompleted(List<BluetoothDevice> bondedList, List<BluetoothDevice> newList) {
                        Log.d(TAG, "SearchCompleted: bondedList" + bondedList.toString());
                        Log.d(TAG, "SearchCompleted: newList" + newList.toString());
                        Message message = handler.obtainMessage();
                        message.what = 0;
                        message.obj = "搜索完成";
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                    }
                });


            }
        });
        findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btHelperClient.connectDevice("00:21:13:02:9B:F1", new OnConnectListener() {
                    @Override
                    public void onConnectStart() {
                        Log.i("blue", "onConnectStart");
                        Message message = handler.obtainMessage();
                        message.what = 0;
                        message.obj = "连接设备";
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onConnectting() {
                        Log.i("blue", "onConnectting");
                        Message message = handler.obtainMessage();
                        message.what = 0;
                        message.obj = "正在连接";
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onConnectFailed() {
                        Log.i("blue", "onConnectFailed");
                        Message message = handler.obtainMessage();
                        message.what = 0;
                        message.obj = "连接失败";
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onConectSuccess() {
                        Log.i("blue", "onConectSuccess");
                        Message message = handler.obtainMessage();
                        message.what = 0;
                        message.obj = "连接成功 00:21:13:02:9B:F1";
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.i("blue", "onError");
                    }
                });

            }
        });


        findViewById(R.id.btn_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progress = 0;
                progressBar.setProgress(progress);
                MessageBean item = new MessageBean(TypeConversion.startDetect());
                btHelperClient.sendMessage(item, true, new OnSendMessageListener() {
                    @Override
                    public void onSuccess(int status, String response) {
                        Message message = handler.obtainMessage();
                        message.what = 0;
                        message.obj = "发送成功";
                        handler.sendMessage(message);
                        Log.i("blue", "send message is success ! ");
                    }

                    @Override
                    public void onConnectionLost(Exception e) {
                        Message message = handler.obtainMessage();
                        message.what = 0;
                        message.obj = "连接断开";
                        handler.sendMessage(message);
                        Log.i("blue", "send message is onConnectionLost ! ");
                    }

                    @Override
                    public void onError(Exception e) {
                        Message message = handler.obtainMessage();
                        message.what = 0;
                        message.obj = "发送失败";
                        handler.sendMessage(message);
                        Log.i("blue", "send message is onError ! ");
                    }
                }, new OnReceiveMessageListener() {


                    @Override
                    public void onProgressUpdate(String what, int progress) {
                        Message message = handler.obtainMessage();
                        message.what = 1;
                        message.obj = "发送失败";
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onDetectDataUpdate(String what) {
                        Message message = handler.obtainMessage();
                        message.what = 3;
                        message.obj = what;
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onDetectDataFinish() {
                        Message message = handler.obtainMessage();
                        message.what = 2;
                        message.obj = "体检完成";
                        handler.sendMessage(message);
                        Log.i("blue", "receive message is onDetectDataFinish");
                    }

                    @Override
                    public void onNewLine(String s) {
                        Message message = handler.obtainMessage();
                        message.what = 3;
                        message.obj = s;
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onConnectionLost(Exception e) {
                        Message message = handler.obtainMessage();
                        message.what = 0;
                        message.obj = "连接断开";
                        handler.sendMessage(message);
                        Log.i("blue", "receive message is onConnectionLost ! ");
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.i("blue", "receive message is onError ! ");
                    }
                });
            }
        });
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // TODO Auto-generated method stub
        if (requestCode == 2) {
            if (permissions[0].equals(Manifest.permission.ACCESS_COARSE_LOCATION)
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户同意使用该权限
            } else {
                // 用户不同意，向用户展示该权限作用
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    //showTipDialog("用来扫描附件蓝牙设备的权限，请手动开启！");
                    return;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        btHelperClient.close();
    }
}