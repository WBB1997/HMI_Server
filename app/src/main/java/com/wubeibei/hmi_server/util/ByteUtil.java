package com.wubeibei.hmi_server.util;

public class ByteUtil {
    public static final int Motorola = 0;
    public static final int Intel = 1;

    //byte转16进制
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() < 2)
                sb.append(0);
            sb.append(hex);
        }
        return sb.toString();
    }

    //16进制转byte
    public static byte hexToByte(String inHex) {
        return (byte) Integer.parseInt(inHex, 16);
    }

    //16进制转byte
    public static byte[] hexToByteArray(String inHex) {
        int hexlen = inHex.length();
        byte[] result;
        if (hexlen % 2 == 1) {
            //奇数
            hexlen++;
            result = new byte[(hexlen / 2)];
            inHex = "0" + inHex;
        } else {
            //偶数
            result = new byte[(hexlen / 2)];
        }
        int j = 0;
        for (int i = 0; i < hexlen; i += 2) {
            result[j] = hexToByte(inHex.substring(i, i + 2));
            j++;
        }
        return result;
    }

    // 返回子数组
    public static byte[] subBytes(byte[] bytes, int start, int length) {
        byte[] sub = new byte[length];
        for (int i = 0; i < length; i++)
            sub[i] = bytes[i + start];
        return sub;
    }

    // 设置某一位
    public static void setBit(byte[] bytes, int Byte_offset, int bit_index, boolean changed) {
        int i = Byte_offset + bit_index / 8;
        int j = bit_index % 8;
        if (changed)
            bytes[i] |= (0b00000001 << j);
        else
            bytes[i] &= ~(0b00000001 << j);
    }

    // 查看一个Byte的某位是否为1
    public static boolean viewBinary(byte Byte, int position) {
        return (Byte & 0b00000001 << position) != 0;
    }

    // 计算值
    public static double countBits(byte[] bytes, int Byte_offset, int start_bit_index, int bitLength, int state) {
        double count = 0;
        if (state == Motorola) {
            int index = start_bit_index;
            for (int i = 0; i < bitLength; i++) {
                if (viewBinary(bytes[Byte_offset + index / 8], index % 8)) {
                    count += Math.pow(2, i);
                }
                index++;
                if (index % 8 == 0) {
                    index -= 2 * 8;
                }
            }
        } else if (state == Intel) {
            for (int i = start_bit_index; i < start_bit_index + bitLength; i++) {
                if (viewBinary(bytes[Byte_offset + i / 8], i % 8)) {
                    count += Math.pow(2, i - start_bit_index);
                }
            }
        }
        return count;
    }

    public static void setBits(byte[] TargetBytes, int SrcNum, int Byte_offset, int start_bit_index, int bitLength, int state) {
        byte[] SrcBytes = new byte[8];
        if (state == Intel) {
            for (int i = 0; i < SrcBytes.length * 8; i++) {
                if (SrcNum / 2 > 0) {
                    setBit(SrcBytes, i / 8, i % 8, SrcNum % 2 == 1);
                    SrcNum /= 2;
                } else {
                    setBit(SrcBytes, i / 8, i % 8, SrcNum % 2 == 1);
                    break;
                }
            }
            for (int i = start_bit_index; i < start_bit_index + bitLength; i++) {
                boolean flag = viewBinary(SrcBytes[(i - start_bit_index) / 8], (i - start_bit_index) % 8);
                setBit(TargetBytes, Byte_offset, i, flag);
            }
        } else if (state == Motorola) {
            boolean flag = true;
            for (int i = SrcBytes.length * 8 - 8; i >= 0; i -= 8) {
                for (int j = i; j - i < 8; j++) {
                    if (SrcNum / 2 > 0) {
                        setBit(SrcBytes, j / 8, j % 8, SrcNum % 2 == 1);
                        SrcNum /= 2;
                    } else {
                        setBit(SrcBytes, j / 8, j % 8, SrcNum % 2 == 1);
                        flag = false;
                        break;
                    }
                }
                if (!flag)
                    break;
            }
            System.out.println(ByteUtil.bytesToHex(SrcBytes));
            int index = start_bit_index;
            for (int i = 0; i < bitLength; i++) {
                flag = viewBinary(SrcBytes[(SrcBytes.length * 8 - i - 1) / 8], i % 8);
                setBit(TargetBytes, Byte_offset, index, flag);
                index++;
                if (index % 8 == 0) {
                    index -= 2 * 8;
                }
            }
        }
    }
}
