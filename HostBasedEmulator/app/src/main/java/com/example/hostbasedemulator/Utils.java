package com.example.hostbasedemulator;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Utils {
    private static final String HEX_CHARS = "0123456789ABCDEF";
    private static final char[] HEX_CHARS_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String IDENTITY_APP_ID = "48273";

    public static String HEALTHCARE_APP_ID = "91546";

    public static String TICKETING_APP_ID="30721";

    public static final String PREF_NAME = "MyAppPrefs";

    public static String ISSUER_PUBLIC_KEY =  "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAl3ZZVLaR2VktMe+CjFyV6Bdm2RmBXWIgZa89cGYUUD18mHGxQnpCbHd5DumpAwDrSXSpsRvapMtTbgwg1pzmKHZaEqAzvnHD33Gk7zJViA31bcgJcbbEgQ+xIji8HCieqCnXg3ldI6Oq/3KVYFWZcfr/4eUANlRWvcldvIiMC2buRkb6gdiui+GMsTzq42rf+yhNhIdMGFIr7+0E79i5c8X0eF2wn4zsKFnWyUiUyyHwyYaT0U9Nl8MfBcRHTCavYqdLjYzgvO2roVlQR9iI0wXiPxFeRQqUUjW4HclzpzjLHnbesT70eBQwvpI779uHZlLn5XHRCy+GLCcPO01IqwIDAQAB";


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
