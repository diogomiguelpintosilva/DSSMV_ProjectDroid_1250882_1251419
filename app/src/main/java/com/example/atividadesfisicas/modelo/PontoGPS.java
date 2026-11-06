package com.example.atividadesfisicas.modelo;

import org.json.JSONException; //erro que JSON lança se estiver mal formado
import org.json.JSONObject;//tipo de objeto JSON,ou seja, que é do tipo chave-valor


public class PontoGPS {
    //Atributos privados, apenas a classe pode mudar.
    private final double latitude;
    private final double longitude;
    private final double altitude;
    private final long instante;//forma comum de guardar uma data em java

    //CONSTRUTOR
    public PontoGPS(double latitude, double longitude, double altitude, long instante) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.instante = instante;
    }

    //GETTERS
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

    public JSONObject toJson() throws JSONException { //cira objeto/ponto que é convertido para JSON
        JSONObject obj = new JSONObject();//cria objeto vazio
        obj.put("lat", latitude);
        obj.put("lon", longitude);
        obj.put("alt", altitude);
        obj.put("t", instante);
        return obj; //objeto já com quatro chaves, que mais tarde vão ter valores associados ao GPS
    }
    //Converte um JSON para um ponto GPS
    public static PontoGPS fromJson(JSONObject obj) throws JSONException {
        return new PontoGPS(
                obj.getDouble("lat"),//lança exceção se faltar o valor
                obj.getDouble("lon"),
                obj.optDouble("alt", 0),//valor opcional
                obj.optLong("t", 0)//valor opcional, pois rotas planeadas não tem
        );
    }
}