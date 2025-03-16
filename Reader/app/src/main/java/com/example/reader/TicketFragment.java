package com.example.reader;

import static com.example.reader.Utils.HEALTHCARE_APP_ID;
import static com.example.reader.Utils.IDENTITY_APP_ID;
import static com.example.reader.Utils.TICKETING_APP_ID;
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

import java.util.Arrays;

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

        Utils.eventType = view.findViewById(R.id.text_event_type);
        Utils.ticketID = view.findViewById(R.id.text_ticket_id);
        Utils.seatNumber = view.findViewById(R.id.text_seat_number);
        Utils.date = view.findViewById(R.id.text_date);
        Utils.ticketCard = view.findViewById(R.id.card_ticket_info);

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
        EditText ticketID = dialogView.findViewById(R.id.edit_ticket_id);
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


            // Get selected Ticket Policy from RadioGroup
            String ticketPolicy = ((RadioButton) popupView.findViewById(radioGroupTicketPolicy.getCheckedRadioButtonId())).getText().toString();
            int ticketPolicyIndex = 0;
            int selectedRadioId = radioGroupTicketPolicy.getCheckedRadioButtonId();
            if (selectedRadioId != -1) { // Ensure a button is selected
                RadioButton selectedRadioButton = popupView.findViewById(selectedRadioId);
                String ticketPolicytext = selectedRadioButton.getText().toString();

                // Set index based on policy selection
                if ("Strict ID Verification".equalsIgnoreCase(ticketPolicytext)) {
                    ticketPolicyIndex = 1; // Strict policy
                }
            }


            // Get user consent switch state
            boolean userConsent = switchUserConsent.isChecked();
            int userConsentIndex = userConsent ? 1 : 0;

            // Get selected Event Type
            String eventType = spinnerEventType.getSelectedItem().toString();
            String[] eventTypes = getResources().getStringArray(R.array.event_types);
            int eventTypeIndex = Arrays.asList(eventTypes).indexOf(eventType);

            // Get selected Security Level
            String securityLevel = spinnerSecurityLevel.getSelectedItem().toString();
            String[] securityLevels = getResources().getStringArray(R.array.security_levels);
            int securityLevelIndex = Arrays.asList(securityLevels).indexOf(securityLevel);

            // Get selected Ticket Status
            String ticketStatus = spinnerTicketStatus.getSelectedItem().toString();
            String[] ticketStatuses = getResources().getStringArray(R.array.ticket_statuses);
            int ticketStatusIndex = Arrays.asList(ticketStatuses).indexOf(ticketStatus);

            // Get selected Crowd Density
            String crowdDensity = spinnerCrowdDensity.getSelectedItem().toString();
            String[] crowdDensities = getResources().getStringArray(R.array.crowd_densities);
            int crowdDensityIndex = Arrays.asList(crowdDensities).indexOf(crowdDensity);

            byte[] appID = Utils.concatenateArrays(Utils.hexStringToByteArray(TICKETING_APP_ID) , Utils.hexStringToByteArray(IDENTITY_APP_ID));
            byte[] getIDData = new byte[]{(byte) ticketPolicyIndex,(byte) eventTypeIndex,(byte) securityLevelIndex, (byte) ticketStatusIndex , (byte) crowdDensityIndex,(byte) userConsentIndex};
            byte[] command = new byte[]{ (byte) 0x80 , (byte) 0x40 , (byte) 0x00 , (byte) 0x00 , (byte) 0x08};

            command = Utils.concatenateArrays(command,appID);
            command = Utils.concatenateArrays(command,getIDData);

            apduCommandListner.sendApduCommand(command);


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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.vaccination_status, null);
        builder.setView(popupView);

        // Initialize UI elements
        Spinner spinnerEvent = popupView.findViewById(R.id.spinner_event_type);
        Spinner spinnerHealthRiskLevel = popupView.findViewById(R.id.spinner_health_risk_level);
        Spinner spinnerEventPolicy = popupView.findViewById(R.id.spinner_event_policy);
        Spinner spinnerGovernmentHealthAlert = popupView.findViewById(R.id.spinner_government_health_alert);
        Switch switchTicketStatus = popupView.findViewById(R.id.switch_ticket_status);
        Switch switchUserConsent = popupView.findViewById(R.id.switch_user_consent);

        builder.setPositiveButton("Get", (dialog, which) -> {
            if (!MainActivity.checkTag()) {
                Toast.makeText(getActivity(), "Error: NFC Card Not Connected!", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Retrieve values from UI elements
                String eventType = getSelectedItem(spinnerEvent, R.array.event_types);
                String healthRiskLevel = getSelectedItem(spinnerHealthRiskLevel, R.array.health_risk_levels);
                String eventPolicy = getSelectedItem(spinnerEventPolicy, R.array.event_policies);
                String governmentHealthAlert = getSelectedItem(spinnerGovernmentHealthAlert , R.array.government_health_alerts);
                boolean userConsent = switchUserConsent.isChecked();
                boolean vaccinationTicketStatus = switchTicketStatus.isChecked();

                // Validate selection
                if (eventType == null || healthRiskLevel == null || eventPolicy == null) {
                    Toast.makeText(getActivity(), "Please select all options", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Prepare data for APDU Command (if required)
                byte userConsentByte = (byte) (userConsent ? 1 : 0);
                byte ticketStatusByte = (byte) (vaccinationTicketStatus ? 1 : 0);

                byte[] vaccinationData = new byte[]{
                        (byte) getIndex(eventType, R.array.event_types),
                        (byte) getIndex(healthRiskLevel, R.array.health_risk_levels),
                        (byte) getIndex(eventPolicy, R.array.event_policies),
                        (byte) getIndex(governmentHealthAlert, R.array.government_health_alerts),
                        ticketStatusByte,
                        userConsentByte
                };

                // Example placeholder APDU command
                byte[] appID = Utils.concatenateArrays(Utils.hexStringToByteArray(TICKETING_APP_ID) , Utils.hexStringToByteArray(HEALTHCARE_APP_ID));
                byte[] command = new byte[]{(byte) 0x80, (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x08};

                command = concatenateArrays(command,appID);
                command = Utils.concatenateArrays(command, vaccinationData);

                // Send APDU Command (if applicable)
                apduCommandListner.sendApduCommand(command);

                // Log Selected Values
                Log.d("VaccinationStatus", "Event Type: " + eventType);
                Log.d("VaccinationStatus", "Health Risk Level: " + healthRiskLevel);
                Log.d("VaccinationStatus", "Event Policy: " + eventPolicy);
                Log.d("VaccinationStatus", "User Consent: " + userConsent);
                Log.d("VaccinationStatus", "Vaccination Ticket Status: " + vaccinationTicketStatus);

            } catch (Exception e) {
                Log.e("VaccinationPopupError", "Error processing vaccination popup: " + e.getMessage());
                Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private String getSelectedItem(Spinner spinner, int arrayResId) {
        String selectedItem = spinner.getSelectedItem().toString();
        String[] itemsArray = getResources().getStringArray(arrayResId);
        return Arrays.asList(itemsArray).contains(selectedItem) ? selectedItem : null;
    }


    private int getIndex(String selectedItem, int arrayResId) {
        String[] itemsArray = getResources().getStringArray(arrayResId);
        return Arrays.asList(itemsArray).indexOf(selectedItem);
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
