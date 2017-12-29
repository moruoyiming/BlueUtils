package com.calypso.bluelib.manage;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import com.calypso.bluelib.Constants;
import com.calypso.bluelib.bean.MessageBean;
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
    private List<BluetoothDevice> mBondedList = new ArrayList<>();
    private List<BluetoothDevice> mNewList = new ArrayList<>();
    private OnSearchDeviceListener mOnSearchDeviceListener;
    private volatile Receiver mReceiver = new Receiver();
    private volatile STATUS mCurrStatus = STATUS.FREE;
    private BluetoothAdapter mBluetoothAdapter;
    private static volatile BlueManager blueManager;
    private BluetoothSocket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    ReadRunnable_ readRunnable;
    private Context mContext;
    private static int DEFAULT_BUFFER_SIZE = 10;
    private volatile boolean mWritable = true;
    private volatile boolean mReadable = true;
    private boolean mNeed2unRegister;
    private boolean what = true;
    private int number = 0;

    private enum STATUS {
        DISCOVERING,
        CONNECTED,
        FREE
    }

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
     * discovery the devices.
     *
     * @param listener listener for the process
     */
    public void searchDevices(OnSearchDeviceListener listener) {

        checkNotNull(listener);
        if (mBondedList == null) mBondedList = new ArrayList<>();
        if (mNewList == null) mNewList = new ArrayList<>();

        mOnSearchDeviceListener = listener;

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

        if (mOnSearchDeviceListener != null)
            mOnSearchDeviceListener.onStartDiscovery();

    }

    /**
     * 搜索蓝牙广播
     */
    private class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mOnSearchDeviceListener != null)
                    mOnSearchDeviceListener.onNewDeviceFound(device);
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    mNewList.add(device);
                } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    mBondedList.add(device);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mOnSearchDeviceListener != null)
                    mOnSearchDeviceListener.onSearchCompleted(mBondedList, mNewList);
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
     * @param listener     lister for the sending process
     * @param needResponse if need to obtain a response getInstance the remote device
     */
    public void sendMessage(MessageBean item, boolean needResponse, OnSendMessageListener listener, OnReceiveMessageListener onReceiveMessageListener) {
        try {
            if (mCurrStatus == STATUS.CONNECTED) {
                if (mBluetoothAdapter == null) {
                    listener.onError(new RuntimeException(DEVICE_HAS_NOT_BLUETOOTH_MODULE));
                    return;
                }
                mMessageBeanQueue.add(item);
                WriteRunnable writeRunnable = new WriteRunnable(listener);
                mExecutorService.submit(writeRunnable);
                number = 0;
                what = true;
                if (needResponse) {
                    if (readRunnable == null) {
                        readRunnable = new ReadRunnable_(onReceiveMessageListener);
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

    public void sendMessage(MessageBean item) {
        mMessageBeanQueue.add(item);
        WriteRunnable writeRunnable = new WriteRunnable(null);
        mExecutorService.submit(writeRunnable);
        number = 0;
        what = true;
    }

    /**
     * 连接bluetooth
     *
     * @param mac
     * @param listener
     */
    public void connectDevice(String mac, OnConnectListener listener) {
        if (mCurrStatus != STATUS.CONNECTED) {
            if (mac == null || TextUtils.isEmpty(mac))
                throw new IllegalArgumentException("mac address is null or empty!");
            if (!BluetoothAdapter.checkBluetoothAddress(mac))
                throw new IllegalArgumentException("mac address is not correct! make sure it's upper case!");
            ConnectDeviceRunnable connectDeviceRunnable = new ConnectDeviceRunnable(mac, listener);
            checkNotNull(mExecutorService);
            mExecutorService.submit(connectDeviceRunnable);
        } else {
            Log.i("blue", "the blue is connected !");
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
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothAdapter = null;
        }
        if (mNeed2unRegister) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
            mNeed2unRegister = !mNeed2unRegister;
        }
        mWritable = false;
        mReadable = false;
        if (mSocket != null) {
            try {
                mSocket.close();
                mSocket = null;
            } catch (IOException e) {
                mSocket = null;
            }
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
    }

    /**
     * 连接bluetooth线程
     */
    private class ConnectDeviceRunnable implements Runnable {
        private String mac;
        private OnConnectListener listener;

        public ConnectDeviceRunnable(String mac, OnConnectListener listener) {
            this.mac = mac;
            this.listener = listener;
            this.listener.onConnectStart();
        }

        @Override
        public void run() {
            BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(mac);
            mBluetoothAdapter.cancelDiscovery();
            mCurrStatus = STATUS.FREE;
            try {
                Log.d(TAG, "prepare to connect: " + remoteDevice.getAddress() + " " + remoteDevice.getName());
                mSocket = remoteDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(Constants.STR_UUID));
                listener.onConnectting();
                mSocket.connect();
                mInputStream = mSocket.getInputStream();
                mOutputStream = mSocket.getOutputStream();
                mCurrStatus = STATUS.CONNECTED;
                if (listener != null) {
                    listener.onConectSuccess();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (listener != null)
                    listener.onConnectFailed();
                try {
                    mInputStream.close();
                    mOutputStream.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
                mCurrStatus = STATUS.FREE;
            }
        }
    }

    /**
     * 读取bluetooth流线程
     */
    private class ReadRunnable_ implements Runnable {

        private OnReceiveMessageListener mListener;

        public ReadRunnable_(OnReceiveMessageListener listener) {
            mListener = listener;
        }

        @Override
        public void run() {
            mReadable = true;
            InputStream stream = mInputStream;
            while (mCurrStatus != STATUS.CONNECTED && mReadable) ;
            checkNotNull(stream);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            StringBuilder builder = new StringBuilder();
            while (mReadable) {
                try {
                    int count = 0;
                    while (count == 0) {
                        count = stream.available();//输入流中的数据个数。
                    }
                    if (count == 10 && what) {
                        int num = stream.read(buffer);
                        String progress = TypeConversion.bytesToHexStrings(buffer);
                        Log.i("progress", progress);
                        if (mListener != null) {
                            mListener.onProgressUpdate(progress, 0);
                        }
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
                            if (mListener != null) {
                                mListener.onDetectDataFinish();
                                mListener.onNewLine(builder.toString().trim());
                                builder.delete(0, builder.length());
                            }
                        } else {
                            if (mListener != null) {
                                mListener.onDetectDataUpdate(detect);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (mListener != null) {
                        mListener.onConnectionLost(e);
                        mCurrStatus = STATUS.FREE;
                    }
                }
            }

        }
    }

    /**
     * 输入bluetooth流线程
     */
    private class WriteRunnable implements Runnable {

        private OnSendMessageListener listener;

        public WriteRunnable(OnSendMessageListener listener) {
            this.listener = listener;
        }

        @Override
        public void run() {
            mWritable = true;
            while (mCurrStatus != STATUS.CONNECTED && mWritable) ;
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(mOutputStream));
            while (mWritable) {
                MessageBean item = mMessageBeanQueue.poll();
                if (item.mTYPE == MessageBean.TYPE.STRING) {
                    try {
                        writer.write(item.text);
                        writer.newLine();
                        writer.flush();
                        Log.d(TAG, "send string message: " + item.text);
                        if (listener != null) {
                            listener.onSuccess(Constants.STATUS_OK, "send string message is success callback !");
                        }
                    } catch (IOException e) {
                        if (listener != null)
                            listener.onConnectionLost(e);
                        mCurrStatus = STATUS.FREE;
                        break;
                    }
                } else if (item.mTYPE == MessageBean.TYPE.CHAR) {
                    try {
                        writer.write(item.data);
                        writer.flush();
                        Log.d(TAG, "send char message: " + item.data);
                        if (listener != null) {
                            listener.onSuccess(Constants.STATUS_OK, "send char message is success callback !");
                        }
                    } catch (IOException e) {
                        if (listener != null)
                            listener.onConnectionLost(e);
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
