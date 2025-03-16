package com.example.hostbasedemulator;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    private static final String HEX_CHARS = "0123456789ABCDEF";
    private static final char[] HEX_CHARS_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String IDENTITY_APP_ID = "482730";
    public static String HEALTHCARE_APP_ID = "915460";
    public static String TICKETING_APP_ID="307210";
    public static final HashMap<String, String> aesKeys;
    static {
        aesKeys = new HashMap<>();
        aesKeys.put(IDENTITY_APP_ID, "QXNNSmdEY0duRGFMZ05SbFFHc0oxRmdqS3MyVnF1TXc=");
        aesKeys.put(HEALTHCARE_APP_ID, "anp0bmNXbW92UVZnTDRwREdpTFA0ZDBnR214cmppZ0I=");
        aesKeys.put(TICKETING_APP_ID, "bWJNM1hmeXZkZTlhMVVDNGRuZTRLTDBRYlFpRWhFZ3U=");
    }
    public static final String PREF_NAME = "MyAppPrefs";
    public static String ISSUER_PUBLIC_KEY =  "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAl3ZZVLaR2VktMe+CjFyV6Bdm2RmBXWIgZa89cGYUUD18mHGxQnpCbHd5DumpAwDrSXSpsRvapMtTbgwg1pzmKHZaEqAzvnHD33Gk7zJViA31bcgJcbbEgQ+xIji8HCieqCnXg3ldI6Oq/3KVYFWZcfr/4eUANlRWvcldvIiMC2buRkb6gdiui+GMsTzq42rf+yhNhIdMGFIr7+0E79i5c8X0eF2wn4zsKFnWyUiUyyHwyYaT0U9Nl8MfBcRHTCavYqdLjYzgvO2roVlQR9iI0wXiPxFeRQqUUjW4HclzpzjLHnbesT70eBQwvpI779uHZlLn5XHRCy+GLCcPO01IqwIDAQAB";

    public static final Map<String, List<String>> healthcare_identity = new LinkedHashMap<>();
    static {
        healthcare_identity.put("request_type", new ArrayList<>(Arrays.asList("emergency", "checkup", "insurance_verification")));
        healthcare_identity.put("emergency_level", new ArrayList<>(Arrays.asList("critical", "moderate", "non_urgent")));
        healthcare_identity.put("hospital_authentication", new ArrayList<>(Arrays.asList("verified_hospital", "unverified_hospital"
        )));
        healthcare_identity.put("time_of_request", new ArrayList<>(Arrays.asList("day", "night")));
        healthcare_identity.put("patient_status", new ArrayList<>(Arrays.asList("conscious", "unconscious")));
        healthcare_identity.put("user_consent", new ArrayList<>(Arrays.asList("false", "true")));
    }

    public static final Map<String, List<String>> ticket_identity = new LinkedHashMap<>();
    static {
        ticket_identity.put("ticket_policy", new ArrayList<>(Arrays.asList("strict_id_verification", "standard_verification")));
        ticket_identity.put("event_type", new ArrayList<>(Arrays.asList("concert", "conference", "sports_match", "private_event")));
        ticket_identity.put("security_level", new ArrayList<>(Arrays.asList("high", "medium", "low")));
        ticket_identity.put("ticket_status", new ArrayList<>(Arrays.asList("valid", "expired", "fraudulent")));
        ticket_identity.put("crowd_density", new ArrayList<>(Arrays.asList("low", "medium", "high")));
        ticket_identity.put("user_consent", new ArrayList<>(Arrays.asList("false", "true")));
    }

    public static final Map<String, List<String>> ticket_healthcare = new LinkedHashMap<>();
    static {
        ticket_healthcare.put("event_type", new ArrayList<>(Arrays.asList("concert", "conference", "sports_match")));
        ticket_healthcare.put("health_risk_level", new ArrayList<>(Arrays.asList("low", "medium", "high")));
        ticket_healthcare.put("event_policy", new ArrayList<>(Arrays.asList("strict_check", "random_check", "no_check")));
        ticket_healthcare.put("government_health_alert", new ArrayList<>(Arrays.asList("active_alert", "no_alert")));
        ticket_healthcare.put("ticket_status", new ArrayList<>(Arrays.asList("valid", "expired")));
        ticket_healthcare.put("user_consent", new ArrayList<>(Arrays.asList("false", "true")));
    }

    public static final Map<String, List<String>> healthcare_ticket = new LinkedHashMap<>();
    static {
        healthcare_ticket.put("contact_tracing_level", new ArrayList<>(Arrays.asList("high_risk", "moderate_risk", "low_risk")));
        healthcare_ticket.put("government_health_alert", new ArrayList<>(Arrays.asList("active_alert", "no_alert")));
        healthcare_ticket.put("hospital_authentication", new ArrayList<>(Arrays.asList("verified_hospital", "unverified_hospital")));
        healthcare_ticket.put("time_since_event", new ArrayList<>(Arrays.asList("recent (0-7 days)", "medium (8-30 days)", "old (30+ days)")));
        healthcare_ticket.put("user_consent", new ArrayList<>(Arrays.asList("false", "true")));
    }
    public static byte[] hexStringToByteArray(String data) {
        byte[] result = new byte[data.length() / 2];

        for (int i = 0; i < data.length(); i += 2) {
            int firstIndex = HEX_CHARS.indexOf(data.charAt(i));
            int secondIndex = HEX_CHARS.indexOf(data.charAt(i + 1));

            int octet = (firstIndex << 4) | secondIndex;
            result[i / 2] = (byte) octet;
        }

        return result;
    }

    public static PublicKey getIssuerPublicKey() throws Exception{
        byte[] issuerPublicKeyBytes = Base64.getDecoder().decode(ISSUER_PUBLIC_KEY);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(issuerPublicKeyBytes);
        return keyFactory.generatePublic(publicKeySpec);
    }


    public static String toHex(byte[] byteArray) {
        StringBuilder result = new StringBuilder();

        for (byte b : byteArray) {
            int octet = b & 0xFF;
            int firstIndex = (octet & 0xF0) >> 4;
            int secondIndex = octet & 0x0F;
            result.append(HEX_CHARS_ARRAY[firstIndex]);
            result.append(HEX_CHARS_ARRAY[secondIndex]);
        }

        return result.toString();
    }

}
