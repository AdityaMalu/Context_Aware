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

import javax.crypto.SecretKey;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketFragment extends Fragment {
    String jsonString;
    String index;
    Button createIdButton, getIdButton, vaccinationButton;
    private APDUCommandListner apduCommandListner;
    private final Handler handler = new Handler();

    public TicketFragment(APDUCommandListner apduCommandListner){
        this.apduCommandListner = apduCommandListner;

    }

    ApiService apiService = RetrofitClient.getApiService();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ticket, container, false);
        TextView textView = view.findViewById(R.id.text_fragment);
        textView.setText("Ticket Fragment");

        createIdButton = view.findViewById(R.id.btn_create_id);
        createIdButton.setOnClickListener(v -> showCreateIdDialog());

        getIdButton = view.findViewById(R.id.btn_get_id);
        getIdButton.setOnClickListener(v -> showGetIdDialog());

        vaccinationButton = view.findViewById(R.id.btn_vaccination_status);
        vaccinationButton.setOnClickListener(v -> showVaccinationDialog());

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
        // Check if the NFC card is connected using MainActivity's checkTag() method
        if (MainActivity.checkTag()) {
            createIdButton.setEnabled(true);
            getIdButton.setEnabled(true);
            vaccinationButton.setEnabled(true);
            createIdButton.setVisibility(View.VISIBLE);
            getIdButton.setVisibility(View.VISIBLE);
            vaccinationButton.setVisibility(View.VISIBLE);
//            Log.d("IDFragment", "Card is connected. Enabling Create ID button.");
        } else {
            createIdButton.setEnabled(false);
            getIdButton.setEnabled(false);
            vaccinationButton.setEnabled(false);
            createIdButton.setVisibility(View.GONE);
            getIdButton.setVisibility(View.GONE);
            vaccinationButton.setVisibility(View.GONE);
//            Log.d("IDFragment", "Card is disconnected. Disabling Create ID button.");
        }
    }

    private void showCreateIdDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.create_ticket, null);
        builder.setView(dialogView);

        EditText eventType = dialogView.findViewById(R.id.edit_event_type);
        EditText ticketID = dialogView.findViewById(R.id.edit_event_type);
        EditText seatNo  = dialogView.findViewById(R.id.edit_seat_no);
        EditText date = dialogView.findViewById(R.id.edit_date_of_event);

        builder.setPositiveButton("Create", (dialog, which) -> {
            if (!MainActivity.checkTag()) {
                Toast.makeText(getActivity(), "Error: NFC Card Not Connected!", Toast.LENGTH_SHORT).show();
                return;
            }
            String eventTypeText = eventType.getText().toString().trim();
            String ticketIdText = ticketID.getText().toString().trim();
            String seatNoText = seatNo.getText().toString().trim();
            String dateText = date.getText().toString().trim();

            if (eventTypeText.isEmpty() || ticketIdText.isEmpty() || seatNoText.isEmpty() || dateText.isEmpty()) {
                Toast.makeText(getActivity(), "All fields are Required", Toast.LENGTH_SHORT).show();
            } else {
                try{
                    TicketData healthCareData = new TicketData(eventTypeText, ticketIdText, seatNoText, dateText);
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
        View popupView = inflater.inflate(R.layout.get_ticket, null);
        builder.setView(popupView);

        // Get references to the views in the popup
        RadioGroup radioGroupTicketPolicy = popupView.findViewById(R.id.radio_group_ticket_policy);
        Switch switchUserConsent = popupView.findViewById(R.id.switch_user_consent);
        Spinner spinnerEventType = popupView.findViewById(R.id.spinner_event_type);
        Spinner spinnerSecurityLevel = popupView.findViewById(R.id.spinner_security_level);
        Spinner spinnerTicketStatus = popupView.findViewById(R.id.spinner_ticket_status);
        Spinner spinnerCrowdDensity = popupView.findViewById(R.id.spinner_crowd_density);

        AlertDialog alertDialog = builder.create();


        builder.setPositiveButton("Get", (dialog, which) -> {
            if (!MainActivity.checkTag()) {
                Toast.makeText(getActivity(), "Error: NFC Card Not Connected!", Toast.LENGTH_SHORT).show();
                return;
            }

            String ticketPolicy = ((RadioButton) popupView.findViewById(radioGroupTicketPolicy.getCheckedRadioButtonId())).getText().toString();
            boolean userConsent = switchUserConsent.isChecked();
            String eventType = spinnerEventType.getSelectedItem().toString();
            String securityLevel = spinnerSecurityLevel.getSelectedItem().toString();
            String ticketStatus = spinnerTicketStatus.getSelectedItem().toString();
            String crowdDensity = spinnerCrowdDensity.getSelectedItem().toString();

            // Log the details
            Log.d("TicketInfo", "Ticket Policy: " + ticketPolicy);
            Log.d("TicketInfo", "User Consent: " + userConsent);
            Log.d("TicketInfo", "Event Type: " + eventType);
            Log.d("TicketInfo", "Security Level: " + securityLevel);
            Log.d("TicketInfo", "Ticket Status: " + ticketStatus);
            Log.d("TicketInfo", "Crowd Density: " + crowdDensity);

        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void showVaccinationDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.vaccination_status, null);
        builder.setView(popupView);

        Spinner spinnerEvent = popupView.findViewById(R.id.spinner_event_type);
        Spinner spinnerHealthRiskLevel = popupView.findViewById(R.id.spinner_health_risk_level);
        Spinner spinnerEventPolicy = popupView.findViewById(R.id.spinner_event_policy);
        Switch switchTicketStatus = popupView.findViewById(R.id.switch_ticket_status);
        Switch switchUserConsent = popupView.findViewById(R.id.switch_user_consent);

        AlertDialog alertDialog = builder.create();

        builder.setPositiveButton("Get", (dialog, which) -> {
            if (!MainActivity.checkTag()) {
                Toast.makeText(getActivity(), "Error: NFC Card Not Connected!", Toast.LENGTH_SHORT).show();
                return;
            }


            String eventType = spinnerEvent.getSelectedItem().toString();
            String healthRiskLevel = spinnerHealthRiskLevel.getSelectedItem().toString();
            String eventPolicy = spinnerEventPolicy.getSelectedItem().toString();
            boolean userConsent = switchUserConsent.isChecked();
            boolean vaccinationTicketStatus = switchTicketStatus.isChecked();

            // Log the details
            Log.d("EventInfo", "Event Type: " + eventType);
            Log.d("EventInfo", "Health Risk Level: " + healthRiskLevel);
            Log.d("EventInfo", "Event Policy: " + eventPolicy);
            Log.d("EventInfo", "User Consent: " + userConsent);
            Log.d("EventInfo", "Vaccination Ticket Status: " + vaccinationTicketStatus);

        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();


    }

    private void sendIDtoServer(String index, String encryptedData, String appID){
        CreateRequest request = new CreateRequest(index,encryptedData,appID);
        Call<CreateResponse> call = apiService.createTicket(request);
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
