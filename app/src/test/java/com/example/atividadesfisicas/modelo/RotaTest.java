package com.example.atividadesfisicas.modelo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** Testa a criação de rotas e a sua conversão para JSON (é assim que são guardadas). */
public class RotaTest {

    @Test
    public void criada_geraIdComPrefixoEModoCriada() {
        Rota r = Rota.criada("Passeio", new ArrayList<PontoGPS>());
        assertTrue(r.getId().startsWith("criada_"));
        assertEquals("Passeio", r.getNome());
        assertEquals(Rota.Modo.CRIADA, r.getModo());
    }

    @Test
    public void criada_copiaOsPontosEZeraOInstante() {
        List<PontoGPS> origem = new ArrayList<>();
        origem.add(new PontoGPS(41.1, -8.6, 50.0, 999L));
        Rota r = Rota.criada("R", origem);
        assertEquals(1, r.getPontos().size());
        assertEquals(41.1, r.getPontos().get(0).getLatitude(), 0.0);
        assertEquals(0L, r.getPontos().get(0).getInstante());   // uma rota não tem horas
    }

    @Test
    public void json_idaEVolta_mantemTudo() throws JSONException {
        Rota original = new Rota("pre_9", "Teste", Rota.Modo.PREDEFINIDA);
        original.adicionarPonto(new PontoGPS(41.1, -8.6, 10, 0));
        original.adicionarPonto(new PontoGPS(41.2, -8.7, 20, 0));

        Rota copia = Rota.fromJson(original.toJson());

        assertEquals("pre_9", copia.getId());
        assertEquals("Teste", copia.getNome());
        assertEquals(Rota.Modo.PREDEFINIDA, copia.getModo());
        assertEquals(2, copia.getPontos().size());
        assertEquals(41.2, copia.getPontos().get(1).getLatitude(), 0.0);
    }
}