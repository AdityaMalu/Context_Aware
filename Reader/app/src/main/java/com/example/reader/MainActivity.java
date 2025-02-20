package com.example.reader;

import static com.example.reader.Utils.concatenateArrays;
import static com.example.reader.Utils.getIssuerPrivateKey;
import static com.example.reader.Utils.hexStringToByteArray;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import com.example.reader.Utils;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {
    private NfcAdapter nfcAdapter;
    private TextView statusTextView;
    private EditText amountEditText;
    private IsoDep isoDep;
    private Fragment activeFragment;
    private HashMap<Fragment, String> fragmentMap = new HashMap<>(); // Map to store fragments and their names


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        Button btnId = findViewById(R.id.btn_id);
        Button btnTicket = findViewById(R.id.btn_ticket);
        Button btnHealthcare = findViewById(R.id.btn_healthcare);

        IDFragment idFragment = new IDFragment();
        TicketFragment ticketFragment = new TicketFragment();
        HealthCareFragment healthCareFragment = new HealthCareFragment();

        fragmentMap.put(idFragment, "482730");
        fragmentMap.put(ticketFragment, "307210");
        fragmentMap.put(healthCareFragment, "915460");

        activeFragment = idFragment;
        loadFragment(activeFragment);

        btnId.setOnClickListener(v -> switchFragment(idFragment));
        btnTicket.setOnClickListener(v -> switchFragment(ticketFragment));
        btnHealthcare.setOnClickListener(v -> switchFragment(healthCareFragment));
    }

    private void switchFragment(Fragment fragment) {
        if (activeFragment != fragment) {
            activeFragment = fragment;
            loadFragment(fragment);
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container, fragment);
        fragmentTransaction.commit();

        // Log or use the fragment name
        String fragmentName = fragmentMap.get(fragment);
        if (fragmentName != null) {
            System.out.println("Active Fragment: " + fragmentName);
        }
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

    private void sendApduCommand(byte[] command) {
        if (isoDep == null || !isoDep.isConnected()) {
            statusTextView.setText("No NFC Tag connected");
            return;
        }

        try {
            Log.i("INFO", "Sending apdu");
            byte[] response = isoDep.transceive(command);
            Log.i("INFO", "Response received" + Arrays.toString(new String[]{toHex(response)}));
            handleResponse(response);
        } catch (IOException e) {
            statusTextView.setText("APDU Command failed");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] signChallenge(byte[] challenge, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(challenge);
        return signature.sign();
    }

    private void handleResponse(byte[] response) throws Exception {
        if (response.length < 2) {
            statusTextView.setText("Invalid response");
            return;
        }
        Log.i("INFO", "Response received" + Arrays.toString(new String[]{toHex(response)}));

        byte sw1 = response[response.length - 2];
        byte sw2 = response[response.length - 1];
        if (sw1 == (byte) 0x90 && sw2 == (byte) 0x00) {
            if (response[1] == (byte) 0x10) {
                // Extract the balance from response for GET_BALANCE
                byte[] challenge = new byte[16];
                System.arraycopy(response,5,challenge,0,16);
                byte[] signedChallenge = signChallenge(challenge,getIssuerPrivateKey());
                byte[] command = new byte[]{(byte) 0x80 , 0x20 , 0x00, 0x00, 0x00};
                command = concatenateArrays(command,signedChallenge);
                sendApduCommand(command);
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
//                runOnUiThread(() -> statusTextView.setText("HCE Device Connected"));
                byte[] data = (Utils.hexStringToByteArray(
                        "00A4040007A0000002471001"));
                byte[] appID = hexStringToByteArray(Objects.requireNonNull(fragmentMap.get(activeFragment)));
                Log.d("Acitve Fragment", Arrays.toString(appID));
                byte[] command = concatenateArrays(data,appID);
                Log.d("Auth" , toHex(command));
                Log.i("INFO", "Response " + Arrays.toString(new String[]{toHex(command)}));
                byte[] response = isoDep.transceive(command);
                handleResponse(response);
            } catch (Exception e) {
//                runOnUiThread(() -> statusTextView.setText("Failed to connect to HCE Device"));
            }
        }
    }
}
