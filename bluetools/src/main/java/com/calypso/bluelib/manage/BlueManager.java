package com.calypso.bluelib.manage;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.calypso.bluelib.Constants;
import com.calypso.bluelib.bean.MessageBean;
import com.calypso.bluelib.bean.SearchResult;
import com.calypso.bluelib.listener.OnConnectListener;
import com.calypso.bluelib.listener.OnReceiveMessageListener;
import com.calypso.bluelib.listener.OnSearchDeviceListener;
import com.calypso.bluelib.listener.OnSendMessageListener;
import com.calypso.bluelib.utils.TypeConversion;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhikang on 2017/12/28.
 */

public class BlueManager {
    private static final String DEVICE_HAS_NOT_BLUETOOTH_MODULE = "device has not bluetooth module!";
    private static final String TAG = BlueManager.class.getSimpleName();
    private Queue<MessageBean> mMessageBeanQueue = new LinkedBlockingQueue<>();
    private ExecutorService mExecutorService = Executors.newCachedThreadPool();
    private List<SearchResult> mBondedList = new ArrayList<>();
    private HashMap<String, Object> paar = new HashMap<>();
    private List<SearchResult> mNewList = new ArrayList<>();
    private OnSearchDeviceListener mOnSearchDeviceListener;
    private OnConnectListener onConnectListener;
    private OnSendMessageListener onSendMessageListener;
    private OnReceiveMessageListener onReceiveMessageListener;
    private volatile Receiver mReceiver = new Receiver();
    private volatile STATUS mCurrStatus = STATUS.FREE;
    private BluetoothAdapter mBluetoothAdapter;
    private static volatile BlueManager blueManager;
    private BluetoothSocket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadRunnable readRunnable;
    private Context mContext;
    private static int DEFAULT_BUFFER_SIZE = 10;
    private volatile boolean mWritable = true;
    private volatile boolean mReadable = true;
    private boolean mNeed2unRegister;
    private boolean what = true;
    private int number = 0;
    private boolean readVersion = true;
    private boolean supportBLE = false;

    private enum STATUS {
        DISCOVERING,
        CONNECTED,
        FREE
    }

    private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(!paar.containsKey(device.getAddress())){
                Log.i("ble", "device " + device.getAddress() + "   " + device.getName());
                paar.put(device.getAddress(), "mac:" + device.getAddress());
                SearchResult searchResult = new SearchResult(device, rssi, null);
                mNewList.add(searchResult);
            }else{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    if (mOnSearchDeviceListener != null)
                        mOnSearchDeviceListener.onSearchCompleted(mBondedList, mNewList);
                }
            }
        }

    };

    /**
     * Obtains the BtHelperClient getInstance the given context.
     *
     * @param context context
     * @return an instance of BtHelperClient
     */
    public static BlueManager getInstance(Context context) {
        if (blueManager == null) {
            synchronized (BlueManager.class) {
                if (blueManager == null)
                    blueManager = new BlueManager(context);
            }
        }
        return blueManager;
    }

    /**
     * private constructor for singleton
     *
     * @param context context
     */
    private BlueManager(Context context) {
        mContext = context.getApplicationContext();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setOnSearchDeviceListener(OnSearchDeviceListener mOnSearchDeviceListener) {
        this.mOnSearchDeviceListener = mOnSearchDeviceListener;
    }

    public void setOnConnectListener(OnConnectListener onConnectListener) {
        this.onConnectListener = onConnectListener;
    }

    public void setOnSendMessageListener(OnSendMessageListener onSendMessageListener) {
        this.onSendMessageListener = onSendMessageListener;
    }

    public void setOnReceiveMessageListener(OnReceiveMessageListener onReceiveMessageListener) {
        this.onReceiveMessageListener = onReceiveMessageListener;
    }

    public void setReadVersion(boolean readVersion) {
        this.readVersion = readVersion;
    }

    /**
     * Request for enable the device's bluetooth asynchronously.
     * Throw a NullPointerException if the device doesn't have a bluetooth module.
     */
    public void requestEnableBt() {
        if (mBluetoothAdapter == null) {
            throw new NullPointerException(DEVICE_HAS_NOT_BLUETOOTH_MODULE);
        }
        if (!mBluetoothAdapter.isEnabled())
            mBluetoothAdapter.enable();
    }

    /**
     * discovery the ble devices.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressWarnings("deprecation")
    public void searchBLEDevices() {
        try {
            checkNotNull(mOnSearchDeviceListener);
            if (mBondedList == null) mBondedList = new ArrayList<>();
            if (mNewList == null) mNewList = new ArrayList<>();
            if (mBluetoothAdapter == null) {
                mOnSearchDeviceListener.onError(new NullPointerException(DEVICE_HAS_NOT_BLUETOOTH_MODULE));
                return;
            }
            if (mBluetoothAdapter.isDiscovering())
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * discovery the devices.
     */
    public void searchDevices() {
        try {
            if (mCurrStatus == STATUS.FREE) {
                mCurrStatus = STATUS.DISCOVERING;
                checkNotNull(mOnSearchDeviceListener);
                if (mBondedList == null) mBondedList = new ArrayList<>();
                if (mNewList == null) mNewList = new ArrayList<>();
                if (mBluetoothAdapter == null) {
                    mOnSearchDeviceListener.onError(new NullPointerException(DEVICE_HAS_NOT_BLUETOOTH_MODULE));
                    return;
                }
                if (mReceiver == null) mReceiver = new Receiver();
                // ACTION_FOUND
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                mContext.registerReceiver(mReceiver, filter);
                // ACTION_DISCOVERY_FINISHED
                filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                mContext.registerReceiver(mReceiver, filter);
                mNeed2unRegister = true;
                mBondedList.clear();
                mNewList.clear();
                if (mBluetoothAdapter.isDiscovering())
                    mBluetoothAdapter.cancelDiscovery();
                mBluetoothAdapter.startDiscovery();
                mOnSearchDeviceListener.onStartDiscovery();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 搜索蓝牙广播
     */
    private class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    if (mOnSearchDeviceListener != null)
                        mOnSearchDeviceListener.onStartDiscovery();
                } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                        if (paar != null && !paar.containsKey(device.getAddress())) {
                            paar.put(device.getAddress(), "mac:" + device.getAddress());
                            if (mNewList != null) {
                                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                                SearchResult searchResult = new SearchResult(device, rssi, null);
                                mNewList.add(searchResult);
                            }
                        }
                    } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        if (mBondedList != null) {
                            int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                            SearchResult searchResult = new SearchResult(device, rssi, null);
                            mBondedList.add(searchResult);
                        }
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    if (mOnSearchDeviceListener != null)
                        mOnSearchDeviceListener.onSearchCompleted(mBondedList, mNewList);

                    searchBLEDevices();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Send a message to a remote device.
     * If the local device did't connected to the remote devices, it will call connectDevice(), then send the message.
     * You can obtain a response getInstance the remote device, just as http.
     * However, it will blocked if didn't get response getInstance the remote device.
     *
     * @param item         the message need to send
     * @param needResponse if need to obtain a response getInstance the remote device
     */
    public void sendMessage(MessageBean item, boolean needResponse) {
        try {
            if (mCurrStatus == STATUS.CONNECTED) {
                if (mBluetoothAdapter == null) {
                    onSendMessageListener.onError(new RuntimeException(DEVICE_HAS_NOT_BLUETOOTH_MODULE));
                    return;
                }
                mMessageBeanQueue.add(item);
                WriteRunnable writeRunnable = new WriteRunnable();
                mExecutorService.submit(writeRunnable);
                number = 0;
                what = true;
                if (needResponse) {
                    if (readRunnable == null) {
                        readRunnable = new ReadRunnable();
                        mExecutorService.submit(readRunnable);
                    } else {
                        Log.i("blue", "readRunnable is not null !");
                    }
                }
            } else {
                Log.i("blue", "the blue is not connected !");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 断开当前连接的蓝牙设备
     */
    public void closeDevice() {
        try {
            if (mCurrStatus == STATUS.CONNECTED) {
                mReadable = false;
                mWritable = false;
                if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mSocket != null && mSocket.isConnected()) {
                    mSocket.close();
                    mSocket = null;
                    number = 0;
                    what = true;
                    if (readRunnable != null) {
                        readRunnable = null;
                    }
                } else {
                    Log.i("blue", "closeDevice faield please check bluetooth is enable and the mSocket is connected !");
                }
            } else {
                Log.i("blue", "the bluetooth is not conencted ! please connect devices !");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 连接bluetooth
     *
     * @param mac
     */
    public void connectDevice(String mac) {
        try {
            if (mCurrStatus != STATUS.CONNECTED) {
                if (mac == null || TextUtils.isEmpty(mac))
                    throw new IllegalArgumentException("mac address is null or empty!");
                if (!BluetoothAdapter.checkBluetoothAddress(mac))
                    throw new IllegalArgumentException("mac address is not correct! make sure it's upper case!");
                if (mReadable = false) {
                    mReadable = true;
                }
                if (mWritable = false) {
                    mWritable = true;
                }
                if (onConnectListener != null) {
                    onConnectListener.onConnectStart();
                    ConnectDeviceRunnable connectDeviceRunnable = new ConnectDeviceRunnable(mac);
                    checkNotNull(mExecutorService);
                    mExecutorService.submit(connectDeviceRunnable);
                }
            } else {
                Log.i("blue", "the blue is connected !");
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * 暂停 读写线程
     *
     * @param pauseread
     * @param pausewriter
     */
    public void pauseBlue(boolean pauseread, boolean pausewriter) {
        this.mWritable = pausewriter;
        this.mReadable = pauseread;
    }

    /**
     * Closes the connection and releases any system resources associated
     * with the stream.
     */
    public void close() {
        try {
            if (mBluetoothAdapter != null) {
                mBluetoothAdapter.cancelDiscovery();
                mBluetoothAdapter = null;
            }
            if (mNeed2unRegister) {
                mContext.unregisterReceiver(mReceiver);
                mReceiver = null;
                mNeed2unRegister = !mNeed2unRegister;
            }
            if (mMessageBeanQueue != null) {
                mMessageBeanQueue.clear();
                mMessageBeanQueue = null;
            }
            mWritable = false;
            mReadable = false;

            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
            if (mOutputStream != null) {
                mOutputStream.close();
                mOutputStream = null;
            }
            if (mExecutorService != null) {
                mExecutorService.shutdown();
                mExecutorService = null;
            }
            mNewList = null;
            mBondedList = null;
            mReceiver = null;
            blueManager = null;
            mCurrStatus = STATUS.FREE;
        } catch (Exception e) {
            e.printStackTrace();
            mSocket = null;
        }
    }

    /**
     * 连接bluetooth线程
     */
    private class ConnectDeviceRunnable implements Runnable {
        private String mac;

        public ConnectDeviceRunnable(String mac) {
            this.mac = mac;
        }

        @Override
        public void run() {
            try {
                if (onConnectListener == null) {
                    Log.i("blue", "the connectListener is null !");
                    return;
                }
                BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(mac);
                mBluetoothAdapter.cancelDiscovery();
                mCurrStatus = STATUS.FREE;
                Log.d(TAG, "prepare to connect: " + remoteDevice.getAddress() + " " + remoteDevice.getName());
                mSocket = remoteDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(Constants.STR_UUID));
                onConnectListener.onConnectting();
                mSocket.connect();
                mInputStream = mSocket.getInputStream();
                mOutputStream = mSocket.getOutputStream();
                mCurrStatus = STATUS.CONNECTED;
                onConnectListener.onConectSuccess(mac);
            } catch (Exception e) {
                e.printStackTrace();
                onConnectListener.onConnectFailed();
                try {
                    mInputStream.close();
                    mOutputStream.close();
                } catch (Exception closeException) {
                    closeException.printStackTrace();
                }
                mCurrStatus = STATUS.FREE;
            }
        }
    }

    /**
     * 读取bluetooth流线程
     */
    private class ReadRunnable implements Runnable {


        @Override
        public void run() {
            try {
                if (onReceiveMessageListener == null) {
                    Log.i("blue", "the receiverMessageListener is null !");
                    return;
                }
                mReadable = true;
                InputStream stream = mInputStream;
                while (mCurrStatus != STATUS.CONNECTED && mReadable) ;
                checkNotNull(stream);
                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                StringBuilder builder = new StringBuilder();
                while (mReadable) {
                    int count = 0;
                    while (count == 0) {
                        count = stream.available();//输入流中的数据个数。
                    }
                    if (readVersion) {
                        if (count == 10) {
                            int num = stream.read(buffer);
                            String text2 = TypeConversion.bytesToHexStrings(buffer);
                            builder.append(text2);
                            Log.i("version", text2);
                            if (text2.endsWith("04 ")) {
                                String versionHex = TypeConversion.HexStringSplit(builder.toString());
                                String[] version = TypeConversion.HexStringConversionVesion(versionHex);
                                if (version.length >= 2) {
                                    String sn = version[1];
                                    Log.i("sn", sn);
                                    onReceiveMessageListener.onNewLine("当前设备SN：" + sn);
                                }
                            }
                        } else {
                            if (count >= 10) {
                                int num = stream.read(buffer);
                                String text2 = TypeConversion.bytesToHexStrings(buffer);
                                builder.append(text2);
                                Log.i("append", text2);
                            }
                        }
                    } else {
                        if (count == 10 && what) {
                            int num = stream.read(buffer);
                            String progress = TypeConversion.bytesToHexStrings(buffer);
                            Log.i("progress", progress);
                            onReceiveMessageListener.onProgressUpdate(progress, 0);
                        } else if (count >= 10) {
                            what = false;
                            int num = stream.read(buffer);
                            String detect = TypeConversion.bytesToHexStrings(buffer);
                            builder.append(detect);
                            Log.i("detect", detect);
                            if (detect.endsWith("04 ")) {
                                number++;
                            }
                            if (number == 5) {
                                onReceiveMessageListener.onDetectDataFinish();
                                onReceiveMessageListener.onNewLine(builder.toString().trim());
                                builder.delete(0, builder.length());
                            } else {
                                onReceiveMessageListener.onDetectDataUpdate(detect);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                onReceiveMessageListener.onConnectionLost(e);
                mCurrStatus = STATUS.FREE;
            }
        }
    }

    /**
     * 输入bluetooth流线程
     */
    private class WriteRunnable implements Runnable {

        @Override
        public void run() {
            if (onSendMessageListener == null) {
                Log.i("blue", "send message listener is null !");
                return;
            }
            mWritable = true;
            while (mCurrStatus != STATUS.CONNECTED && mWritable) ;
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(mOutputStream));
            while (mWritable) {
                MessageBean item = mMessageBeanQueue.poll();
                if (item.mTYPE != null && MessageBean.TYPE.STRING == item.mTYPE) {
                    try {
                        writer.write(item.text);
                        writer.newLine();
                        writer.flush();
                        Log.d(TAG, "send string message: " + item.text);
                        onSendMessageListener.onSuccess(Constants.STATUS_OK, "send string message is success callback !");
                    } catch (IOException e) {
                        onSendMessageListener.onConnectionLost(e);
                        mCurrStatus = STATUS.FREE;
                        break;
                    }
                } else if (item.mTYPE != null && MessageBean.TYPE.CHAR == item.mTYPE) {
                    try {
                        writer.write(item.data);
                        writer.flush();
                        Log.d(TAG, "send char message: " + item.data);
                        onSendMessageListener.onSuccess(Constants.STATUS_OK, "send char message is success callback !");
                    } catch (IOException e) {
                        onSendMessageListener.onConnectionLost(e);
                        mCurrStatus = STATUS.FREE;
                        break;
                    }
                }
            }

        }

    }

    /**
     * 校验
     *
     * @param o
     */
    private void checkNotNull(Object o) {
        if (o == null)
            throw new NullPointerException();
    }


}
