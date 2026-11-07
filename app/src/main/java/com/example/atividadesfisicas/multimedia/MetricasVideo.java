package com.example.atividadesfisicas.multimedia;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.annotation.OptIn;
import androidx.media3.common.Effect;
import androidx.media3.common.MediaItem;
import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.BitmapOverlay;
import androidx.media3.effect.OverlayEffect;
import androidx.media3.effect.TextureOverlay;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.Effects;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.Transformer;

import com.example.atividadesfisicas.modelo.Telemetria;
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
     * Largura e altura do vídeo tal como é visto (já "em pé"). Se o vídeo tiver rotação de 90 ou 270
     * graus nos metadados, troca a largura com a altura. Se não conseguir ler, assume 1080x1920.
     */
    private static int[] tamanhoVideo(File ficheiro) {
        MediaMetadataRetriever leitor = new MediaMetadataRetriever();
        try {
            leitor.setDataSource(ficheiro.getAbsolutePath());
            int largura = Integer.parseInt(
                    leitor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int altura = Integer.parseInt(
                    leitor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            String texto = leitor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            int graus = (texto == null) ? 0 : Integer.parseInt(texto);
            if (graus == 90 || graus == 270) {
                return new int[]{altura, largura};
            }
            return new int[]{largura, altura};
        } catch (Exception e) {
            return new int[]{1080, 1920};
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
        int[] tamanho = tamanhoVideo(entrada);
        TextoTelemetria texto = new TextoTelemetria(amostras, inicioMs, tamanho[0], tamanho[1]);
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
     * Imagem transparente do tamanho do vídeo, com o texto desenhado como nas fotos
     * (MetricasFoto.desenharTexto). Como cobre o fotograma inteiro, o texto fica sempre
     * com o mesmo tamanho e posição que na foto, seja qual for a resolução do vídeo.
     * A imagem só é redesenhada quando a telemetria muda.
     */
    private static class TextoTelemetria extends BitmapOverlay {
        private final List<Telemetria> amostras;
        private final long inicioMs;
        private final int largura;
        private final int altura;

        private Bitmap imagem;        // última imagem desenhada
        private Telemetria ultima;    // telemetria usada nessa imagem

        TextoTelemetria(List<Telemetria> amostras, long inicioMs, int largura, int altura) {
            this.amostras = amostras;
            this.inicioMs = inicioMs;
            this.largura = largura;
            this.altura = altura;
        }

        @Override
        public Bitmap getBitmap(long presentationTimeUs) throws VideoFrameProcessingException {
            // instante real do fotograma = início do vídeo + tempo do fotograma (microssegundos ÷ 1000)
            long instante = inicioMs + presentationTimeUs / 1000;
            Telemetria atual = procurar(instante);
            if (imagem == null || atual != ultima) {
                try {
                    Bitmap nova = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888);
                    MetricasFoto.desenharTexto(new Canvas(nova), largura, altura, atual);
                    imagem = nova;
                    ultima = atual;
                } catch (OutOfMemoryError e) {
                    throw new VideoFrameProcessingException("Sem memória para escrever a telemetria no vídeo.");
                }
            }
            return imagem;
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
