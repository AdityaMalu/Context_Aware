package com.example.hostbasedemulator;

import static com.example.hostbasedemulator.Utils.*;
import static com.example.hostbasedemulator.ContextManager.*;

import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class HostCardEmulatorService extends HostApduService {
    public static final String TAG = "HostCardEmulator";
    public static final String STATUS_SUCCESS = "9000";
    public static final String STATUS_FAILED = "6F00";
    public static final String CLA_NOT_SUPPORTED = "6E00";
    public static final String INS_NOT_SUPPORTED = "6D00";
    public static final String AID = "A0000002471001";
    public static final String SELECT_INS = "A4";
    public static final String DEFAULT_CLA = "00";
    public static final int MIN_APDU_LENGTH = 12;
    private static final byte[] SELECT_AID_RESPONSE = {(byte) 0x90, (byte) 0x00}; // Success status word
    private static final byte[] UNKNOWN_COMMAND_RESPONSE = {(byte) 0x6F, (byte) 0x00}; // Unknown command
    private short balance = 0;
    private String appID;

    private byte[] challenge = new byte[16];

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "Deactivated: " + reason);
    }

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        Log.i("INFO", "Apdu received " + Arrays.toString(apdu));
        // Handle SELECT AID command
        if (isSelectAidCommand(apdu)) {
            return challenge(apdu);
        }

        // Parse APDU command
        if (apdu.length < 4) {
            return UNKNOWN_COMMAND_RESPONSE;
        }

        byte cla = apdu[0];
        byte ins = apdu[1];

        if (cla == (byte) 0x80) {
            try {
                switch (ins) {
                    case (byte) 0x20:
                        return checkSignature(apdu);
                    case (byte) 0x30: // CREDIT
                        return createRecord(apdu);
                    case (byte) 0x40:
                        return crossResourceRequest(apdu);
                    default:
                        return UNKNOWN_COMMAND_RESPONSE;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return UNKNOWN_COMMAND_RESPONSE;
    }

    private byte[] createRecord(byte[] apdu) {
        byte[] appIdBytes = new byte[3];
        System.arraycopy(apdu, 5, appIdBytes, 0, 3);
        String appId = toHex(appIdBytes);

        byte[] indexBytes = new byte[16];
        System.arraycopy(apdu, 8, indexBytes, 0, 16);
        String index = toHex(indexBytes);
        Log.d("PREF", appId + " - " + index);

        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(appId, index); // Storing the value
        editor.apply();

        return new byte[] {
                (byte) 0x90, (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x90, (byte) 0x00
        };
    }

    private boolean isSelectAidCommand(byte[] apdu) {
        // Check if the APDU is a SELECT AID command
        Log.d("APDU", toHex(apdu));
        return apdu.length >= 5 && apdu[1] == (byte) 0xA4;
    }

    private byte[] challenge(byte[] apdu) {
        // Check if the APDU is a SELECT AID command
        Log.d("APDU", toHex(apdu));
        if (apdu.length >= 5 && apdu[1] == (byte) 0xA4) {
            String data = toHex(apdu);
            appID = data.substring(24);
            Log.d("AppID", appID);

            challenge = generateChallenge();

            if (challenge.length != 16) {
                Log.e("ERROR", "Challenge size is not 16 bytes!");
                return new byte[]{(byte) 0x6A, (byte) 0x80};
            }

            byte[] response = new byte[5 + 16 + 2];

            response[0] = (byte) 0x90;
            response[1] = (byte) 0x10;
            response[2] = (byte) 0x00;
            response[3] = (byte) 0x00;
            response[4] = (byte) 0x18;

            System.arraycopy(challenge, 0, response, 5, 16);

            response[21] = (byte) 0x90;
            response[22] = (byte) 0x00;

            return response;
        }
        return new byte[]{(byte) 0x6A, (byte) 0x82};
    }

    public byte[] generateChallenge(){
        byte[] challenge = new byte[16];
        new SecureRandom().nextBytes(challenge);
        return challenge;
    }

    public byte[] checkSignature(byte[] apdu) throws Exception {
        byte[] signedData = new byte[256];
        System.arraycopy(apdu, 5, signedData, 0, 256);
        PublicKey publicKey = getIssuerPublicKey();
        if (verifySignature(signedData, publicKey)) {
            Log.d("SUCCESS", "9000");

            SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            editor.remove(appID); // Removes only this key
//            editor.apply();
            if (sharedPreferences.contains(appID)) {
                String index = sharedPreferences.getString(appID, "");
                byte[] encryptedIndex = encryptionAES(Base64.getDecoder().decode(aesKeys.get(appID)), index);
                Log.d("EncIndex", String.valueOf(encryptedIndex.length));

                byte[] response = new byte[7 + encryptedIndex.length];
                response[0] = (byte) 0x90;
                response[1] = (byte) 0x20;
                response[2] = (byte) 0x00;
                response[3] = (byte) 0x00;
                response[4] = (byte) (encryptedIndex.length + 2);

                System.arraycopy(encryptedIndex, 0, response, 5, encryptedIndex.length);

                response[response.length - 2] = (byte) 0x90;
                response[response.length - 1] = (byte) 0x00;

                Log.d("Response", Arrays.toString(response));

                byte[] indexBytes = new byte[response.length - 7];
                System.arraycopy(response, 5, indexBytes, 0, response.length - 7);



                return response;
            } else {
                return new byte[] {
                    (byte) 0x90, (byte) 0x20, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x90, (byte) 0x00
            };
            }
        }
        return new byte[] {
                (byte) 0x80, (byte) 0x20, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x6F, (byte) 0x00
        };
    }

    public boolean verifySignature(byte[] signedData, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(challenge);
        return signature.verify(signedData);
    }

    private byte[] encryptionAES(byte[] decodedKey, String plainText) throws Exception{
        System.out.println("Plain text " + plainText);
        SecretKey secretKey = new SecretKeySpec(decodedKey, "AES");
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        IvParameterSpec ivspec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);

        byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        System.out.println("Enc text " + Arrays.toString(encryptedData));

        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedData);
        System.out.println("Byte buffer " + Arrays.toString(byteBuffer.array()) + " length " + byteBuffer.array().length);
        return byteBuffer.array();
    }

    private byte[] crossResourceRequest(byte[] apdu) throws Exception {
        byte[] response = new byte[0];
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        byte[] requestingBytes = new byte[3];
        byte[] requestedBytes = new byte[3];
        byte[] contextVariables = new byte[apdu.length - 11];

        System.arraycopy(apdu, 5, requestingBytes, 0, 3);
        System.arraycopy(apdu, 8, requestedBytes, 0,3);
        System.arraycopy(apdu, 11, contextVariables, 0, apdu.length - 11);

        String requesting = toHex(requestingBytes);
        String requested = toHex(requestedBytes);

        if (requesting.equals(HEALTHCARE_APP_ID) && requested.equals(IDENTITY_APP_ID)) {
            Log.d("REQUESTING", "HEALTHCARE_APP_ID");
            Log.d("REQUESTED", "IDENTITY_APP_ID");
            Log.d("PREF", String.valueOf(sharedPreferences.contains(requested)));
            if (sharedPreferences.contains(requested) && healthcareIdentity(contextVariables)) {
                String index = sharedPreferences.getString(requested, "");
                byte[] encryptedIndex = encryptionAES(Base64.getDecoder().decode(aesKeys.get(requesting)), index);
                response = createResponse(encryptedIndex, requestedBytes);
            } else {
                response = accessDeniedResponse();
            }
        } else if (requesting.equals(TICKETING_APP_ID) && requested.equals(IDENTITY_APP_ID)) {
            Log.d("REQUESTING", "TICKETING_APP_ID");
            Log.d("REQUESTED", "IDENTITY_APP_ID");
            Log.d("PREF", String.valueOf(sharedPreferences.contains(requested)));
            if (sharedPreferences.contains(requested) && ticketIdentity(contextVariables)) {
                String index = sharedPreferences.getString(requested, "");
                byte[] encryptedIndex = encryptionAES(Base64.getDecoder().decode(aesKeys.get(requesting)), index);
                response = createResponse(encryptedIndex, requestedBytes);
            } else {
                response = accessDeniedResponse();
            }
        } else if (requesting.equals(HEALTHCARE_APP_ID) && requested.equals(TICKETING_APP_ID)) {
            Log.d("REQUESTING", "HEALTHCARE_APP_ID");
            Log.d("REQUESTED", "TICKETING_APP_ID");
            Log.d("PREF", String.valueOf(sharedPreferences.contains(requested)));
            if (sharedPreferences.contains(requested) && healthcareTicket(contextVariables)) {
                String index = sharedPreferences.getString(requested, "");
                byte[] encryptedIndex = encryptionAES(Base64.getDecoder().decode(aesKeys.get(requesting)), index);
                response = createResponse(encryptedIndex, requestedBytes);
            } else {
                response = accessDeniedResponse();
            }
        } else if (requesting.equals(TICKETING_APP_ID) && requested.equals(HEALTHCARE_APP_ID)) {
            Log.d("REQUESTING", "TICKETING_APP_ID");
            Log.d("REQUESTED", "HEALTHCARE_APP_ID");
            Log.d("PREF", String.valueOf(sharedPreferences.contains(requested)));
            if (sharedPreferences.contains(requested) && ticketHealthcare(contextVariables)) {
                String index = sharedPreferences.getString(requested, "");
                byte[] encryptedIndex = encryptionAES(Base64.getDecoder().decode(aesKeys.get(requesting)), index);
                response = createResponse(encryptedIndex, requestedBytes);
            } else {
                response = accessDeniedResponse();
            }
        } else {
            System.out.println("Invalid request");
        }
        Log.d("RESPONSE", toHex(response));
        return response;
    }

    private byte[] createResponse(byte[] data, byte[] requestedBytes) {
        byte[] response = new byte[data.length + requestedBytes.length + 7];
        response[0] = (byte) 0x80;
        response[1] = (byte) 0x40;
        response[2] = (byte) 0x00;
        response[3] = (byte) 0x00;
        response[4] = (byte) (data.length + requestedBytes.length + 2);

        System.arraycopy(requestedBytes, 0, response, 5, requestedBytes.length);
        System.arraycopy(data, 0, response, requestedBytes.length + 5, data.length);

        response[response.length - 2] = (byte) 0x90;
        response[response.length - 1] = (byte) 0x00;

        return response;
    }

    private byte[] accessDeniedResponse() {
        byte[] response = new byte[9];
        response[0] = (byte) 0x80;
        response[1] = (byte) 0x40;
        response[2] = (byte) 0x00;
        response[3] = (byte) 0x00;
        response[4] = (byte) 0x02;
        response[5] = (byte) 0x40;
        response[6] = (byte) 0x40;
        response[7] = (byte) 0x90;
        response[8] = (byte) 0x00;

        return response;
    }

    private String decryptionAES(byte[] decodedKey, byte[] encryptedText)throws Exception {
        System.out.println("Decoded data " + Arrays.toString(encryptedText));
        SecretKey secretKey = new SecretKeySpec(decodedKey, "AES");
        ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedText);

        byte[] iv = new byte[16];
        byteBuffer.get(iv);  // Extract IV
        byte[] encryptedData = new byte[byteBuffer.remaining()];
        byteBuffer.get(encryptedData);

        IvParameterSpec ivspec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);

        byte[] decryptedData = cipher.doFinal(encryptedData);
        System.out.println("Dec data " + Arrays.toString(decryptedData));
        return new String(decryptedData, StandardCharsets.UTF_8);
    }
}