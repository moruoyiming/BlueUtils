package com.calypso.bluelib.listener;

/**
 * Created by zhikang on 2017/12/28.
 * 消息发送监听
 */

public interface OnSendMessageListener extends IErrorListener, IConnectionLostListener {
    /**
     * Call when send a message succeed, and get a response from the remote device.
     *
     * @param status   the status describes ok or error.
     *                 1 respect the response is valid,
     *                 -1 respect the response is invalid
     * @param response the response from the remote device
     */
    void onSuccess(int status, String response);
}