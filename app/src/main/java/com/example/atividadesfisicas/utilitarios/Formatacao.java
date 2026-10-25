package com.example.atividadesfisicas.utilitarios;

import java.util.Locale;

/** Formatação de valores para mostrar no ecrã.*/

public final class Formatacao {

    private Formatacao() {
    }

    public static String formatarTempo(long ms) {
        long totalSegundos = ms / 1000;
        long horas = totalSegundos / 3600;
        long minutos = (totalSegundos % 3600) / 60;
        long segundos = totalSegundos % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", horas, minutos, segundos);
    }

    public static String formatarVelocidade(double kmh) {
        return String.format(Locale.getDefault(), "%.1f km/h", kmh);
    }

    public static String formatarAltitude(double metros) {
        return String.format(Locale.getDefault(), "%.0f m", metros);
    }

    public static String formatarDistancia(double metros) {
        if (metros >= 1000) {
            return String.format(Locale.getDefault(), "%.2f km", metros / 1000.0);
        }
        return String.format(Locale.getDefault(), "%.0f m", metros);
    }
}