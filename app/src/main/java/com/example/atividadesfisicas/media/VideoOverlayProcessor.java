package com.example.atividadesfisicas.media;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import androidx.annotation.OptIn;
import androidx.media3.common.Effect;
import androidx.media3.common.MediaItem;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.OverlayEffect;
import androidx.media3.effect.OverlaySettings;
import androidx.media3.effect.TextOverlay;
import androidx.media3.effect.TextureOverlay;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.Effects;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.Transformer;
import com.example.atividadesfisicas.model.Telemetria;
import com.example.atividadesfisicas.util.FormatUtil;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
/**
 * UC8: depois de o vídeo ser gravado,
 "queima" nele a velocidade e
 a altitude de cada instante (overlay).
 * Usa o Media3 Transformer. TEM de ser chamado na UI thread (os
 callbacks também chegam à UI thread).
 */
@OptIn(markerClass = UnstableApi.class)
public final class VideoOverlayProcessor {
    public interface Listener {
        void onConcluido();
        void onErro(Exception e);
    }
    // guarda os Transformers em curso para não serem apanhados pelo
    garbage collector
    private static final List<Transformer> EM_CURSO = new
            ArrayList<>();
    private VideoOverlayProcessor() {
    }
    public static long duracaoMs(File ficheiro) {
        MediaMetadataRetriever retriever = new
                MediaMetadataRetriever();
        try {
            retriever.setDataSource(ficheiro.getAbsolutePath());
            String duracao =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return (duracao == null) ? 0 : Long.parseLong(duracao);
        } catch (Exception e) {
            return 0;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignorado) {
// nada a fazer
            }
        }
    }
    /**
     * @param amostras telemetria recolhida durante a atividade
     * @param inicioMs instante (System.currentTimeMillis) em que o
    vídeo começou a ser gravado
     */
    public static void processar(Context ctx, File entrada, File
                                         saida, List<Telemetria> amostras,
                                 long inicioMs, final Listener
                                         listener) {
        TelemetriaOverlay overlay = new TelemetriaOverlay(amostras,
                inicioMs);
        OverlayEffect efeitoOverlay = new
                OverlayEffect(ImmutableList.<TextureOverlay>of(overlay));
        Effects efeitos = new
                Effects(ImmutableList.<AudioProcessor>of(),
                ImmutableList.<Effect>of(efeitoOverlay));
        EditedMediaItem item = new
                EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(entrada)))
                .setEffects(efeitos)
                .build();
        final Transformer[] referencia = new Transformer[1];
        Transformer transformer = new
                Transformer.Builder(ctx.getApplicationContext())
                .addListener(new Transformer.Listener() {
                    @Override
                    public void onCompleted(Composition composition,
                                            ExportResult exportResult) {
                        EM_CURSO.remove(referencia[0]);
                        listener.onConcluido();
                    }
                    @Override
                    public void onError(Composition composition,
                                        ExportResult exportResult,
                                        ExportException
                                                exportException) {
                        EM_CURSO.remove(referencia[0]);
                        listener.onErro(exportException);
                    }
                })
                .build();
        referencia[0] = transformer;
        EM_CURSO.add(transformer);
        transformer.start(item, saida.getAbsolutePath());
    }
    /** Texto que muda ao longo do vídeo conforme a telemetria do
     instante correspondente.
     */
    private static class TelemetriaOverlay extends TextOverlay {
        private final List<Telemetria> amostras;
        private final long inicioMs;
        TelemetriaOverlay(List<Telemetria> amostras, long inicioMs)
        {
            this.amostras = amostras;
            this.inicioMs = inicioMs;
        }
        @Override
        public SpannableString getText(long presentationTimeUs) {
            long instante = inicioMs + presentationTimeUs / 1000;
            Telemetria t = procurar(instante);
            String texto;
            if (t == null) {
                texto =
                        " Vel: -- | Alt: -- ";
            } else {
                texto =
                        " Vel: " +
                                FormatUtil.formatarVelocidade(t.getVelocidadeKmh())
                                + " | Alt: " +
                                FormatUtil.formatarAltitude(t.getAltitude()) + " ";
            }
            SpannableString spannable = new SpannableString(texto);
            spannable.setSpan(new ForegroundColorSpan(Color.WHITE),
                    0, texto.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new BackgroundColorSpan(0x99000000),
                    0, texto.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new AbsoluteSizeSpan(48), 0,
                    texto.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return spannable;
        }
        @Override
        public OverlaySettings getOverlaySettings(long
                                                          presentationTimeUs) {
// canto inferior esquerdo do vídeo
            return new OverlaySettings.Builder()
                    .setOverlayFrameAnchor(-1f,
                            -1f)
                    .setBackgroundFrameAnchor(-0.95f,-0.9f).build();
        }
        /** Última amostra com instante <= ao pedido (ou a primeira,
         se o vídeo começar antes da primeira amostra).
         */
        private Telemetria procurar(long instante) {
            if (amostras.isEmpty()) {
                return null;
            }
            Telemetria escolhida = amostras.get(0);
            for (Telemetria t : amostras) {
                if (t.getInstante() <= instante) {
                    escolhida = t;
                } else {
                    break;
                }
            }
            return escolhida;
        }
    }
}