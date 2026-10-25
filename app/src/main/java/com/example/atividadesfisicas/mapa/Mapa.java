package com.example.atividadesfisicas.mapa;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.Location;
import android.location.LocationManager;
import com.example.atividadesfisicas.modelo.PontoGPS;
import com.example.atividadesfisicas.utilitarios.Config;
import com.example.atividadesfisicas.utilitarios.Permissoes;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Tudo o que depende da biblioteca de mapas (OpenStreetMap / osmdroid) está aqui,
 * para ser fácil trocar de biblioteca (ex.: Google Maps) mexendo só neste ficheiro.
 */
public final class Mapa {

    /** Centro do mapa enquanto não há posição do utilizador (Porto / Vila Nova de Gaia). */
    private static final GeoPoint LOCAL_POR_OMISSAO = new GeoPoint(41.1400, -8.6130);

    private Mapa() {
    }

    /**
     * Chamar ANTES de setContentView em qualquer ecrã com mapa.
     */
    public static void inicializar(Context ctx) {
        IConfigurationProvider cfg = Configuration.getInstance();
        cfg.setUserAgentValue(Config.USER_AGENT_MAPA);
        File base = new File(ctx.getCacheDir(), "osmdroid");
        cfg.setOsmdroidBasePath(base);
        cfg.setOsmdroidTileCache(new File(base, "tiles"));
    }

    public static void configurar(MapView map, double zoom) {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(zoom);
        // sem isto o mapa abre em (0,0); começa no Porto/Gaia até haver posição do utilizador
        map.getController().setCenter(LOCAL_POR_OMISSAO);
    }

    /**
     * Acrescenta ao mapa o ícone da posição do utilizador, que o acompanha enquanto se desloca.
     * Se seguir=true, o mapa fica centrado no utilizador (deixa de seguir se tocar/arrastar o mapa).
     * Chamar disableMyLocation() em onPause e enableMyLocation() em onResume.
     */
    public static MyLocationNewOverlay adicionarMinhaLocalizacao(Context ctx, MapView map, boolean seguir) {
        MyLocationNewOverlay overlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx), map);
        // ícones próprios (roxo com contorno branco) para se distinguirem do mapa e das linhas das rotas
        Bitmap ponto = criarIconeUtilizador(ctx, false);
        Bitmap seta = criarIconeUtilizador(ctx, true);
        overlay.setDirectionArrow(ponto, seta);
        overlay.setPersonHotspot(ponto.getWidth() / 2f, ponto.getHeight() / 2f);
        overlay.enableMyLocation();
        if (seguir) {
            overlay.enableFollowLocation();
        }
        map.getOverlays().add(overlay);
        return overlay;
    }

    private static final int COR_UTILIZADOR = 0xFFAA00FF;

    /**
     * Desenha o ícone da posição do utilizador: um ponto roxo com halo (parado) ou uma seta roxa
     * (em movimento). Os dois têm contorno branco.
     */
    private static Bitmap criarIconeUtilizador(Context ctx, boolean seta) {
        float d = ctx.getResources().getDisplayMetrics().density;
        int tam = Math.round(44 * d);
        Bitmap bmp = Bitmap.createBitmap(tam, tam, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);
        float cx = tam / 2f;
        float cy = tam / 2f;
        if (!seta) {
            tinta.setColor((COR_UTILIZADOR & 0x00FFFFFF) | 0x55000000);
            canvas.drawCircle(cx, cy, tam / 2f, tinta);
            tinta.setColor(Color.WHITE);
            canvas.drawCircle(cx, cy, tam * 0.30f, tinta);
            tinta.setColor(COR_UTILIZADOR);
            canvas.drawCircle(cx, cy, tam * 0.22f, tinta);
        } else {
            Path caminho = new Path();
            caminho.moveTo(cx, tam * 0.06f);
            caminho.lineTo(tam * 0.84f, tam * 0.90f);
            caminho.lineTo(cx, tam * 0.70f);
            caminho.lineTo(tam * 0.16f, tam * 0.90f);
            caminho.close();
            tinta.setStyle(Paint.Style.STROKE);
            tinta.setStrokeWidth(4 * d);
            tinta.setStrokeJoin(Paint.Join.ROUND);
            tinta.setColor(Color.WHITE);
            canvas.drawPath(caminho, tinta);
            tinta.setStyle(Paint.Style.FILL);
            tinta.setColor(COR_UTILIZADOR);
            canvas.drawPath(caminho, tinta);
        }
        return bmp;
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
        if (!Permissoes.temLocalizacao(ctx)) {
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