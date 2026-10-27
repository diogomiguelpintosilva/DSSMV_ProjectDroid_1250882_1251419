package com.example.atividadesfisicas.multimedia;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.OptIn;
import androidx.media3.common.Effect;
import androidx.media3.common.MediaItem;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.Presentation;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.EditedMediaItemSequence;
import androidx.media3.transformer.Effects;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.Transformer;

import com.example.atividadesfisicas.modelo.Multimedia;
import com.example.atividadesfisicas.modelo.Video;
import com.example.atividadesfisicas.utilitarios.Config;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UC9: junta todas as fotos e vídeos do exercício, por ordem cronológica, num único vídeo
 * vertical de, no máximo, 2 minutos (Media3 Transformer). O vídeo resumo fica sem som.
 * TEM de ser chamado na thread principal.
 */
@OptIn(markerClass = UnstableApi.class)
public final class GeradorVideoResumo {

    private static final int LARGURA = 1080;
    private static final int ALTURA = 1920;

    /** Avisa quem pediu o vídeo resumo quando ele termina (com a duração total). */
    public interface AoTerminar {
        void onConcluido(long duracaoMs);

        void onErro(Exception e);
    }

    // guarda os exportadores em curso para não serem apagados pelo garbage collector a meio do trabalho
    private static final List<Transformer> EM_CURSO = new ArrayList<>();

    private GeradorVideoResumo() {
    }

    /** Gera o vídeo resumo. Só deve ser chamado se houver pelo menos uma captura. */
    public static void gerar(Context ctx, List<Multimedia> capturas, File saida, final AoTerminar aoTerminar) {
        List<Multimedia> ordenadas = new ArrayList<>(capturas);
        Collections.sort(ordenadas, (a, b) -> Long.compare(a.getInstante(), b.getInstante()));

        // reparte os 2 minutos pelas capturas, para a soma nunca ultrapassar o máximo
        long maximoPorItem = Config.VIDEO_RESUMO_MAX_MS / ordenadas.size();
        long total = 0;
        List<EditedMediaItem> itens = new ArrayList<>();
        for (Multimedia captura : ordenadas) {
            Uri endereco = Uri.fromFile(new File(captura.getCaminho()));
            // ajusta cada captura a 1080x1920 sem a deformar (faixas pretas se a proporção for diferente)
            Effects efeitos = new Effects(ImmutableList.<AudioProcessor>of(),
                    ImmutableList.<Effect>of(Presentation.createForWidthAndHeight(
                            LARGURA, ALTURA, Presentation.LAYOUT_SCALE_TO_FIT)));
            long duracao;
            if (captura instanceof Video) {
                duracao = ((Video) captura).getDuracaoMs();
                if (duracao <= 0) {
                    duracao = Config.DURACAO_FOTO_NO_RESUMO_MS;
                }
                duracao = Math.min(duracao, maximoPorItem);
                MediaItem video = new MediaItem.Builder()
                        .setUri(endereco)
                        .setClippingConfiguration(new MediaItem.ClippingConfiguration.Builder()
                                .setEndPositionMs(duracao)
                                .build())
                        .build();
                itens.add(new EditedMediaItem.Builder(video)
                        .setRemoveAudio(true)
                        .setEffects(efeitos)
                        .build());
            } else {
                // uma foto não tem duração: fica DURACAO_FOTO_NO_RESUMO_MS no ecrã, a 30 imagens por segundo
                duracao = Math.min(Config.DURACAO_FOTO_NO_RESUMO_MS, maximoPorItem);
                itens.add(new EditedMediaItem.Builder(MediaItem.fromUri(endereco))
                        .setDurationUs(duracao * 1000)
                        .setFrameRate(30)
                        .setRemoveAudio(true)
                        .setEffects(efeitos)
                        .build());
            }
            total += duracao;
        }

        final long duracaoTotal = total;
        Composition composicao = new Composition.Builder(new EditedMediaItemSequence(itens)).build();
        final Transformer[] referencia = new Transformer[1];
        Transformer exportador = new Transformer.Builder(ctx.getApplicationContext())
                .setEncoderFactory(ConfigCodificadorVideo.codificador(ctx))
                .addListener(new Transformer.Listener() {
                    @Override
                    public void onCompleted(Composition composicao, ExportResult resultado) {
                        EM_CURSO.remove(referencia[0]);
                        aoTerminar.onConcluido(duracaoTotal);
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
        exportador.start(composicao, saida.getAbsolutePath());
    }
}
