package com.example.atividadesfisicas.multimedia;

import android.content.Context;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.transformer.Codec;
import androidx.media3.transformer.DefaultEncoderFactory;
import androidx.media3.transformer.VideoEncoderSettings;

import com.example.atividadesfisicas.utilitarios.Config;

/**
 * Configuração do codificador de vídeo do Media3 Transformer. Por omissão o débito é baixo
 * e o vídeo fica pixelado; aqui pede-se um débito alto (Config.VIDEO_BITRATE).
 * Só é usada dentro do pacote multimedia (MetricasVideo e GeradorVideoResumo).
 */
@OptIn(markerClass = UnstableApi.class)
final class ConfigCodificadorVideo {

    private ConfigCodificadorVideo() {
    }

    /** Fábrica de codificadores com o débito pedido. */
    static Codec.EncoderFactory codificador(Context ctx) {
        return new DefaultEncoderFactory.Builder(ctx.getApplicationContext())
                .setRequestedVideoEncoderSettings(new VideoEncoderSettings.Builder()
                        .setBitrate(Config.VIDEO_BITRATE)
                        .build())
                .build();
    }
}
