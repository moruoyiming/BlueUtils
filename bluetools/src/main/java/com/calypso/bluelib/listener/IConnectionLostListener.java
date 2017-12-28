package com.calypso.bluelib.listener;

/**
 * Created by zhikang on 2017/12/28.
 * 连接断开监听
 */

public interface IConnectionLostListener {
    void onConnectionLost(Exception e);
}
