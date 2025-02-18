package com.example.hostbasedemulator;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

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

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "Deactivated: " + reason);
    }

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        Log.i("INFO", "Apdu received " + Arrays.toString(apdu));
        // Handle SELECT AID command
        if (isSelectAidCommand(apdu)) {
            return SELECT_AID_RESPONSE;
        }

        // Parse APDU command
        if (apdu.length < 4) {
            return UNKNOWN_COMMAND_RESPONSE;
        }

        byte cla = apdu[0];
        byte ins = apdu[1];

        if (cla == (byte) 0x80) {
            switch (ins) {
                case (byte) 0x50: // GET_BALANCE
                    return getBalanceResponse();
                case (byte) 0x30: // CREDIT
                    return creditBalance(apdu);
                case (byte) 0x40: // DEBIT
                    return debitBalance(apdu);
                default:
                    return UNKNOWN_COMMAND_RESPONSE;
            }
        }

        return UNKNOWN_COMMAND_RESPONSE;
    }

    private byte[] getBalanceResponse() {
        return new byte[]{
                (byte) (balance >> 8), // High byte of balance
                (byte) (balance & 0xFF), // Low byte of balance
                (byte) 0x90, (byte) 0x00 // Success status word
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
        if (apdu.length >= 5 && apdu[1] == (byte) 0xA4) {
            return true;
        }
        return false;
    }

    // Helper method to handle extended length APDUs
    private int getExtendedLength(byte[] apdu, int offset) {
        if (apdu.length < offset + 2) {
            return -1; // Invalid length
        }
        return ((apdu[offset] & 0xFF) << 8) | (apdu[offset + 1] & 0xFF);
    }
}