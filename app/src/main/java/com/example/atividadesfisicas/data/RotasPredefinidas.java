package com.example.atividadesfisicas.data;

import com.example.atividadesfisicas.model.PontoGPS;
import com.example.atividadesfisicas.model.Rota;

import java.util.ArrayList;
import java.util.List;

/**
 * UC3: rotas predefinidas existentes na aplicação.
 * ATENÇÃO: coordenadas de EXEMPLO (centro do Porto). Substituir/acrescentar rotas reais conforme necessário.
 */
public final class RotasPredefinidas {

    private RotasPredefinidas() {
    }

    public static List<Rota> obter() {
        List<Rota> lista = new ArrayList<>();

        Rota centro = new Rota("pre_1", "Centro do Porto (exemplo)", Rota.Modo.PREDEFINIDA);
        adicionar(centro, 41.14582, -8.61398); // Torre dos Clérigos
        adicionar(centro, 41.14690, -8.61100); // Praça da Liberdade
        adicionar(centro, 41.14064, -8.61386); // Ribeira
        adicionar(centro, 41.13990, -8.60940); // Ponte D. Luís
        lista.add(centro);

        Rota jardim = new Rota("pre_2", "Clérigos - Palácio de Cristal (exemplo)", Rota.Modo.PREDEFINIDA);
        adicionar(jardim, 41.14582, -8.61398);
        adicionar(jardim, 41.14700, -8.62000);
        adicionar(jardim, 41.14750, -8.62540); // Palácio de Cristal
        lista.add(jardim);

        return lista;
    }

    private static void adicionar(Rota rota, double lat, double lon) {
        rota.adicionarPonto(new PontoGPS(lat, lon, 0, 0));
    }
}