package com.example.reader;

import static com.example.reader.Utils.HEALTHCARE_APP_ID;
import static com.example.reader.Utils.IDENTITY_APP_ID;
import static com.example.reader.Utils.TICKETING_APP_ID;
import static com.example.reader.Utils.aesKeys;
import static com.example.reader.Utils.concatenateArrays;

import static com.example.reader.Utils.decrypt;
import static com.example.reader.Utils.generateKey;
import static com.example.reader.Utils.get;
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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Objects;

import com.example.reader.Utils;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback, APDUCommandListner{
    private NfcAdapter nfcAdapter;
    private TextView statusTextView;
    private EditText amountEditText;
    private static IsoDep isoDep;
    public static Fragment activeFragment;

    private String decryptedIndex;

    ApiService apiService = RetrofitClient.getApiService();

    IDFragment idFragment;
    TicketFragment ticketFragment;
    HealthCareFragment healthCareFragment;

    // Map to store fragments and their names


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        Button btnId = findViewById(R.id.btn_id);
        Button btnTicket = findViewById(R.id.btn_ticket);
        Button btnHealthcare = findViewById(R.id.btn_healthcare);

        idFragment = new IDFragment(this::sendApduCommand);
        ticketFragment = new TicketFragment(this::sendApduCommand);
        healthCareFragment = new HealthCareFragment(this::sendApduCommand);

        Utils.put(idFragment, IDENTITY_APP_ID);
        Utils.put(ticketFragment, TICKETING_APP_ID);
        Utils.put(healthCareFragment, HEALTHCARE_APP_ID);

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
        String fragmentName = Utils.get(fragment);
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

    public static boolean checkTag(){
        try{
            return isoDep.isConnected();
        }
        catch (Exception e){
            Log.d("Security Exception" , "The Card has lost the connection");
        }
        return false;
    }

    public void sendApduCommand(byte[] command) {
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

    private String decrpytIndex(byte[] encrpytedIndex) throws Exception{
        byte[] decodedKey = Base64.getDecoder().decode(aesKeys.get(get(activeFragment)));
        System.out.println("Decoded data " + (Arrays.toString(encrpytedIndex)));
        SecretKey secretKey = new SecretKeySpec(decodedKey, "AES");
        ByteBuffer byteBuffer = ByteBuffer.wrap(encrpytedIndex);

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

    private void sendIndextoServer(String decryptedIndex){
        String fragmentValue = Utils.get(activeFragment);
        String seed = fragmentValue + decryptedIndex;
        Log.d("Seed", seed);
        byte[] seedBytes = Utils.hexStringToByteArray(seed);

        if (Objects.equals(fragmentValue, Utils.get(idFragment))) {
            fetchIDData(decryptedIndex);
        } else if (Objects.equals(fragmentValue, Utils.get(healthCareFragment))) {
            fetchHealthData(decryptedIndex);
        }

    }

    private void fetchIDData(String decryptedIndex){
        DisplayRequest request = new DisplayRequest(decryptedIndex,get(activeFragment));
        Call<DisplayResponse> call = apiService.displayDetails(request);

        call.enqueue(new Callback<DisplayResponse>() {
            @Override
            public void onResponse(Call<DisplayResponse> call, Response<DisplayResponse> response) {
                if(response.isSuccessful()){
                    DisplayResponse body = response.body();
                    String encryptedData = body.getMessage();
                    Log.d("server response" , encryptedData);
                    String fragmentValue = Utils.get(MainActivity.activeFragment);
                    String seed = fragmentValue + decryptedIndex;
                    Log.d("Seed" , seed);
                    try {
                        SecretKey key = generateKey(seed);
                        String decryptedJSON = decrypt(encryptedData,key);
                        IDData idData = IDData.fromJson(decryptedJSON);
                        Utils.idCard.setVisibility(View.VISIBLE);
                        Utils.nameId.setText("Name : " + idData.getName());
                        Utils.dob.setText("DOB : " + idData.getDob());
                        Utils.idNumber.setText("ID : " + idData.getIdNumber());

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                else{
                    Log.e("Request failed", "Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<DisplayResponse> call, Throwable t) {
                Log.e("Request failed", "Response code: " + t);
            }
        });
    }

    void fetchHealthData(String decryptedIndex){
        DisplayRequest request = new DisplayRequest(decryptedIndex,get(activeFragment));
        Call<DisplayResponse> call = apiService.displayHealthDetails(request);

        call.enqueue(new Callback<DisplayResponse>() {
            @Override
            public void onResponse(Call<DisplayResponse> call, Response<DisplayResponse> response) {
                if(response.isSuccessful()){
                    DisplayResponse body = response.body();
                    String encryptedData = body.getMessage();
                    Log.d("server response" , encryptedData);
                    String fragmentValue = Utils.get(MainActivity.activeFragment);
                    String seed = fragmentValue + decryptedIndex;
                    Log.d("Seed" , seed);
                    try {
                        SecretKey key = generateKey(seed);
                        String decryptedJSON = decrypt(body.getMessage(), key);
                        HealthCareData healthData = HealthCareData.fromJson(decryptedJSON);

                        Log.d("HealthData", "Blood Type: " + healthData.getBloodType());
                        Log.d("HealthData", "Allergy: " + healthData.getAllergy());
                        Log.d("HealthData", "Vaccination: " + healthData.getVaccination());
                        Log.d("HealthData", "Medical History: " + healthData.getMedicalHistory());

                        Utils.healthCard.setVisibility(View.VISIBLE);
                        Utils.bloodType.setText("Blood Type: " + healthData.getBloodType());
                        Utils.allergy.setText("Allergy: " + healthData.getAllergy());
                        Utils.vaccination.setText("Vaccination: " + healthData.getVaccination());
                        Utils.medicalHistory.setText("History: " + healthData.getMedicalHistory());

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                else{
                    Log.e("Request failed", "Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<DisplayResponse> call, Throwable t) {
                Log.e("Request failed", "Response code: " + t);
            }
        });
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
            }
            else if(response[1] == (byte) 0x20){
                if(response.length == 7){
                    Log.d("Index Saved" , toHex(response));
                }
                else{
                    byte[] indexBytes = new byte[response.length - 7];
                    System.arraycopy(response,5,indexBytes,0, response.length-7);
                    Log.d("Index Bytes" , Arrays.toString(indexBytes));
                    decryptedIndex = decrpytIndex(indexBytes);
                    Log.d("Decrtypted Index" , decryptedIndex);
                    sendIndextoServer(decryptedIndex);
                }

            }
            else {
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
                byte[] appID = hexStringToByteArray(Objects.requireNonNull(Utils.get(activeFragment)));
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
