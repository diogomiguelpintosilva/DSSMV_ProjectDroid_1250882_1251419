package com.example.atividadesfisicas.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import com.example.atividadesfisicas.model.PontoGPS;
import com.example.atividadesfisicas.util.PermissoesHelper;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Tudo o que depende da biblioteca de mapas (OpenStreetMap / osmdroid) está aqui,
 * para ser fácil trocar de biblioteca (ex.: Google Maps) mexendo só neste ficheiro.
 */
public final class MapaHelper {

    private MapaHelper() {
    }

    /**
     * Chamar ANTES de setContentView em qualquer ecrã com mapa.
     */
    public static void inicializar(Context ctx) {
        IConfigurationProvider cfg = Configuration.getInstance();
        cfg.setUserAgentValue(ctx.getPackageName());
        File base = new File(ctx.getCacheDir(), "osmdroid");
        cfg.setOsmdroidBasePath(base);
        cfg.setOsmdroidTileCache(new File(base, "tiles"));
    }

    public static void configurar(MapView map, double zoom) {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(zoom);
    }

    public static GeoPoint paraGeoPoint(PontoGPS p) {
        return new GeoPoint(p.getLatitude(), p.getLongitude());
    }

    public static List<GeoPoint> paraGeoPoints(List<PontoGPS> pontos) {
        List<GeoPoint> lista = new ArrayList<>();
        for (PontoGPS p : pontos) {
            lista.add(paraGeoPoint(p));
        }
        return lista;
    }

    public static Polyline criarLinha(List<GeoPoint> pontos, int cor, float largura) {
        Polyline linha = new Polyline();
        linha.setPoints(pontos);
        linha.getOutlinePaint().setColor(cor);
        linha.getOutlinePaint().setStrokeWidth(largura);
        return linha;
    }

    public static void centrar(MapView map, GeoPoint ponto) {
        map.getController().setCenter(ponto);
    }

    /**
     * Ajusta o zoom para mostrar todos os pontos (precisa de pelo menos 2).
     */
    public static void ajustarAosPontos(final MapView map, final List<GeoPoint> pontos) {
        if (pontos.size() < 2) {
            if (pontos.size() == 1) {
                centrar(map, pontos.get(0));
            }
            return;
        }
        map.post(new Runnable() {
            @Override
            public void run() {
                map.zoomToBoundingBox(BoundingBox.fromGeoPoints(pontos), false, 80);
            }
        });
    }

    /**
     * Última posição conhecida (ou null se não houver permissão/posição).
     */
    @SuppressLint("MissingPermission")
    public static GeoPoint ultimaPosicaoConhecida(Context ctx) {
        if (!PermissoesHelper.temLocalizacao(ctx)) {
            return null;
        }
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            return null;
        }
        Location melhor = null;
        for (String provider : lm.getProviders(true)) {
            Location l = lm.getLastKnownLocation(provider);
            if (l != null && (melhor == null || l.getAccuracy() < melhor.getAccuracy())) {
                melhor = l;
            }
        }
        return (melhor == null) ? null : new GeoPoint(melhor.getLatitude(), melhor.getLongitude());
    }
}