package com.calypso.bluelib.listener;

/**
 * Created by zhikang on 2017/12/28.
 * 蓝牙连接监听
 */

public interface OnConnectListener extends IErrorListener {

    void onConnectStart();

    void onConnectting();

    void onConnectFailed();

    void onConectSuccess(String mac);
}
