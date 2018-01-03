package com.calypso.bluelib.utils;

import android.util.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * 项目名称：HealthDetectLecon
 * 类描述：
 * 创建人：jianz
 * 创建时间：2017/2/22 10:05
 * 修改备注：
 */
public class TypeConversion {
    public static char[] baseStart = {(byte) 0x01};
    public static char[] baseEnd = {(byte) 0x04};
    private static char[] startDetect = {(byte) 0x31, (byte) 0x32, (byte) 0x30, (byte) 0x30};
    private static char[] deviceInfo = {(byte) 0x31, (byte) 0x31, (byte) 0x30, (byte) 0x30};
    /**
     * 指令组装
     *
     * @param bytes
     * @return
     */
    public static char[] buildControllerProtocol(char[]... bytes) {
        List<Character> byteArray = new LinkedList<Character>();
        for (char[] bytes1 : bytes) {
            for (char b : bytes1) {
                byteArray.add(b);
            }
        }
        char[] ret = new char[byteArray.size()];
        for (int i = 0; i < byteArray.size(); i++) {
            ret[i] = byteArray.get(i);
        }
        return ret;
    }


    /**
     * start detect\ 命令字 0x12
     * <p>
     * request  0x01 0x31 0x32 0x30 0x30 0x4
     * 控制字类型  00：读所有单次模式  0x01-0x16：读单通道数据  0x8f 都所有通道自适应模式
     *
     * @return
     */
    public static char[] startDetect() {
        char[] ret = buildControllerProtocol(baseStart, startDetect, baseEnd);
        return ret;
    }
    /**
     * get device versioncode
     * request  0x01 0x31 0x31 0x30 0x30 0x4
     * reponse  01 39 31 30 3C 35 36 33 31 32 3E 33 30 33 30 33 31 3C 32 33 3D 3B 3F 34 30 31 36 35 31 04
     *          01 39 31 30 3C 35 36 33 31 32 3E 33 30 33 30 33 33 31 37 30 34 3B 3B 3B 3B 3B 3B 3B 3B 04
     * @return
     */

    public static char[] getDeviceVersion() {
        char[] ret = buildControllerProtocol(baseStart, deviceInfo, baseEnd);
        return ret;
    }
    /**
     * 是否包含某字段
     *
     * @param arr
     * @param targetValue
     * @return
     */
    public static boolean contain(String[] arr, String targetValue) {
        for (String s : arr) {
            if (s.equals(targetValue))
                return true;
        }
        return false;
    }

    /**
     * byte转16进制字符串
     *
     * @param b
     * @return
     */
    public static String byte2hex(byte[] b) {
        StringBuffer sb = new StringBuffer();
        String tmp = "";
        for (int i = 0; i < b.length; i++) {
            tmp = Integer.toHexString(b[i] & 0XFF);
            if (tmp.length() == 1) {
                sb.append("0x" + "0" + tmp + " ");
            } else {
                sb.append("0x" + tmp + " ");
            }
        }
        return sb.toString();
    }

    public static String bytesToHexStrings(byte[] src) {
        if (src == null || src.length <= 0) {
            return null;
        }
        String[] str = new String[src.length];
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                str[i] = "0";
            }
            str[i] = hv;
        }
        StringBuffer ret = new StringBuffer();
        for (String s : str) {
            if (!s.equals("0")) {
                if (s.length() < 2) {
                    ret.append("0").append(s).append(" ");
                } else {
                    ret.append(s).append(" ");
                }
            }
        }
        return ret.toString().toUpperCase();
    }

    /**
     * @param bytes
     * @return
     */
    public static String bytes2hex(byte[] bytes) {
        final String HEX = "0123456789abcdef";
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // 取出这个字节的高4位，然后与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt((b >> 4) & 0x0f));
            // 取出这个字节的低位，与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt(b & 0x0f) + " ");
        }
        return sb.toString();
    }

    /**
     * 字节数组转16进制字符串
     *
     * @param src 字节数组
     * @return 16进制字符串 01 30 31 32
     */
    public static String bytes2HexString(byte[] src) {
        if (src == null || src.length <= 0) {
            return null;
        }
        StringBuffer result = new StringBuffer();
        String hex;
        for (int i = 0; i < src.length; i++) {
            hex = Integer.toHexString(src[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            result.append(hex.toUpperCase() + " ");

        }
        return result.toString();
    }

    public static String modify(String str2) {
        String[] response = str2.split(" ");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < response.length; i++) {
            char[] ch = response[i].toCharArray();
            if (i % 2 == 0) {
                sb.append(" " + ch[1]);
            } else {
                sb.append(ch[1]);
            }
        }
        return sb.toString();
    }

    /**
     * 字符串转16进制字符串
     *
     * @param strPart 字符串
     * @return 16进制字符串
     */
    public static String string2HexString(String strPart) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < strPart.length(); i++) {
            int ch = (int) strPart.charAt(i);
            String strHex = Integer.toHexString(ch);
            hexString.append(strHex);
        }
        return hexString.toString();
    }

    /**
     * 16进制字符串转字符串
     *
     * @param src 16进制字符串
     * @return 字节数组
     */
    public static String hexString2String(String src) {
        String temp = "";
        for (int i = 0; i < src.length() / 2; i++) {
            temp = temp
                    + (char) Integer.valueOf(src.substring(i * 2, i * 2 + 2),
                    16).byteValue();
        }
        return temp;
    }

    /**
     * 字符转成字节数据char-->integer-->byte
     *
     * @param src
     * @return Byte
     */
    public static Byte char2Byte(Character src) {
        return Integer.valueOf((int) src).byteValue();
    }

    /**
     * 10进制数字转成16进制
     *
     * @param a   转化数据
     * @param len 占用字节数
     * @return
     */
    private static String intToHexString(int a, int len) {
        len <<= 1;
        String hexString = Integer.toHexString(a);
        int b = len - hexString.length();
        if (b > 0) {
            for (int i = 0; i < b; i++) {
                hexString = "0" + hexString;
            }
        }
        return hexString;
    }

    /**
     * 十六进制字符串 转 拼接字符串 去 01 04
     *
     * @param hex
     * @return
     */
    public static String HexStringSplit(String hex) {
        StringBuffer sb = new StringBuffer();
        if (hex == null && hex.length() <= 6) {
            return null;
        }
        hex = hex.substring(3, hex.length() - 3);
        String[] response = hex.split(" ");
        for (int i = 0; i < response.length; i++) {
            char[] ch = response[i].toCharArray();
            if (i % 2 == 0) {
                sb.append(" " + ch[1]);
            } else {
                sb.append(ch[1]);
            }
        }
        return sb.toString();
    }

    /**
     * return byte
     *
     * @param hex
     * @return
     */
    public static byte[] Split(String hex) {
        StringBuffer sb = new StringBuffer();
        if (hex == null && hex.length() <= 0) {
            return null;
        }
        String[] response = hex.split(" 04 ");
        String resp = TypeConversion.HexStringSplit(response[0], 6);
        String resp2 = TypeConversion.HexStringSplit(response[1], 4);
        String resp3 = TypeConversion.HexStringSplit(response[2], 4);
        String resp4 = TypeConversion.HexStringSplit(response[3], 4);
        String resp5 = TypeConversion.HexStringSplit(response[4], 4);
        sb.append(resp).append(resp2)
                .append(resp3).append(resp4)
                .append(resp5);
        Log.i("fuck", sb.toString());
        Log.i("fuck", sb.toString().replace(" ", ""));
        byte[] bytes = hexStringToByte(sb.toString());
        return bytes;
    }

    public static byte[] hexStringToByte(String hex) {
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        return result;
    }

    private static int toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }

    /**
     * @param hex
     * @param index 6 or 4
     * @return
     */
    public static String HexStringSplit(String hex, int index) {
        StringBuffer sb = new StringBuffer();
        if (hex == null && hex.length() <= 3 * index) {
            return null;
        }
        hex = hex.substring(3 + 3 * index, hex.length());
        String[] response = hex.split(" ");
        for (int i = 0; i < response.length; i++) {
            char[] ch = response[i].toCharArray();
            if (i % 2 == 0) {
                sb.append(" " + ch[1]);
            } else {
                sb.append(ch[1]);
            }
        }
        return sb.toString();
    }

    /**
     * return version info
     *
     * @param hex
     * @return
     */
    public static String[] HexStringConversionVesion(String hex) {
        String[] version = new String[2];
        if (hex == null && hex.length() < 42) {
            return null;
        }
        String six1 = hex.substring(6, 24);
        String six2 = hex.substring(25, 42);
        String[] response = six1.split(" ");
        String[] response2 = six2.split(" ");
        StringBuffer sb = new StringBuffer();
        for (String s : response) {
            String ch = hexString2String(s);
            sb.append(ch);
        }
        version[0] = sb.toString();
        StringBuffer sb2 = new StringBuffer();
        for (String s : response2) {
            sb2.append(s);
        }
        version[1] = sb2.toString();
        return version;
    }

    /**
     * return last char
     *
     * @param hex
     * @return
     */
    public static String HexStringCheck(String hex) {
        String ch = "";
        if (hex == null && hex.length() <= 0) {
            return null;
        }
        String str = HexStringSplit(hex);
        String[] response = str.split(" ");
        if (response.length >= 5) {
            ch = response[4];
        }
        return ch;
    }
}
