package com.example.reader;

import static com.example.reader.Utils.concatenateArrays;
import static com.example.reader.Utils.deriveAESKey;
import static com.example.reader.Utils.encryptAES;
import static com.example.reader.Utils.hexStringToByteArray;

import android.app.AlertDialog;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;

import java.security.KeyPair;

import javax.crypto.SecretKey;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IDFragment extends Fragment{
    String jsonString;
    String index;

    private APDUCommandListner apduCommandListner;

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

        Button createIdButton = view.findViewById(R.id.btn_create_id);
        createIdButton.setOnClickListener(v -> showCreateIdDialog());

        return view;
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
                    byte[] seedBytes = Utils.hexStringToByteArray(seed);
                    KeyPair keyPair = Utils.generateKeyPairFromSeed(seedBytes);
                    SecretKey aesKey = deriveAESKey(keyPair.getPrivate(), keyPair.getPublic());
                    String encryptedJson = encryptAES(jsonString, aesKey);
                    Toast.makeText(getActivity(), "Encrypted JSON: " + encryptedJson, Toast.LENGTH_LONG).show();
                    sendIDtoServer(index, encryptedJson, Utils.get(MainActivity.activeFragment));
                    byte[] command = new byte[]{(byte) 0x80, 0x30, 0x00, 0x00, 0x00};
                    byte[] data = concatenateArrays(hexStringToByteArray(fragmentValue), hexStringToByteArray(index));
                    command = concatenateArrays(command,data);
                    apduCommandListner.sendApduCommand(command);
                    if(MainActivity.isHCEConnected){

                    }
                    else{
                        Log.d("HCE Card" , "Card is Disconncted");
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

