package com.krishilink.marketservice.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeoUtils {

    private static final Map<String, List<Double>> CITY_COORDINATES = new HashMap<>();

    static {
        CITY_COORDINATES.put("indore", List.of(75.8577, 22.7196));
        CITY_COORDINATES.put("dewas", List.of(76.0534, 22.9676));
        CITY_COORDINATES.put("ujjain", List.of(75.7885, 23.1765));
        CITY_COORDINATES.put("bhopal", List.of(77.4126, 23.2599));
        CITY_COORDINATES.put("sagar", List.of(78.7378, 23.8388));
        CITY_COORDINATES.put("vidisha", List.of(77.8146, 23.5236));
        CITY_COORDINATES.put("mandsaur", List.of(75.0693, 24.0734));
        CITY_COORDINATES.put("neemuch", List.of(74.8720, 24.4764));
        CITY_COORDINATES.put("ratlam", List.of(75.0376, 23.3315));
        CITY_COORDINATES.put("khandwa", List.of(76.3498, 21.8247));
        CITY_COORDINATES.put("khurai", List.of(78.3262, 24.0450));
        CITY_COORDINATES.put("bina", List.of(78.1812, 24.1750));
        CITY_COORDINATES.put("sehore", List.of(77.0850, 23.2030));
        CITY_COORDINATES.put("jabalpur", List.of(79.9864, 23.1815));
        CITY_COORDINATES.put("gwalior", List.of(78.1828, 26.2183));
    }

    public static List<Double> resolveCoordinates(String district, String state, String location) {
        String[] candidates = {location, district, state};
        for (String c : candidates) {
            if (c != null) {
                String key = c.trim().toLowerCase();
                if (CITY_COORDINATES.containsKey(key)) {
                    return CITY_COORDINATES.get(key);
                }
                for (Map.Entry<String, List<Double>> entry : CITY_COORDINATES.entrySet()) {
                    if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                        return entry.getValue();
                    }
                }
            }
        }
        return List.of(75.8577, 22.7196); // Default Indore
    }

    public static double calculateDistanceKm(List<Double> coord1, List<Double> coord2) {
        if (coord1 == null || coord2 == null || coord1.size() < 2 || coord2.size() < 2) {
            return 25.0;
        }
        double lon1 = coord1.get(0);
        double lat1 = coord1.get(1);
        double lon2 = coord2.get(0);
        double lat2 = coord2.get(1);

        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(earthRadiusKm * c * 10.0) / 10.0;
    }
}
