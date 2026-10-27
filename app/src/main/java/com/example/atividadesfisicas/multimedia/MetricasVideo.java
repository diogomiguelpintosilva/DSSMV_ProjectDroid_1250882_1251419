package com.example.atividadesfisicas.multimedia;

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

import com.example.atividadesfisicas.modelo.Telemetria;
import com.example.atividadesfisicas.utilitarios.Formatacao;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * UC8: depois de o vídeo ser gravado, escreve nele a velocidade e a altitude de cada instante.
 * Usa o Media3 Transformer. TEM de ser chamado na thread principal (os avisos de fim também chegam lá).
 */
@OptIn(markerClass = UnstableApi.class)
public final class MetricasVideo {

    /** Avisa quem pediu o processamento quando ele termina. */
    public interface AoTerminar {
        void onConcluido();

        void onErro(Exception e);
    }

    // guarda os exportadores em curso para não serem apagados pelo garbage collector a meio do trabalho
    private static final List<Transformer> EM_CURSO = new ArrayList<>();

    private MetricasVideo() {
    }

    /** Duração de um vídeo em milissegundos (0 se não for possível ler). */
    public static long duracaoMs(File ficheiro) {
        MediaMetadataRetriever leitor = new MediaMetadataRetriever();
        try {
            leitor.setDataSource(ficheiro.getAbsolutePath());
            String duracao = leitor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return (duracao == null) ? 0 : Long.parseLong(duracao);
        } catch (Exception e) {
            return 0;
        } finally {
            try {
                leitor.release();
            } catch (Exception ignorado) {
                // nada a fazer
            }
        }
    }

    /**
     * @param amostras telemetria recolhida durante o exercício
     * @param inicioMs instante (System.currentTimeMillis) em que o vídeo começou a ser gravado
     */
    public static void processar(Context ctx, File entrada, File saida, List<Telemetria> amostras,
                                 long inicioMs, final AoTerminar aoTerminar) {
        TextoTelemetria texto = new TextoTelemetria(amostras, inicioMs);
        OverlayEffect efeitoTexto = new OverlayEffect(ImmutableList.<TextureOverlay>of(texto));
        Effects efeitos = new Effects(ImmutableList.<AudioProcessor>of(), ImmutableList.<Effect>of(efeitoTexto));
        EditedMediaItem item = new EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(entrada)))
                .setEffects(efeitos)
                .build();

        final Transformer[] referencia = new Transformer[1];
        Transformer exportador = new Transformer.Builder(ctx.getApplicationContext())
                .setEncoderFactory(ConfigCodificadorVideo.codificador(ctx))
                .addListener(new Transformer.Listener() {
                    @Override
                    public void onCompleted(Composition composicao, ExportResult resultado) {
                        EM_CURSO.remove(referencia[0]);
                        aoTerminar.onConcluido();
                    }

                    @Override
                    public void onError(Composition composicao, ExportResult resultado, ExportException erro) {
                        EM_CURSO.remove(referencia[0]);
                        aoTerminar.onErro(erro);
                    }
                })
                .build();
        referencia[0] = exportador;
        EM_CURSO.add(exportador);
        exportador.start(item, saida.getAbsolutePath());
    }

    /**
     * Texto que muda ao longo do vídeo, conforme a telemetria do instante de cada fotograma.
     */
    private static class TextoTelemetria extends TextOverlay {
        private final List<Telemetria> amostras;
        private final long inicioMs;

        TextoTelemetria(List<Telemetria> amostras, long inicioMs) {
            this.amostras = amostras;
            this.inicioMs = inicioMs;
        }

        @Override
        public SpannableString getText(long presentationTimeUs) {
            // instante real do fotograma = início do vídeo + tempo do fotograma (microssegundos ÷ 1000)
            long instante = inicioMs + presentationTimeUs / 1000;
            String texto = " " + Formatacao.textoMetricas(procurar(instante)) + " ";
            SpannableString formatado = new SpannableString(texto);
            formatado.setSpan(new ForegroundColorSpan(Color.WHITE), 0, texto.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            formatado.setSpan(new BackgroundColorSpan(0x99000000), 0, texto.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            formatado.setSpan(new AbsoluteSizeSpan(48), 0, texto.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return formatado;
        }

        @Override
        public OverlaySettings getOverlaySettings(long presentationTimeUs) {
            // canto inferior esquerdo do vídeo (coordenadas de -1 a 1)
            return new OverlaySettings.Builder()
                    .setOverlayFrameAnchor(-1f, -1f)
                    .setBackgroundFrameAnchor(-0.95f, -0.9f)
                    .build();
        }

        /**
         * Última amostra com instante <= ao pedido (ou a primeira, se o vídeo começar antes da primeira amostra).
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
