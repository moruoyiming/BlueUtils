package com.calypso.bluelib.bean;

/**
 * Created by zhikang on 2017/12/28.
 */

public class MessageBean {
    public enum TYPE {
        STRING,
        CHAR,
        BYTE
    }

    public String text;
    public char[] data;
    public byte[] bytes;
    public TYPE mTYPE;

    public MessageBean(String text) {
        this.text = text;
        mTYPE = TYPE.STRING;
    }

    public MessageBean(char[] data) {
        this.data = data;
        mTYPE = TYPE.CHAR;
    }

    public MessageBean(byte[] bytes) {
        this.bytes = bytes;
        mTYPE = TYPE.BYTE;
    }
}
