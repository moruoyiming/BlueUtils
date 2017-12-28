package com.calypso.bluelib.listener;

/**
 * Created by zhikang on 2017/12/28.
 * 体检数据监听
 */

public interface OnDetectResponseListener {

    /**
     * call when blue have some reponse and need update progressbar .
     *
     * @param what
     * @param progress
     */
    void onProgressUpdate(String what, int progress);

    /**
     * call when blue have some detectreponse .
     *
     * @param response
     */
    void onDetectDataUpdate(String response);

    /**
     * call wen blue detectreponse is finish .
     */
    void onDetectDataFinish();

}
