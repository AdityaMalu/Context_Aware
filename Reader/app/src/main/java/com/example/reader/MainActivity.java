package com.example.reader;

import static android.widget.Toast.LENGTH_SHORT;
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

import android.app.AlertDialog;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.security.PrivateKey;

import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;

import java.util.Objects;
import javax.crypto.Cipher;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback, APDUCommandListner{
    private NfcAdapter nfcAdapter;
    private static IsoDep isoDep;
    public static Fragment activeFragment;
    ApiService apiService = RetrofitClient.getApiService();
    IDFragment idFragment;
    TicketFragment ticketFragment;
    HealthCareFragment healthCareFragment;

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
            Toast.makeText(this,"No NFC Tag connected",LENGTH_SHORT).show();
            return;
        }

        try {
            Log.i("INFO", "Sending apdu");
            byte[] response = isoDep.transceive(command);
            Log.i("INFO", "Response received" + Arrays.toString(new String[]{toHex(response)}));
            handleResponse(response);
        } catch (IOException e) {
            Toast.makeText(this,"APDU Command Failed",LENGTH_SHORT).show();
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
        } else if (Objects.equals(fragmentValue, Utils.get(ticketFragment))) {
            fetchTicketData(decryptedIndex);
        }

    }

    private void popUpIDData(IDData idData){
        if (idData == null) {
            Log.e("popUpIDData", "IDData is null");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ID Details");

        // Creating a message with IDData details
        String message = "Name: " + idData.getName() + "\n" +
                "DOB: " + idData.getDob() + "\n" +
                "ID Number: " + idData.getIdNumber();

        builder.setMessage(message);

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void popUpHealthData(HealthCareData healthData) {
        if (healthData == null) {
            Log.e("popUpHealthData", "HealthData is null");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Health Details");

        // Creating a message with HealthData details
        String message = "Blood Type: " + healthData.getBloodType() + "\n" +
                "Allergy: " + healthData.getAllergy() + "\n" +
                "Vaccination: " + healthData.getVaccination() + "\n" +
                "Medical History: " + healthData.getMedicalHistory();

        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void popUpAccessDenied() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Access Denied");

        builder.setMessage("You do not have permission to access this feature.");

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void popUpTicketData(TicketData ticketData) {
        if (ticketData == null) {
            Log.e("popUpTicketData", "TicketData is null");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ticket Details");

        // Creating a message with TicketData details
        String message = "Event Type: " + ticketData.getEventType() + "\n" +
                "TicketID: " + ticketData.getTicketId() + "\n" +
                "Date of Event: " + ticketData.getDateOfEvent() + "\n" +
                "Seat Number " + ticketData.getSeatNo();

        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void sendIDCrossIndextoServer(String decryptedIndex){
        DisplayRequest request = new DisplayRequest(decryptedIndex,get(idFragment));
        Call<DisplayResponse> call = apiService.displayDetails(request);

        call.enqueue(new Callback<DisplayResponse>() {
            @Override
            public void onResponse(Call<DisplayResponse> call, Response<DisplayResponse> response) {
                DisplayResponse body = response.body();
                assert body != null;
                String encryptedData = body.getMessage();
                Log.d("server response" , encryptedData);
                String fragmentValue = get(idFragment);
                String seed = fragmentValue + decryptedIndex;
                Log.d("Seed" , seed);
                try {
                    SecretKey key = generateKey(seed);
                    String decryptedJSON = decrypt(encryptedData,key);
                    IDData idData = IDData.fromJson(decryptedJSON);
                    popUpIDData(idData);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }

            @Override
            public void onFailure(Call<DisplayResponse> call, Throwable t) {

            }
        });
    }

    private void sendHealthDataCrossIndextoServer(String decryptedIndex){
        DisplayRequest request = new DisplayRequest(decryptedIndex,get(healthCareFragment));
        Call<DisplayResponse> call = apiService.displayHealthDetails(request);

        call.enqueue(new Callback<DisplayResponse>() {
            @Override
            public void onResponse(Call<DisplayResponse> call, Response<DisplayResponse> response) {
                DisplayResponse body = response.body();
                assert body != null;
                String encryptedData = body.getMessage();
                Log.d("server response" , encryptedData);
                String fragmentValue = get(healthCareFragment);
                String seed = fragmentValue + decryptedIndex;
                Log.d("Seed" , seed);
                try {
                    SecretKey key = generateKey(seed);
                    String decryptedJSON = decrypt(encryptedData,key);
                    HealthCareData healthCareData = HealthCareData.fromJson(decryptedJSON);
                    popUpHealthData(healthCareData);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }

            @Override
            public void onFailure(Call<DisplayResponse> call, Throwable t) {

            }
        });
    }

    private void sendTicketDataCrossIndextoServer(String decryptedIndex){
        DisplayRequest request = new DisplayRequest(decryptedIndex,get(ticketFragment));
        Call<DisplayResponse> call = apiService.displayTicketDetails(request);

        call.enqueue(new Callback<DisplayResponse>() {
            @Override
            public void onResponse(Call<DisplayResponse> call, Response<DisplayResponse> response) {
                DisplayResponse body = response.body();
                assert body != null;
                String encryptedData = body.getMessage();
                Log.d("server response" , encryptedData);
                String fragmentValue = get(ticketFragment);
                String seed = fragmentValue + decryptedIndex;
                Log.d("Seed" , seed);
                try {
                    SecretKey key = generateKey(seed);
                    String decryptedJSON = decrypt(encryptedData,key);
                    TicketData ticketData = TicketData.fromJson(decryptedJSON);
                    popUpTicketData(ticketData);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }

            @Override
            public void onFailure(Call<DisplayResponse> call, Throwable t) {

            }
        });
    }


    private void fetchIDData(String decryptedIndex){
        DisplayRequest request = new DisplayRequest(decryptedIndex,get(activeFragment));
        Call<DisplayResponse> call = apiService.displayDetails(request);

        call.enqueue(new Callback<DisplayResponse>() {
            @Override
            public void onResponse(Call<DisplayResponse> call, Response<DisplayResponse> response) {
                if(response.isSuccessful()){
                    DisplayResponse body = response.body();
                    assert body != null;
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

    void fetchTicketData(String decryptedIndex){
        DisplayRequest request = new DisplayRequest(decryptedIndex,get(activeFragment));
        Call<DisplayResponse> call = apiService.displayTicketDetails(request);

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
                        TicketData ticketData = TicketData.fromJson(decryptedJSON);



                        Utils.ticketCard.setVisibility(View.VISIBLE);
                        Utils.eventType.setText("Event Type: " + ticketData.getEventType());
                        Utils.ticketID.setText("Ticket ID: " + ticketData.getTicketId());
                        Utils.seatNumber.setText("Seat Number: " + ticketData.getSeatNo());
                        Utils.date.setText("Date of Event: " + ticketData.getDateOfEvent());

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
            Toast.makeText(this,"Invalid Response",LENGTH_SHORT).show();
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
                    String decryptedIndex = decrpytIndex(indexBytes);
                    Log.d("Decrtypted Index" , decryptedIndex);
                    sendIndextoServer(decryptedIndex);
                }
            }
            else if(response[1] == (byte) 0x40){
                if(response[5] == (byte) 0x40 && response[6] == (byte) 0x40){
                    popUpAccessDenied();
                }
                else{
                    byte[] framgentRequested = new byte[3];
                    System.arraycopy(response,5,framgentRequested,0,3);
                    byte[] encryptedIDIndex = new byte[response.length - 10];
                    System.arraycopy(response, 8, encryptedIDIndex , 0 , response.length-10);
                    Log.d("FragmentRequested" , Utils.toHex(framgentRequested));
                    Log.d("EncytpedRequest" , Arrays.toString(encryptedIDIndex));
                    String decryptedIndex = decrpytIndex(encryptedIDIndex);
                    if(toHex(framgentRequested).equals("482730")){
                        sendIDCrossIndextoServer(decryptedIndex);
                    } else if (toHex(framgentRequested).equals("915460")) {
                        sendHealthDataCrossIndextoServer(decryptedIndex);
                    } else if (toHex(framgentRequested).equals("307210")) {
                        sendTicketDataCrossIndextoServer(decryptedIndex);
                    }

                }

            }
            else {
                Log.d("Unknown Ins bytes", "Command executed successfully");
            }
        } else {
           Log.e("Error", "SW=" + String.format("%02X%02X", sw1, sw2));
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
