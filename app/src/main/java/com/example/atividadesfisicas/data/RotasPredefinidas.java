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

        // Coordenadas de pontos reais de Gaia (Jardim do Morro, Passeio das Caves, Afurada);
        // o ponto intermédio é aproximado. Confirmar no mapa e ajustar se for preciso.
        Rota gaia = new Rota("pre_3", "Gaia: Jardim do Morro - Afurada", Rota.Modo.PREDEFINIDA);
        adicionar(gaia, 41.13721, -8.60906); // Jardim do Morro
        adicionar(gaia, 41.13764, -8.61447); // Passeio das Caves (Av. Diogo Leite)
        adicionar(gaia, 41.13700, -8.62300); // margem do Douro
        adicionar(gaia, 41.13600, -8.63092); // Afurada
        lista.add(gaia);

        return lista;
    }

    private static void adicionar(Rota rota, double lat, double lon) {
        rota.adicionarPonto(new PontoGPS(lat, lon, 0, 0));
    }
}