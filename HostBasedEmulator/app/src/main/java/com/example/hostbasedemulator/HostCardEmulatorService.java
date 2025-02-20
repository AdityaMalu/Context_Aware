package com.example.hostbasedemulator;

import static com.example.hostbasedemulator.Utils.PREF_NAME;
import static com.example.hostbasedemulator.Utils.getIssuerPublicKey;
import static com.example.hostbasedemulator.Utils.toHex;

import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.time.LocalDate;
import java.util.Arrays;

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
                    case (byte) 0x40: // DEBIT
                        return debitBalance(apdu);
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

    private byte[] creditBalance(byte[] apdu) {
        // Ensure the APDU has enough data for the extended length amount (2 bytes)
        if (apdu.length < 7) { // CLA (1) + INS (1) + P1 (1) + P2 (1) + LC (1) + Amount (2)
            return UNKNOWN_COMMAND_RESPONSE;
        }

        // Read the 2-byte amount from the APDU
        short creditAmount = (short) (((apdu[5] & 0xFF) << 8) | (apdu[6] & 0xFF));

        // Validate the amount (e.g., ensure it's positive and within a reasonable range)
        if (creditAmount < 0 || creditAmount > 32767) { // Max positive value for a signed short
            return new byte[]{(byte) 0x6A, (byte) 0x83}; // Invalid transaction amount
        }

        // Update the balance
        int newBalance = balance + creditAmount;
        if (newBalance > 0x7FFF) { // Check for overflow (max positive value for a signed short)
            return new byte[]{(byte) 0x6A, (byte) 0x84}; // Exceed maximum balance
        }

        balance = (short) newBalance;
        return new byte[]{(byte) 0x90, (byte) 0x00}; // Success
    }


    private byte[] debitBalance(byte[] apdu) {
        // Ensure the APDU has enough data for the extended length amount (2 bytes)
        if (apdu.length < 7) { // CLA (1) + INS (1) + P1 (1) + P2 (1) + LC (1) + Amount (2)
            return UNKNOWN_COMMAND_RESPONSE;
        }

        // Read the 2-byte amount from the APDU
        short debitAmount = (short) (((apdu[5] & 0xFF) << 8) | (apdu[6] & 0xFF));

        // Validate the amount (e.g., ensure it's positive and within a reasonable range)
        if (debitAmount < 0 || debitAmount > 32767) { // Max positive value for a signed short
            return new byte[]{(byte) 0x6A, (byte) 0x83}; // Invalid transaction amount
        }

        // Update the balance
        int newBalance = balance - debitAmount;
        if (newBalance < 0) { // Check for negative balance
            return new byte[]{(byte) 0x6A, (byte) 0x85}; // Negative balance
        }

        balance = (short) newBalance;
        return new byte[]{(byte) 0x90, (byte) 0x00}; // Success
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
            String appID = data.substring(24);
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
            return new byte[] {
                    (byte) 0x80, (byte) 0x20, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x90, (byte) 0x00
            };
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

    // Helper method to handle extended length APDUs
    private int getExtendedLength(byte[] apdu, int offset) {
        if (apdu.length < offset + 2) {
            return -1; // Invalid length
        }
        return ((apdu[offset] & 0xFF) << 8) | (apdu[offset + 1] & 0xFF);
    }
}