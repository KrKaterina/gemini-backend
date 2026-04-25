package com.platform.accident.enrichment.util;

public class GeoHashUtils {
    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

    public static String encode(double lat, double lon) {
        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};
        StringBuilder hash = new StringBuilder();
        int bit = 0;
        int val = 0;
        boolean isEven = true;

        while (hash.length() < 7) { // 7-char precision
            double mid;
            if (isEven) {
                mid = (lonRange[0] + lonRange[1]) / 2;
                if (lon > mid) { val = (val << 1) | 1; lonRange[0] = mid; }
                else { val = (val << 1) | 0; lonRange[1] = mid; }
            } else {
                mid = (latRange[0] + latRange[1]) / 2;
                if (lat > mid) { val = (val << 1) | 1; latRange[0] = mid; }
                else { val = (val << 1) | 0; latRange[1] = mid; }
            }
            isEven = !isEven;
            if (bit < 4) { bit++; }
            else { hash.append(BASE32.charAt(val)); bit = 0; val = 0; }
        }
        return hash.toString();
    }
}