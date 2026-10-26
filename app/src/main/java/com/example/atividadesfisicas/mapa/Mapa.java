package com.example.atividadesfisicas.mapa;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

    /** Cor da rota planeada (azul). */
    public static final int COR_ROTA_PLANEADA = 0xFF1565C0;
    /** Cor do percurso feito (vermelho). */
    public static final int COR_PERCURSO = 0xFFD32F2F;
    /** Cor do ícone do utilizador (roxo), diferente das duas linhas. */
    private static final int COR_UTILIZADOR = 0xFFAA00FF;

    /** Centro do mapa enquanto não há posição do utilizador (Porto / Vila Nova de Gaia). */
    private static final GeoPoint LOCAL_POR_OMISSAO = new GeoPoint(41.1400, -8.6130);

    private Mapa() {
    }

    /**
     * Configuração geral do osmdroid. Chamar ANTES de setContentView em qualquer ecrã com mapa.
     */
    public static void inicializar(Context ctx) {
        IConfigurationProvider configuracao = Configuration.getInstance();
        configuracao.setUserAgentValue(Config.USER_AGENT_MAPA);
        File base = new File(ctx.getCacheDir(), "osmdroid");
        configuracao.setOsmdroidBasePath(base);
        configuracao.setOsmdroidTileCache(new File(base, "tiles"));
    }

    /** Estilo do mapa, zoom com dois dedos, zoom inicial e centro inicial. */
    public static void configurar(MapView mapa, double zoom) {
        mapa.setTileSource(TileSourceFactory.MAPNIK);
        mapa.setMultiTouchControls(true);
        mapa.getController().setZoom(zoom);
        // sem isto o mapa abre em (0,0); começa no Porto/Gaia até haver posição do utilizador
        mapa.getController().setCenter(LOCAL_POR_OMISSAO);
    }

    /**
     * Acrescenta ao mapa o ícone da posição do utilizador, que o acompanha enquanto se desloca.
     * Se seguir=true, o mapa fica centrado no utilizador (deixa de seguir se o utilizador arrastar o mapa).
     * Chamar disableMyLocation() em onPause e enableMyLocation() em onResume.
     */
    public static MyLocationNewOverlay adicionarMinhaLocalizacao(Context ctx, MapView mapa, boolean seguir) {
        MyLocationNewOverlay camada = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx), mapa);
        // ícone próprio (roxo) para se distinguir do mapa e das linhas das rotas; é o mesmo parado e em movimento
        Bitmap ponto = criarIconeUtilizador(ctx);
        camada.setDirectionArrow(ponto, ponto);
        camada.setPersonHotspot(ponto.getWidth() / 2f, ponto.getHeight() / 2f);
        camada.enableMyLocation();
        if (seguir) {
            camada.enableFollowLocation();
        }
        mapa.getOverlays().add(camada);
        return camada;
    }

    /** Desenha o ícone da posição do utilizador: um ponto roxo com contorno branco e um halo transparente. */
    private static Bitmap criarIconeUtilizador(Context ctx) {
        float densidade = ctx.getResources().getDisplayMetrics().density;
        int tamanho = Math.round(44 * densidade);
        Bitmap imagem = Bitmap.createBitmap(tamanho, tamanho, Bitmap.Config.ARGB_8888);
        Canvas tela = new Canvas(imagem);
        Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);
        float centro = tamanho / 2f;

        tinta.setColor((COR_UTILIZADOR & 0x00FFFFFF) | 0x55000000); // a mesma cor, semitransparente
        tela.drawCircle(centro, centro, tamanho / 2f, tinta);
        tinta.setColor(Color.WHITE);
        tela.drawCircle(centro, centro, tamanho * 0.30f, tinta);
        tinta.setColor(COR_UTILIZADOR);
        tela.drawCircle(centro, centro, tamanho * 0.22f, tinta);
        return imagem;
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

    /** Linha que passa pelos pontos, com a cor e a espessura pedidas. */
    public static Polyline criarLinha(List<GeoPoint> pontos, int cor, float largura) {
        Polyline linha = new Polyline();
        linha.setPoints(pontos);
        linha.getOutlinePaint().setColor(cor);
        linha.getOutlinePaint().setStrokeWidth(largura);
        return linha;
    }

    public static void centrar(MapView mapa, GeoPoint ponto) {
        mapa.getController().setCenter(ponto);
    }

    /**
     * Ajusta o zoom para mostrar todos os pontos (com um só ponto, centra nele).
     */
    public static void ajustarAosPontos(final MapView mapa, final List<GeoPoint> pontos) {
        if (pontos.size() < 2) {
            if (pontos.size() == 1) {
                centrar(mapa, pontos.get(0));
            }
            return;
        }
        // post: o mapa só sabe o seu tamanho depois de ser desenhado pela primeira vez
        mapa.post(() -> mapa.zoomToBoundingBox(BoundingBox.fromGeoPoints(pontos), false, 80));
    }

    /**
     * Última posição conhecida do telemóvel (ou null se não houver permissão ou posição).
     */
    @SuppressLint("MissingPermission")
    public static GeoPoint ultimaPosicaoConhecida(Context ctx) {
        if (!Permissoes.temLocalizacao(ctx)) {
            return null;
        }
        LocationManager gestorLocalizacao = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        if (gestorLocalizacao == null) {
            return null;
        }
        Location melhor = null;
        for (String fornecedor : gestorLocalizacao.getProviders(true)) {
            Location l = gestorLocalizacao.getLastKnownLocation(fornecedor);
            if (l != null && (melhor == null || l.getAccuracy() < melhor.getAccuracy())) {
                melhor = l;
            }
        }
        return (melhor == null) ? null : new GeoPoint(melhor.getLatitude(), melhor.getLongitude());
    }
}
