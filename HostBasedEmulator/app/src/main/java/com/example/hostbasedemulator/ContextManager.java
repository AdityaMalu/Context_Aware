package com.example.hostbasedemulator;

import static com.example.hostbasedemulator.Utils.*;

import android.util.Log;

import java.util.List;

public class ContextManager {

    public static boolean healthcareIdentity(byte[] contextVariables) {
        Log.d("CONTEXT VAR", toHex(contextVariables));
        List<String> requestType = healthcare_identity.get("request_type");
        assert requestType != null;
        String request_type = requestType.get(contextVariables[0]);
        Log.d("REQUEST_TYPE", request_type);
        List<String> emergencyLevel = healthcare_identity.get("emergency_level");
        assert emergencyLevel != null;
        String emergency_level = emergencyLevel.get(contextVariables[1]);
        Log.d("EMERGENCY LEVEL", emergency_level);
        List<String> hospitalAuthentication = healthcare_identity.get("hospital_authentication");
        assert hospitalAuthentication != null;
        String hospital_authentication = hospitalAuthentication.get(contextVariables[2]);
        Log.d("HOSP AUTH", hospital_authentication);
        List<String> patientStatus = healthcare_identity.get("patient_status");
        assert patientStatus != null;
        String patient_status = patientStatus.get(contextVariables[4]);
        Log.d("PATIENT STAT", patient_status);
        List<String> userConsent = healthcare_identity.get("user_consent");
        assert userConsent != null;
        String user_consent = userConsent.get(contextVariables[5]);
        Log.d("USER CONSENT", user_consent);

        if (request_type.equals("emergency") && emergency_level.equals("critical") && hospital_authentication.equals("verified_hospital")) {
            return true;
        } else if (request_type.equals("emergency") && emergency_level.equals("critical") && patient_status.equals("unconscious")) {
            return true;
        } else if (request_type.equals("emergency") && user_consent.equals("true") && hospital_authentication.equals("verified_hospital")) {
            return true;
        } else {
            return false;
        }
    }
}
