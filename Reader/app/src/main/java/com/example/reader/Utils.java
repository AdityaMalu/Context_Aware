package com.example.reader;



import android.annotation.SuppressLint;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;


import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;

import java.security.spec.PKCS8EncodedKeySpec;

import java.util.Base64;


import org.bouncycastle.jce.provider.BouncyCastleProvider;


import java.security.*;

import java.util.HashMap;

import javax.crypto.Cipher;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Utils {
    private static final String HEX_CHARS = "0123456789ABCDEF";
    private static final char[] HEX_CHARS_ARRAY = "0123456789ABCDEF".toCharArray();

    @SuppressLint("StaticFieldLeak")
    public static TextView nameId,dob,idNumber,bloodType,allergy,vaccination,medicalHistory,eventType, ticketID, seatNumber, date;
    public static CardView idCard, healthCard,ticketCard;
    static String IDENTITY_APP_ID = "482730";
    static String HEALTHCARE_APP_ID = "915460";
     static String  TICKETING_APP_ID = "307210";

    private static final String SALT = "fixed_salt"; // Should be unique per user if possible
    private static final int KEY_SIZE = 256;
    private static final int ITERATIONS = 10000;
    private static final int GCM_TAG_LENGTH = 128;

    private static HashMap<Fragment, String> fragmentMap = new HashMap<>();

    public static final HashMap<String, String> aesKeys;
    static {
        aesKeys = new HashMap<>();
        aesKeys.put(IDENTITY_APP_ID, "QXNNSmdEY0duRGFMZ05SbFFHc0oxRmdqS3MyVnF1TXc=");
        aesKeys.put(HEALTHCARE_APP_ID, "anp0bmNXbW92UVZnTDRwREdpTFA0ZDBnR214cmppZ0I=");
        aesKeys.put(TICKETING_APP_ID, "bWJNM1hmeXZkZTlhMVVDNGRuZTRLTDBRYlFpRWhFZ3U=");
    }


    public static void put(Fragment fragment, String value){
        fragmentMap.put(fragment,value);
    }

    public static String get(Fragment fragment){
        return fragmentMap.get(fragment);
    }


    public static String ISSUER_PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCXdllUtpHZWS0x" +
            "74KMXJXoF2bZGYFdYiBlrz1wZhRQPXyYcbFCekJsd3kO6akDAOtJdKmxG9qky1Nu" +
            "DCDWnOYodloSoDO+ccPfcaTvMlWIDfVtyAlxtsSBD7EiOLwcKJ6oKdeDeV0jo6r/" +
            "cpVgVZlx+v/h5QA2VFa9yV28iIwLZu5GRvqB2K6L4YyxPOrjat/7KE2Eh0wYUivv" +
            "7QTv2LlzxfR4XbCfjOwoWdbJSJTLIfDJhpPRT02Xwx8FxEdMJq9ip0uNjOC87auh" +
            "WVBH2IjTBeI/EV5FCpRSNbgdyXOnOMsedt6xPvR4FDC+kjvv24dmUuflcdELL4Ys" +
            "Jw87TUirAgMBAAECggEBAIGnCWj+r3LCZ3GreLD2QExgW+XTc22gpeLlU05300Os" +
            "no9QI1Con1uCGdquIcM0cjR67RdypIMUmeJF94Y+4LYaOZhgEmsfr3ACNBGdoKHK" +
            "cSD8bzksHqHr8NE+h6gDDW0eBCHDLftoCuSIDV5dZ/Ctz4RrJvda0rW7PcY5jMfk" +
            "juTeNT5aUEECgKrgCkR/wgQ63QMxRHetDxvqJNZEQaGPDOkJQWkPfL1HbzE+Fcm4" +
            "BanXYo16VAEA52ZmTQ6kOaZd3NA/z2ZnwM2YudSX27Fnvs7JezSRSMIiT2hia4nW" +
            "xvwKjES8JCJbl3+Gj856n7+oEj6W3dGYL+sUEwZ8AqECgYEAzsmTwKXX1FqoEsyg" +
            "dTXtkBZbnf0CN0gipXexqjAPLzjyPwIbEDE8puGIjeWpfAEZZVFKXijv5es1I2u3" +
            "ZPRwAc4pkhgiARb5+n8JQ6dXRqkr9Ol6vA7KUlIpQoF8We1aLAbcQ6j4kMRj+Tli" +
            "YHl1DzIyf9SXAF4Yv24QHGFqgAcCgYEAu4IdwoSXVl8QU3IWfqyMohsZO8uTSKXU" +
            "2Wz2xiq6MIaTBtoq/vi0YXllaBlQ+lgIzZxHwxQ1RR+6SAMy475rBH1Ugzy9GwP6" +
            "REQqTfLo+KuwMxFkuHP/w3Q3PNlsKutX34f2kDrQ4+HVXyuJVk0ebrzz6jwlZUEh" +
            "gJS1uRF9QT0CgYBaX5qlXVWgRyahYLDXyQPULxFHUOYBBxOtQUxyVqKsPrUQeHkf" +
            "cIVTYcnuSeryeQCCWS6pTDbcQxlsK41xH6s7/sAIS2fBFiuWIKMJ4D6ycQj29ntf" +
            "aQ9fPu0tVa3lF2iLSUxGfbh0fIA80al/BFX2mDedymlcDcO/FfkQjjqfTQKBgHt6" +
            "CsjucY5SGbkptwCB9jZF7A8BVyMO+SSY6cTDnEqaRDXN82RmOLq7q3iquzWwRVPI" +
            "50TiiNXVN+F2IcvxuB2DvuSRWeJxcxwDW2xrWtlujiCDArWoxbNbU4jBkMaOphYw" +
            "PcDqymZWcCE01UK8lB7OVT1ZkDKmej5nL2gIyVftAoGAO1A3vYzDB6dD34pTm09e" +
            "c4lyc7h1sH32rj4L9TWG/Vvqs7WaKBUFwTK970hLDJv6vZv0cDac+lFgwMji8N15" +
            "PsGrGZXsBQzzE6dbiEcOO38TNQ8WzRskMS8fygj4ftQx5Sk8PiI5Yw5Yd/eYvqIr" +
            "enPjgPIqseUXkPhngW3SU4c=";

    static {
            Security.addProvider(new BouncyCastleProvider());
    }

    public static SecretKey generateKey(String seed) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(seed.toCharArray(), SALT.getBytes(), ITERATIONS, KEY_SIZE);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(),"AES");
    }


    public static String encrypt(String plaintext, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12]; // GCM standard IV size
        new SecureRandom().nextBytes(iv); // Generate a new IV

        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] encryptedData = cipher.doFinal(plaintext.getBytes());

        // Combine IV + encrypted data and encode to Base64
        byte[] combined = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    // Decrypt data using AES-GCM
    public static String decrypt(String encryptedText, SecretKey key) throws Exception {
        byte[] decodedData = Base64.getDecoder().decode(encryptedText);
        byte[] iv = new byte[12];
        byte[] encryptedData = new byte[decodedData.length - 12];

        System.arraycopy(decodedData, 0, iv, 0, 12);
        System.arraycopy(decodedData, 12, encryptedData, 0, encryptedData.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        byte[] decryptedData = cipher.doFinal(encryptedData);
        return new String(decryptedData);
    }



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

//    public static PublicKey getIssuerPublicKey() throws Exception{
//        byte[] issuerPublicKeyBytes = Base64.getDecoder().decode(ISSUER_PUBLIC_KEY);
//        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(issuerPublicKeyBytes);
//        return keyFactory.generatePublic(publicKeySpec);
//    }

    public static PrivateKey getIssuerPrivateKey() throws Exception{
        byte[] issuerPrivateKeyBytes = Base64.getDecoder().decode(ISSUER_PRIVATE_KEY);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(issuerPrivateKeyBytes);
        return keyFactory.generatePrivate(privateKeySpec);
    }

    public static String createIndex(){
        byte[] index = new byte[16]; // 16-byte (128-bit) index
        new SecureRandom().nextBytes(index); // Generate random bytes
        return toHex(index); // Convert to HexString

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

    public static byte[] concatenateArrays(byte[] array1, byte[] array2) {
        byte[] result = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }
}
