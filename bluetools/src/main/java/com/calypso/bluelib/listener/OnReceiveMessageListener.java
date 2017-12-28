package com.calypso.bluelib.listener;

/**
 * Created by zhikang on 2017/12/28.
 * 接收消息监听
 */

public interface OnReceiveMessageListener extends OnDetectResponseListener, IErrorListener, IConnectionLostListener {
    /**
     * call when have some response
     * @param s
     */
    void onNewLine(String s);
}
