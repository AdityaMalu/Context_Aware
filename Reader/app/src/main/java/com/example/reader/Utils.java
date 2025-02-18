package com.example.reader;

public class Utils {
    private static final String HEX_CHARS = "0123456789ABCDEF";
    private static final char[] HEX_CHARS_ARRAY = "0123456789ABCDEF".toCharArray();

    public static byte[] hexStringToByteArray(String data) {
        byte[] result = new byte[data.length() / 2];

        for (int i = 0; i < data.length(); i += 2) {
            int firstIndex = HEX_CHARS.indexOf(data.charAt(i));
            int secondIndex = HEX_CHARS.indexOf(data.charAt(i + 1));

            int octet = (firstIndex << 4) | secondIndex;
            result[i / 2] = (byte) octet;
        }

        return result;
    }

    public static String toHex(byte[] byteArray) {
        StringBuilder result = new StringBuilder();

        for (byte b : byteArray) {
            int octet = b & 0xFF;
            int firstIndex = (octet & 0xF0) >> 4;
            int secondIndex = octet & 0x0F;
            result.append(HEX_CHARS_ARRAY[firstIndex]);
            result.append(HEX_CHARS_ARRAY[secondIndex]);
        }

        return result.toString();
    }

    private int[] convertToUnsigned(byte[] byteArray) {
        int[] unsignedArray = new int[byteArray.length];
        for (int i = 0; i < byteArray.length; i++) {
            unsignedArray[i] = byteArray[i] & 0xFF; // Convert to unsigned
        }
        return unsignedArray;
    }
}
