package com.example.reader;

import androidx.fragment.app.Fragment;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Utils {
    private static final String HEX_CHARS = "0123456789ABCDEF";
    private static final char[] HEX_CHARS_ARRAY = "0123456789ABCDEF".toCharArray();

    private static HashMap<Fragment, String> fragmentMap = new HashMap<>();

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
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static KeyPair generateKeyPairFromSeed(byte[] seed) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");

        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");

        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(seed);

        keyPairGenerator.initialize(ecSpec, secureRandom);
        return keyPairGenerator.generateKeyPair();
    }

    public static SecretKey deriveAESKey(PrivateKey privateKey, PublicKey publicKey) throws Exception {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");

        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);

        byte[] sharedSecret = keyAgreement.generateSecret();
        byte[] aesKeyBytes = MessageDigest.getInstance("SHA-256").digest(sharedSecret);  // Hashing for AES key
        return new SecretKeySpec(aesKeyBytes, 0, 16, "AES"); // AES-128
    }

    public static String encryptAES(String plaintext, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        byte[] iv = new byte[16];  // Zero IV (for simplicity)
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decryptAES(String encryptedText, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        byte[] iv = new byte[16];  // Zero IV (same as encryption)
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));

        return new String(decryptedBytes, StandardCharsets.UTF_8);
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
