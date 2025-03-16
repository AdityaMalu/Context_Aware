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
            Log.d("ACCESS", "Granted");
            return true;
        } else if (request_type.equals("emergency") && emergency_level.equals("critical") && patient_status.equals("unconscious")) {
            Log.d("ACCESS", "Granted");
            return true;
        } else if (request_type.equals("emergency") && user_consent.equals("true") && hospital_authentication.equals("verified_hospital")) {
            Log.d("ACCESS", "Granted");
            return true;
        } else {
            Log.d("ACCESS", "Denied");
            return false;
        }
    }

    public static boolean ticketIdentity(byte[] contextVariables) {
        Log.d("CONTEXT VAR", toHex(contextVariables));
        List<String> ticketPolicy = ticket_identity.get("ticket_policy");
        assert ticketPolicy != null;
        String ticket_policy = ticketPolicy.get(contextVariables[0]);
        Log.d("TICKET POLICY", ticket_policy);
        List<String> eventType = ticket_identity.get("event_type");
        assert eventType != null;
        String event_type = eventType.get(contextVariables[1]);
        Log.d("EVENT TYPE", event_type);
        List<String> securityLevel = ticket_identity.get("security_level");
        assert securityLevel != null;
        String security_level = securityLevel.get(contextVariables[2]);
        Log.d("SECURITY LEVEL", security_level);
        List<String> ticketStatus = ticket_identity.get("ticket_status");
        assert ticketStatus != null;
        String ticket_status = ticketStatus.get(contextVariables[4]);
        Log.d("TICKET STATUS", ticket_status);
        List<String> crowdDensity = ticket_identity.get("crowd_density");
        assert crowdDensity != null;
        String crowd_density = crowdDensity.get(contextVariables[4]);
        Log.d("CROWD DENSITY", crowd_density);
        List<String> userConsent = ticket_identity.get("user_consent");
        assert userConsent != null;
        String user_consent = userConsent.get(contextVariables[5]);
        Log.d("USER CONSENT", user_consent);

        if (ticket_policy.equals("strict_id_verification") && ticket_status.equals("valid") && security_level.equals("high")) {
            Log.d("ACCESS", "Granted");
            return true;
        } else if (ticket_policy.equals("strict_id_verification") && ticket_status.equals("valid") && crowd_density.equals("high")) {
            Log.d("ACCESS", "Granted");
            return true;
        } else if (ticket_policy.equals("strict_id_verification") && ticket_status.equals("valid") && event_type.equals("private_event")) {
            Log.d("ACCESS", "Granted");
            return true;
        } else if (user_consent.equals("true") && ticket_status.equals("valid") && security_level.equals("high")) {
            Log.d("ACCESS", "Granted");
            return true;
        } else {
            Log.d("ACCESS", "Denied");
            return false;
        }
    }

    public static boolean ticketHealthcare(byte[] contextVariables) {
        Log.d("CONTEXT VAR", toHex(contextVariables));
        List<String> eventType = ticket_healthcare.get("event_type");
        assert eventType != null;
        String event_type = eventType.get(contextVariables[0]);
        Log.d("EVENT TYPE", event_type);
        List<String> healthRiskLevel = ticket_healthcare.get("health_risk_level");
        assert healthRiskLevel != null;
        String health_risk_level = healthRiskLevel.get(contextVariables[1]);
        Log.d("HEALTH RISK LEVEL", health_risk_level);
        List<String> eventPolicy = ticket_healthcare.get("event_policy");
        assert eventPolicy != null;
        String event_policy = eventPolicy.get(contextVariables[2]);
        Log.d("EVENT POLICY", event_policy);
        List<String> governmentHealthAlert = ticket_healthcare.get("government_health_alert");
        assert governmentHealthAlert != null;
        String government_health_alert = governmentHealthAlert.get(contextVariables[4]);
        Log.d("GOVERNMENT HEALTH ALERT", government_health_alert);
        List<String> ticketStatus = ticket_healthcare.get("ticket_status");
        assert ticketStatus != null;
        String ticket_status = ticketStatus.get(contextVariables[4]);
        Log.d("TICKET STATUS", ticket_status);
        List<String> userConsent = ticket_healthcare.get("user_consent");
        assert userConsent != null;
        String user_consent = userConsent.get(contextVariables[5]);
        Log.d("USER CONSENT", user_consent);

        if (event_policy.equals("strict_check") && health_risk_level.equals("high") && government_health_alert.equals("active_alert")) {
            Log.d("ACCESS", "Granted");
            return true;
        } else if (event_policy.equals("strict_check") && health_risk_level.equals("medium") && user_consent.equals("true")) {
            Log.d("ACCESS", "Granted");
            return true;
        } else if (government_health_alert.equals("active_alert") && health_risk_level.equals("high") && ticket_status.equals("valid")) {
            Log.d("ACCESS", "Granted");
            return true;
        } else {
            Log.d("ACCESS", "Denied");
            return false;
        }
    }

    public static boolean healthcareTicket(byte[] contextVariables) {
        Log.d("CONTEXT VAR", toHex(contextVariables));
        List<String> contactTracingLevel = healthcare_ticket.get("contact_tracing_level");
        assert contactTracingLevel != null;
        String contact_tracing_level = contactTracingLevel.get(contextVariables[0]);
        Log.d("CONTACT TRACING LEVEL", contact_tracing_level);
        List<String> governmentHealthAlert = healthcare_ticket.get("government_health_alert");
        assert governmentHealthAlert != null;
        String government_health_alert = governmentHealthAlert.get(contextVariables[1]);
        Log.d("GOVERNMENT HEALTH ALERT", government_health_alert);
        List<String> hospitalAuthentication = healthcare_ticket.get("hospital_authentication");
        assert hospitalAuthentication != null;
        String hospital_authentication = hospitalAuthentication.get(contextVariables[2]);
        Log.d("HOSPITAL AUTHENTICATION", hospital_authentication);
        List<String> timeSinceEvent = healthcare_ticket.get("time_since_event");
        assert timeSinceEvent != null;
        String time_since_event = timeSinceEvent.get(contextVariables[3]);
        Log.d("TICKET STATUS", time_since_event);
        List<String> userConsent = healthcare_ticket.get("user_consent");
        assert userConsent != null;
        String user_consent = userConsent.get(contextVariables[4]);
        Log.d("USER CONSENT", user_consent);

        if (contact_tracing_level.equals("high_risk") && government_health_alert.equals("active_alert") && hospital_authentication.equals("verified_hospital") && time_since_event.equals("recent (0-7 days)")) {
            Log.d("ACCESS", "Granted");
            return true;
        } else if (contact_tracing_level.equals("moderate_risk") && user_consent.equals("true") && hospital_authentication.equals("verified_hospital") && time_since_event.equals("recent (0-7 days)")) {
            Log.d("ACCESS", "Granted");
            return true;
        } else if (government_health_alert.equals("active_alert") && hospital_authentication.equals("verified_hospital") && time_since_event.equals("medium (8-30 days)")) {
            Log.d("ACCESS", "Granted");
            return true;
        } else {
            Log.d("ACCESS", "Denied");
            return false;
        }
    }
}
