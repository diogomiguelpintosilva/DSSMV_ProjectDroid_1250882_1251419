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
import java.util.Comparator;
import java.util.List;
/**
 * UC9: junta todas as fotos e vídeos da atividade num único vídeo
 de, no máximo, 2 minutos (Media3 Transformer).
 * O vídeo resumo fica sem som. TEM de ser chamado na UI thread.
 */
@OptIn(markerClass = UnstableApi.class)
public final class GeradorVideoResumo {
    private static final int LARGURA = 1080;
    private static final int ALTURA = 1920;
    public interface Listener {
        void onConcluido(long duracaoMs);
        void onErro(Exception e);
    }
    private static final List<Transformer> EM_CURSO = new
            ArrayList<>();
    private GeradorVideoResumo() {
    }
    public static void gerar(Context ctx, List<Multimedia> capturas,
                             File saida, final Listener listener) {
        List<Multimedia> ordenadas = new ArrayList<>(capturas);
        Collections.sort(ordenadas, new Comparator<Multimedia>() {
            @Override
            public int compare(Multimedia a, Multimedia b) {
                return Long.compare(a.getInstante(),
                        b.getInstante());
            }
        });
        // reparte os 2 minutos pelas capturas para nuncaultrapassar o máximo
        long maximoPorItem = Config.VIDEO_RESUMO_MAX_MS /
                ordenadas.size();
        long total = 0;
        List<EditedMediaItem> itens = new ArrayList<>();
        for (Multimedia captura : ordenadas) {
            Uri uri = Uri.fromFile(new File(captura.getCaminho()));
            Effects efeitos = new
                    Effects(ImmutableList.<AudioProcessor>of(),
                    ImmutableList.<Effect>of(Presentation.createForWidthAndHeight(
                            LARGURA, ALTURA,
                            Presentation.LAYOUT_SCALE_TO_FIT)));
            if (captura instanceof Video) {
                long duracao = ((Video) captura).getDuracaoMs();
                if (duracao <= 0) {
                    duracao = Config.DURACAO_FOTO_NO_RESUMO_MS;
                }
                duracao = Math.min(duracao, maximoPorItem);
                MediaItem mediaItem = new MediaItem.Builder()
                        .setUri(uri)
                        .setClippingConfiguration(new
                                MediaItem.ClippingConfiguration.Builder()
                                .setEndPositionMs(duracao)
                                .build())
                        .build();
                itens.add(new EditedMediaItem.Builder(mediaItem)
                        .setRemoveAudio(true)
                        .setEffects(efeitos)
                        .build());
                total += duracao;
            } else {
                long duracao =
                        Math.min(Config.DURACAO_FOTO_NO_RESUMO_MS, maximoPorItem);
                itens.add(new
                        EditedMediaItem.Builder(MediaItem.fromUri(uri))
                        .setDurationUs(duracao * 1000)
                        .setFrameRate(30)
                        .setRemoveAudio(true)
                        .setEffects(efeitos)
                        .build());
                total += duracao;
            }
        }
        final long duracaoTotal = total;
        Composition composicao = new Composition.Builder(new
                EditedMediaItemSequence(itens)).build();
        final Transformer[] referencia = new Transformer[1];
        Transformer transformer = new
                Transformer.Builder(ctx.getApplicationContext())
                .setEncoderFactory(ConfigCodificadorVideo.codificador(ctx))
                .addListener(new Transformer.Listener() {
                    @Override
                    public void onCompleted(Composition composition,
                                            ExportResult exportResult) {
                        EM_CURSO.remove(referencia[0]);
                        listener.onConcluido(duracaoTotal);
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
        transformer.start(composicao, saida.getAbsolutePath());
    }
}