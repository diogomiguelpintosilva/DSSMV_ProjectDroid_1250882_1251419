package com.example.atividadesfisicas.utilitarios;

import com.example.atividadesfisicas.modelo.Telemetria;

import java.util.Locale;

/**
 * Formatação de valores para mostrar no ecrã e nas fotos/vídeos.
 */
public final class Formatacao {

    private Formatacao() {
    }

    /** Converte milissegundos em "HH:MM:SS". */
    public static String formatarTempo(long ms) {
        long totalSegundos = ms / 1000;
        long horas = totalSegundos / 3600;
        long minutos = (totalSegundos % 3600) / 60;
        long segundos = totalSegundos % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", horas, minutos, segundos);
    }

    /** Velocidade com uma casa decimal, por exemplo "8,4 km/h". */
    public static String formatarVelocidade(double kmh) {
        return String.format(Locale.getDefault(), "%.1f km/h", kmh);
    }

    /** Altitude sem casas decimais, por exemplo "62 m". */
    public static String formatarAltitude(double metros) {
        return String.format(Locale.getDefault(), "%.0f m", metros);
    }

    /** Distância em metros (abaixo de 1 km) ou em quilómetros com duas casas decimais. */
    public static String formatarDistancia(double metros) {
        if (metros >= 1000) {
            return String.format(Locale.getDefault(), "%.2f km", metros / 1000.0);
        }
        return String.format(Locale.getDefault(), "%.0f m", metros);
    }

    /**
     * Texto com as métricas do momento, escrito nas fotos e nos vídeos (UC7 e UC8).
     * Sem telemetria (ainda sem sinal GPS) mostra "--".
     */
    public static String textoMetricas(Telemetria t) {
        if (t == null) {
            return "Vel: -- | Alt: --";
        }
        return "Vel: " + formatarVelocidade(t.getVelocidadeKmh()) + " | Alt: " + formatarAltitude(t.getAltitude());
    }
}
