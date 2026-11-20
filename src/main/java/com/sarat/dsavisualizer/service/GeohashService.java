package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Geohash (Phase 22, how Google Maps indexes the world):
 * a latitude/longitude pair is turned into a short base32
 * string by interleaving the bits of the longitude and
 * latitude bisections, longitude first. Each bit halves one
 * range: bit 1 when the coordinate is in the upper half,
 * 0 when it is in the lower half. Every 5 bits emit one
 * base32 character (alphabet
 * {@code 0123456789bcdefghjkmnpqrstuvwxyz}). Longer hashes
 * are smaller cells — precision 6 is roughly a 1.2 km &times;
 * 0.6 km cell. The 8 neighbours are found by re-encoding
 * the adjacent cell centres at the same precision (never by
 * string manipulation at the edges — the same method works
 * for edge cells too).
 *
 * <p>Steps: one per bit (line 3 for a longitude bit, 4 for a
 * latitude bit), plus the final Step (line 0). A bit step
 * carries {@code data} = the bits so far (0/1 list),
 * {@code highlight} = [the index of the bit just added] and
 * {@code aux} = [bits completed]; the message names the bit,
 * the axis, the half and the emitted character when a group
 * of 5 completes, e.g. {@code Bit 1 (lon): -96.786 in lower
 * half [-180, 0] → 0}. The final Step carries {@code data} =
 * all bits and {@code sorted} = [precision]; its message
 * gives the hash and all 8 neighbours. Demo (Prosper, TX —
 * Babu's town — 33.236, -96.786, precision 6): hash
 * {@code 9vgjjd}, 30 bits, first 6 bits [0, 1, 0, 0, 1, 1],
 * neighbours N 9vgjje, S 9vgjj9, E 9vgjjf, W 9vgjj6,
 * NE 9vgjjg, NW 9vgjj7, SE 9vgjjc, SW 9vgjj3; 30 bit steps +
 * final = 31. Ground truth: (57.64911, 10.40744) encodes to
 * {@code u4pruydqqvj} at precision 11 (the Wikipedia example).</p>
 *
 * <p>The controller validates the input (word missing or not
 * a "lat,lon" pair &rarr; 400, lat outside [-90, 90] or lon
 * outside [-180, 180] &rarr; 400, precision outside 1..12
 * &rarr; 400); this method guards against bad input anyway
 * with a single "empty" Step, and defaults a missing
 * precision to 6.</p>
 */
@Service
public class GeohashService {

    private static final int LINE_LON_BIT = 3;
    private static final int LINE_LAT_BIT = 4;
    private static final int LINE_FINAL = 0;

    private static final int DEFAULT_PRECISION = 6;
    private static final String BASE32 =
            "0123456789bcdefghjkmnpqrstuvwxyz";

    /** Neighbour directions in message order, with (dlat, dlon) signs. */
    private static final String[] NEIGHBOUR_NAMES = {
            "N", "S", "E", "W", "NE", "NW", "SE", "SW"};
    private static final int[] NEIGHBOUR_DLAT = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] NEIGHBOUR_DLON = {0, 0, 1, -1, 1, -1, 1, -1};

    /**
     * Runs geohash encoding on {@code word} ("lat,lon") at the
     * given {@code precision} (null = 6).
     */
    public List<Step> geohash(String word, Integer precision) {
        List<Step> steps = new ArrayList<>();
        double[] latLon = parseLatLon(word);
        if (latLon == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A latitude/longitude pair "
                            + "(e.g. \"33.236,-96.786\") is needed.",
                    LINE_FINAL));
            return steps;
        }
        double lat = latLon[0];
        double lon = latLon[1];
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Latitude must be between -90 and 90 "
                            + "and longitude between -180 and 180.",
                    LINE_FINAL));
            return steps;
        }
        int prec = precision == null ? DEFAULT_PRECISION : precision;
        if (prec < 1 || prec > 12) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Precision must be between 1 and 12.",
                    LINE_FINAL));
            return steps;
        }

        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};
        List<Integer> bits = new ArrayList<>();
        StringBuilder hash = new StringBuilder();
        boolean isLon = true;
        for (int i = 0; i < prec * 5; i++) {
            int bit;
            double value;
            String axis;
            String half;
            if (isLon) {
                double mid = (lonRange[0] + lonRange[1]) / 2;
                value = lon;
                axis = "lon";
                if (lon >= mid) {
                    bit = 1;
                    lonRange[0] = mid;
                    half = "upper half [" + fmt(mid) + ", "
                            + fmt(lonRange[1]) + "]";
                } else {
                    bit = 0;
                    lonRange[1] = mid;
                    half = "lower half [" + fmt(lonRange[0]) + ", "
                            + fmt(mid) + "]";
                }
            } else {
                double mid = (latRange[0] + latRange[1]) / 2;
                value = lat;
                axis = "lat";
                if (lat >= mid) {
                    bit = 1;
                    latRange[0] = mid;
                    half = "upper half [" + fmt(mid) + ", "
                            + fmt(latRange[1]) + "]";
                } else {
                    bit = 0;
                    latRange[1] = mid;
                    half = "lower half [" + fmt(latRange[0]) + ", "
                            + fmt(mid) + "]";
                }
            }
            bits.add(bit);
            int line = isLon ? LINE_LON_BIT : LINE_LAT_BIT;
            isLon = !isLon;
            StringBuilder message = new StringBuilder();
            message.append("Bit ").append(i + 1).append(" (")
                    .append(axis).append("): ").append(fmt(value))
                    .append(" in ").append(half).append(" \u2192 ")
                    .append(bit);
            if (bits.size() % 5 == 0) {
                int v = 0;
                for (int b : bits.subList(bits.size() - 5, bits.size())) {
                    v = (v << 1) | b;
                }
                char c = BASE32.charAt(v);
                hash.append(c);
                message.append("; char '").append(c).append("'");
            }
            message.append(".");
            steps.add(geohashStep(bits, List.of(bits.size() - 1),
                    List.of(), List.of(bits.size()),
                    message.toString(), line));
        }

        Map<String, String> neighbours = neighbours(hash.toString());
        StringBuilder finalMessage = new StringBuilder();
        finalMessage.append("Geohash (precision ").append(prec)
                .append("): '").append(hash).append("'. Neighbours: ");
        for (int i = 0; i < NEIGHBOUR_NAMES.length; i++) {
            if (i > 0) {
                finalMessage.append(", ");
            }
            finalMessage.append(NEIGHBOUR_NAMES[i]).append(" ")
                    .append(neighbours.get(NEIGHBOUR_NAMES[i]));
        }
        finalMessage.append(".");
        steps.add(geohashStep(bits, List.of(), List.of(prec),
                List.of(bits.size()), finalMessage.toString(), LINE_FINAL));
        return steps;
    }

    /**
     * The 8 neighbours of a hash, by re-encoding the adjacent
     * cell centres at the same precision (the same method at
     * the edges — no string tricks).
     */
    private Map<String, String> neighbours(String hash) {
        double[][] ranges = decode(hash);
        double[] latRange = ranges[0];
        double[] lonRange = ranges[1];
        double clat = (latRange[0] + latRange[1]) / 2;
        double clon = (lonRange[0] + lonRange[1]) / 2;
        double dlat = latRange[1] - latRange[0];
        double dlon = lonRange[1] - lonRange[0];
        Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i < NEIGHBOUR_NAMES.length; i++) {
            double nlat = clat + NEIGHBOUR_DLAT[i] * dlat;
            double nlon = clon + NEIGHBOUR_DLON[i] * dlon;
            out.put(NEIGHBOUR_NAMES[i],
                    encode(nlat, nlon, hash.length()));
        }
        return out;
    }

    /** Encodes a lat/lon pair at the given precision (the hash only). */
    private String encode(double lat, double lon, int precision) {
        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};
        StringBuilder hash = new StringBuilder();
        List<Integer> bits = new ArrayList<>();
        boolean isLon = true;
        while (hash.length() < precision) {
            if (isLon) {
                double mid = (lonRange[0] + lonRange[1]) / 2;
                if (lon >= mid) {
                    bits.add(1);
                    lonRange[0] = mid;
                } else {
                    bits.add(0);
                    lonRange[1] = mid;
                }
            } else {
                double mid = (latRange[0] + latRange[1]) / 2;
                if (lat >= mid) {
                    bits.add(1);
                    latRange[0] = mid;
                } else {
                    bits.add(0);
                    latRange[1] = mid;
                }
            }
            isLon = !isLon;
            if (bits.size() % 5 == 0) {
                int v = 0;
                for (int b : bits.subList(bits.size() - 5, bits.size())) {
                    v = (v << 1) | b;
                }
                hash.append(BASE32.charAt(v));
            }
        }
        return hash.toString();
    }

    /** Decodes a hash into [latRange, lonRange]. */
    private double[][] decode(String hash) {
        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};
        boolean isLon = true;
        for (int i = 0; i < hash.length(); i++) {
            int v = BASE32.indexOf(hash.charAt(i));
            for (int j = 4; j >= 0; j--) {
                int bit = (v >> j) & 1;
                if (isLon) {
                    double mid = (lonRange[0] + lonRange[1]) / 2;
                    if (bit == 1) {
                        lonRange[0] = mid;
                    } else {
                        lonRange[1] = mid;
                    }
                } else {
                    double mid = (latRange[0] + latRange[1]) / 2;
                    if (bit == 1) {
                        latRange[0] = mid;
                    } else {
                        latRange[1] = mid;
                    }
                }
                isLon = !isLon;
            }
        }
        return new double[][]{latRange, lonRange};
    }

    /**
     * Parses "lat,lon" (whitespace tolerated) into [lat, lon],
     * or null when the word is missing or unparseable.
     */
    private double[] parseLatLon(String word) {
        if (word == null) {
            return null;
        }
        String[] parts = word.split(",", -1);
        if (parts.length != 2) {
            return null;
        }
        try {
            double lat = Double.parseDouble(parts[0].trim());
            double lon = Double.parseDouble(parts[1].trim());
            if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
                return null;
            }
            return new double[]{lat, lon};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Formats a coordinate or bound: integers without a decimal
     * point, other values with up to 4 decimals, trailing zeros
     * stripped (33.236 stays "33.236", 22.5 stays "22.5").
     */
    private String fmt(double value) {
        if (value == Math.rint(value)
                && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        String text = String.format(java.util.Locale.ROOT, "%.4f", value);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    /** One geohash Step: bits in data, bits completed in aux. */
    private Step geohashStep(List<Integer> bits, List<Integer> highlight,
                             List<Integer> sorted, List<Integer> aux,
                             String message, int line) {
        Step step = new Step(new ArrayList<>(bits), highlight,
                List.of(), new ArrayList<>(sorted), message, line);
        step.setAux(new ArrayList<>(aux));
        return step;
    }
}
