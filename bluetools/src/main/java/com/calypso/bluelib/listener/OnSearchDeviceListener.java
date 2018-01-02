package com.calypso.bluelib.listener;

import android.bluetooth.BluetoothDevice;

import com.calypso.bluelib.bean.SearchResult;

import java.util.List;

/**
 * Created by zhikang on 2017/12/28.
 * 搜索蓝牙设备监听
 */

public interface OnSearchDeviceListener extends IErrorListener {
    /**
     * Call before discovery devices.
     */
    void onStartDiscovery();

    /**
     * Call when found a new device.
     *
     * @param device the new device
     */
    void onNewDeviceFound(BluetoothDevice device);

    /**
     * Call when the discovery process completed.
     *
     * @param bondedList the remote devices those are bonded(paired).
     * @param newList    the remote devices those are not bonded(paired).
     */
    void onSearchCompleted(List<SearchResult> bondedList, List<SearchResult> newList);
}
