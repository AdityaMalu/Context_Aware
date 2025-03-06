package com.example.reader;

import static com.example.reader.Utils.concatenateArrays;
import static com.example.reader.Utils.encrypt;
import static com.example.reader.Utils.generateKey;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import javax.crypto.SecretKey;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HealthCareFragment extends Fragment {

    String jsonString;
    String index;
    Button createIdButton, getIdButton, contactTracingButton;
    private APDUCommandListner apduCommandListner;
    private final Handler handler = new Handler();

    public HealthCareFragment(APDUCommandListner apduCommandListner){
        this.apduCommandListner = apduCommandListner;

    }

    ApiService apiService = RetrofitClient.getApiService();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_healthcare, container, false);
        TextView textView = view.findViewById(R.id.text_fragment);
        textView.setText("HealthCare Fragment");

        Utils.bloodType = view.findViewById(R.id.text_blood_type);
        Utils.allergy = view.findViewById(R.id.text_allergy);
        Utils.vaccination = view.findViewById(R.id.text_vaccination);
        Utils.medicalHistory = view.findViewById(R.id.text_medical_history);

        Utils.healthCard = view.findViewById(R.id.card_health_info);

        createIdButton = view.findViewById(R.id.btn_create_id);
        createIdButton.setOnClickListener(v -> showCreateIdDialog());

        getIdButton = view.findViewById(R.id.btn_get_id);
        getIdButton.setOnClickListener(v -> showGetIdDialog());

        contactTracingButton = view.findViewById(R.id.btn_contact_tracing);
        contactTracingButton.setOnClickListener(v -> showContactTracing());


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
        String bloodType = Utils.bloodType.getText().toString().trim();
        // Check if the NFC card is connected using MainActivity's checkTag() method
        if (MainActivity.checkTag()) {
            createIdButton.setEnabled(true);
            getIdButton.setEnabled(true);
            contactTracingButton.setEnabled(true);
            createIdButton.setVisibility(View.VISIBLE);
            getIdButton.setVisibility(View.VISIBLE);
            contactTracingButton.setVisibility(View.VISIBLE);
//            Log.d("IDFragment", "Card is connected. Enabling Create ID button.");
        }if(!bloodType.isEmpty()) {
            createIdButton.setEnabled(false);
            getIdButton.setEnabled(false);
            contactTracingButton.setEnabled(false);
            createIdButton.setVisibility(View.INVISIBLE);
            getIdButton.setVisibility(View.INVISIBLE);
            contactTracingButton.setVisibility(View.INVISIBLE);
//            Log.d("IDFragment", "Card is disconnected. Disabling Create ID button.");
        }
    }

    private void showCreateIdDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_create_healthcare_id, null);
        builder.setView(dialogView);

        EditText bloodType = dialogView.findViewById(R.id.edit_blood_type);
        EditText allergy = dialogView.findViewById(R.id.edit_allergy);
        EditText vaccination  = dialogView.findViewById(R.id.edit_vaccination);
        EditText medicalHistory = dialogView.findViewById(R.id.edit_medical_history);

        builder.setPositiveButton("Create", (dialog, which) -> {
            if (!MainActivity.checkTag()) {
                Toast.makeText(getActivity(), "Error: NFC Card Not Connected!", Toast.LENGTH_SHORT).show();
                return;
            }
            String bloodTypeText = bloodType.getText().toString().trim();
            String allergyText = allergy.getText().toString().trim();
            String vaccinationText = vaccination.getText().toString().trim();
            String medicalHistroyText = medicalHistory.getText().toString().trim();

            if (bloodTypeText.isEmpty()) {
                Toast.makeText(getActivity(), "AtLeast BloodType is Required", Toast.LENGTH_SHORT).show();
            } else {
                try{
                    HealthCareData healthCareData = new HealthCareData(bloodTypeText, allergyText, vaccinationText, medicalHistroyText);
                    jsonString = healthCareData.toJson();
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


    private void showGetIdDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.getid_healthcare_layout, null);
        builder.setView(popupView);

// Get references to the views in the popup
        Spinner spinnerHospitalRequestType = popupView.findViewById(R.id.spinner_hospital_request_type);
        Switch switchUserConsent = popupView.findViewById(R.id.switch_user_consent);
        Spinner spinnerEmergencyLevel = popupView.findViewById(R.id.spinner_emergency_level);
        Spinner spinnerHospitalAuthentication = popupView.findViewById(R.id.spinner_hospital_authentication);
        Switch switchTimeOfRequest = popupView.findViewById(R.id.switch_time_of_request);
        Switch switchPatientStatus = popupView.findViewById(R.id.switch_patient_status);

        builder.setPositiveButton("Request", (dialog, which) -> {
            if (!MainActivity.checkTag()) {
                Toast.makeText(getActivity(), "Error: NFC Card Not Connected!", Toast.LENGTH_SHORT).show();
                return;
            }

            String hospitalRequestType = spinnerHospitalRequestType.getSelectedItem().toString();
            boolean userConsent = switchUserConsent.isChecked();
            String emergencyLevel = spinnerEmergencyLevel.getSelectedItem().toString();
            String hospitalAuthentication = spinnerHospitalAuthentication.getSelectedItem().toString();
            boolean timeOfRequest = switchTimeOfRequest.isChecked();
            boolean patientStatus = switchPatientStatus.isChecked();

            // Log the details
            Log.d("HospitalRequest", "Hospital Request Type: " + hospitalRequestType);
            Log.d("HospitalRequest", "User Consent: " + userConsent);
            Log.d("HospitalRequest", "Emergency Level: " + emergencyLevel);
            Log.d("HospitalRequest", "Hospital Authentication: " + hospitalAuthentication);
            Log.d("HospitalRequest", "Time of Request (Day=true/Night=false): " + timeOfRequest);
            Log.d("HospitalRequest", "Patient Status (Conscious=true/Unconscious=false): " + patientStatus);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void showContactTracing(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.contact_tracing, null);
        builder.setView(popupView);

// Get references to the views in the popup
        Spinner spinnerContactTracingLevel = popupView.findViewById(R.id.spinner_contact_tracing_level);
        Spinner spinnerGovernmentHealthAlert = popupView.findViewById(R.id.spinner_government_health_alert);
        Spinner spinnerHospitalAuthentication = popupView.findViewById(R.id.spinner_hospital_authentication);
        Switch switchUserConsent = popupView.findViewById(R.id.switch_user_consent);
        Spinner spinnerTimeSinceEvent = popupView.findViewById(R.id.spinner_time_since_event);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            if (!MainActivity.checkTag()) {
                Toast.makeText(getActivity(), "Error: NFC Card Not Connected!", Toast.LENGTH_SHORT).show();
                return;
            }

            String contactTracingLevel = spinnerContactTracingLevel.getSelectedItem().toString();
            String governmentHealthAlert = spinnerGovernmentHealthAlert.getSelectedItem().toString();
            String hospitalAuthentication = spinnerHospitalAuthentication.getSelectedItem().toString();
            boolean userConsent = switchUserConsent.isChecked();
            String timeSinceEvent = spinnerTimeSinceEvent.getSelectedItem().toString();

            // Log the details
            Log.d("ContactTracing", "Contact Tracing Level: " + contactTracingLevel);
            Log.d("ContactTracing", "Government Health Alert: " + governmentHealthAlert);
            Log.d("ContactTracing", "Hospital Authentication: " + hospitalAuthentication);
            Log.d("ContactTracing", "User Consent: " + userConsent);
            Log.d("ContactTracing", "Time Since Event: " + timeSinceEvent);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void sendIDtoServer(String index, String encryptedData, String appID){
        CreateRequest request = new CreateRequest(index,encryptedData,appID);
        Call<CreateResponse> call = apiService.createHealthCareID(request);
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

