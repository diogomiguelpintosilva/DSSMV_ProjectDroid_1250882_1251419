package com.example.atividadesfisicas.utilitarios;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

/** Testa os textos mostrados no ecrã (tempo e distância). Locale fixo em US para o "." decimal. */
public class FormatacaoTest {

    private Locale original;

    @Before
    public void fixarLocale() {
        original = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
    public void repor() {
        Locale.setDefault(original);
    }

    @Test
    public void tempo_horasMinutosSegundos() {
        assertEquals("00:00:00", Formatacao.formatarTempo(0));
        assertEquals("01:01:01", Formatacao.formatarTempo(3_661_000));
    }

    @Test
    public void distancia_metrosAteMilEDepoisKm() {
        assertEquals("999 m", Formatacao.formatarDistancia(999));
        assertEquals("1.23 km", Formatacao.formatarDistancia(1234.5));
    }
}