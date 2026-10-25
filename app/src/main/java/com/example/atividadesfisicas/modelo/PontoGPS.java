package com.example.atividadesfisicas.modelo;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Modelo de domínio: ponto GPS (latitude, longitude, altitude e instante).
 */
public class PontoGPS {

    private final double latitude;
    private final double longitude;
    private final double altitude;
    private final long instante;

    public PontoGPS(double latitude, double longitude, double altitude, long instante) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.instante = instante;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public long getInstante() {
        return instante;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("lat", latitude);
        obj.put("lon", longitude);
        obj.put("alt", altitude);
        obj.put("t", instante);
        return obj;
    }

    public static PontoGPS fromJson(JSONObject obj) throws JSONException {
        return new PontoGPS(
                obj.getDouble("lat"),
                obj.getDouble("lon"),
                obj.optDouble("alt", 0),
                obj.optLong("t", 0)
        );
    }
}