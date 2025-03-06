package com.example.reader;


import static com.example.reader.Utils.concatenateArrays;

import static com.example.reader.Utils.encrypt;
import static com.example.reader.Utils.generateKey;
import static com.example.reader.Utils.hexStringToByteArray;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IDFragment extends Fragment{
    String jsonString;
    String index;
    Button createIdButton;
    private APDUCommandListner apduCommandListner;
    private final Handler handler = new Handler();

    public IDFragment(APDUCommandListner apduCommandListner){
        this.apduCommandListner = apduCommandListner;

    }

    ApiService apiService = RetrofitClient.getApiService();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_id, container, false);

        TextView textView = view.findViewById(R.id.text_fragment);
        textView.setText("ID Fragment");

        Utils.nameId = view.findViewById(R.id.text_name);
        Utils.dob = view.findViewById(R.id.text_dob);
        Utils.idNumber = view.findViewById(R.id.text_id_number);
        Utils.idCard = view.findViewById(R.id.card_id_info);
        createIdButton = view.findViewById(R.id.btn_create_id);
        createIdButton.setOnClickListener(v -> showCreateIdDialog());

        checkCardStatus();

        return view;
    }

    private final Runnable checkTagRunnable = new Runnable() {
        @Override
        public void run() {
            checkCardStatus();
            handler.postDelayed(this, 2000); // Check every 2 seconds
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        handler.postDelayed(checkTagRunnable, 1000); // Start checking
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(checkTagRunnable); // Stop checking when fragment is inactive
    }

    private void checkCardStatus() {
        String idNumber = Utils.idNumber.getText().toString().trim();
        // Check if the NFC card is connected using MainActivity's checkTag() method
        if (MainActivity.checkTag()) {
            createIdButton.setEnabled(true);
            createIdButton.setVisibility(View.VISIBLE);
//            Log.d("IDFragment", "Card is connected. Enabling Create ID button.");
        }
        if (!idNumber.isEmpty()){
            createIdButton.setEnabled(false);
            createIdButton.setVisibility(View.INVISIBLE);
//            Log.d("IDFragment", "Card is disconnected. Disabling Create ID button.");
        }
    }


    private void showCreateIdDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_create_id, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.edit_name);
        EditText editDob = dialogView.findViewById(R.id.edit_dob);
        EditText editIdNumber = dialogView.findViewById(R.id.edit_id_number);

        builder.setPositiveButton("Create", (dialog, which) -> {
            if (!MainActivity.checkTag()) {
                Toast.makeText(getActivity(), "Error: NFC Card Not Connected!", Toast.LENGTH_SHORT).show();
                return;
            }
            String name = editName.getText().toString().trim();
            String dob = editDob.getText().toString().trim();
            String idNumber = editIdNumber.getText().toString().trim();

            if (name.isEmpty() || dob.isEmpty() || idNumber.isEmpty()) {
                Toast.makeText(getActivity(), "All fields are required!", Toast.LENGTH_SHORT).show();
            } else {
                try{
                    IDData idData = new IDData(name, dob, idNumber);
                    jsonString = idData.toJson();
                    Toast.makeText(getActivity(), "JSON: " + jsonString, Toast.LENGTH_LONG).show();
                    index = Utils.createIndex();
                    String fragmentValue = Utils.get(MainActivity.activeFragment);


                    if (fragmentValue == null || fragmentValue.isEmpty()) {
                        Toast.makeText(getActivity(), "Error: Seed (Active Fragment) is missing!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String seed = fragmentValue + index;
                    Log.d("Seed" , seed);
                    byte[] seedBytes = Utils.hexStringToByteArray(seed);
                    SecretKey key = generateKey(seed);
                    String encryptedJson = encrypt(jsonString, key);
                    Toast.makeText(getActivity(), "Encrypted JSON: " + encryptedJson, Toast.LENGTH_LONG).show();

                    Log.d("Server", "Server is avaiable");
                    if (MainActivity.checkTag()){
                        sendIDtoServer(index, encryptedJson, Utils.get(MainActivity.activeFragment));
                        Log.d("HCEDevice" , "HCEDevice Connectd");
                        byte[] command = new byte[]{(byte) 0x80, 0x30, 0x00, 0x00, 0x00};
                        byte[] data = concatenateArrays(Utils.hexStringToByteArray(fragmentValue), Utils.hexStringToByteArray(index));
                        command = concatenateArrays(command, data);
                        // Send APDU command
                        apduCommandListner.sendApduCommand(command);
                    }
                    else{
                        Log.d("HCE Card", "Card is Disconnected");
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "Encryption Error", Toast.LENGTH_SHORT).show();

                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }




    private void sendIDtoServer(String index, String encryptedData, String appID){
            CreateRequest request = new CreateRequest(index,encryptedData,appID);
        Call<CreateResponse> call = apiService.createID(request);
        call.enqueue(new Callback<CreateResponse>() {
            @Override
            public void onResponse(Call<CreateResponse> call, Response<CreateResponse> response) {
                if(response.isSuccessful()){
                    CreateResponse incoming = response.body();
                    assert incoming != null;
                    Log.d("Create ID" , incoming.getMessage());

                }
                else{
                    Log.e("Request failed", "Response code: " + response.code());

                }
            }

            @Override
            public void onFailure(Call<CreateResponse> call, Throwable t) {
                Log.e("Request failed", "Response code: " + t);

            }
        });
    }

}

