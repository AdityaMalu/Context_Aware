package com.example.reader;

import static com.example.reader.Utils.toHex;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Arrays;
import com.example.reader.Utils;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {
    private NfcAdapter nfcAdapter;
    private TextView statusTextView;
    private EditText amountEditText;
    private IsoDep isoDep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        statusTextView = findViewById(R.id.tv_status);
        amountEditText = findViewById(R.id.et_amount);

        Button btnCredit = findViewById(R.id.btn_credit);
        Button btnDebit = findViewById(R.id.btn_debit);
        Button btnGetBalance = findViewById(R.id.btn_get_balance);

        btnCredit.setOnClickListener(v -> sendApduCommand((byte) 0x30)); // Credit
        btnDebit.setOnClickListener(v -> sendApduCommand((byte) 0x40)); // Debit
        btnGetBalance.setOnClickListener(v -> sendApduCommand((byte) 0x50)); // Get Balance
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableReaderMode(this, this,
                    NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(this);
        }
    }

    private void sendApduCommand(byte instruction) {
        if (isoDep == null || !isoDep.isConnected()) {
            statusTextView.setText("No NFC Tag connected");
            return;
        }

        byte[] command;
        if (instruction == (byte) 0x50) { // GET_BALANCE
            Log.i("INFO", "Get Balance command");
            command = new byte[]{(byte) 0x80, instruction, 0x00, 0x00, 0x00};
            Log.i("INFO", "Processed command" + Arrays.toString(new String[]{toHex(command)}));
        } else {
            String amountStr = amountEditText.getText().toString();
            if (amountStr.isEmpty()) {
                statusTextView.setText("Enter a valid amount");
                return;
            }

            byte amount = (byte) Integer.parseInt(amountStr);
            Log.i("INFO", "Amount " + amount);
            command = new byte[]{(byte) 0x80, instruction, 0x00, 0x00, 0x01, amount};
            Log.i("INFO", "Processed command" + Arrays.toString(new String[]{toHex(command)}));
        }

        try {
            Log.i("INFO", "Sending apdu");
            byte[] response = isoDep.transceive(command);
            Log.i("INFO", "Response received" + Arrays.toString(new String[]{toHex(response)}));
            handleResponse(response);
        } catch (IOException e) {
            statusTextView.setText("APDU Command failed");
        }
    }

    private void handleResponse(byte[] response) {
        if (response.length < 2) {
            statusTextView.setText("Invalid response");
            return;
        }
        Log.i("INFO", "Response received" + Arrays.toString(new String[]{toHex(response)}));

        byte sw1 = response[response.length - 2];
        byte sw2 = response[response.length - 1];
        if (sw1 == (byte) 0x90 && sw2 == (byte) 0x00) {
            if (response.length > 2) {
                // Extract the balance from response for GET_BALANCE
                short balance = (short) ((response[0] << 8) | (response[1] & 0xFF));
                statusTextView.setText("Balance: " + balance);
            } else {
                statusTextView.setText("Command executed successfully");
            }
        } else {
            statusTextView.setText("Error: SW=" + String.format("%02X%02X", sw1, sw2));
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        isoDep = IsoDep.get(tag);
        if (isoDep != null) {
            try {
                isoDep.connect();
                runOnUiThread(() -> statusTextView.setText("HCE Device Connected"));
                byte[] response = isoDep.transceive(Utils.hexStringToByteArray(
                        "00A4040007A0000002471001"));
                Log.i("INFO", "Response " + Arrays.toString(new String[]{toHex(response)}));
                handleResponse(response);
            } catch (IOException e) {
                runOnUiThread(() -> statusTextView.setText("Failed to connect to HCE Device"));
            }
        }
    }
}
