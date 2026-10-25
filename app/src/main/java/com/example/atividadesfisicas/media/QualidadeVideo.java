package com.example.atividadesfisicas.media;

import android.content.Context;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.transformer.Codec;
import androidx.media3.transformer.DefaultEncoderFactory;
import androidx.media3.transformer.VideoEncoderSettings;

import com.example.atividadesfisicas.util.Config;

/**
 * Configuração do codificador de vídeo do Media3 Transformer. Por omissão o débito é baixo
 * e o vídeo fica pixelado; aqui pede-se um débito alto (Config.VIDEO_BITRATE).
 */
@OptIn(markerClass = UnstableApi.class)
final class QualidadeVideo {

    private QualidadeVideo() {
    }

    static Codec.EncoderFactory codificador(Context ctx) {
        return new DefaultEncoderFactory.Builder(ctx.getApplicationContext())
                .setRequestedVideoEncoderSettings(new VideoEncoderSettings.Builder()
                        .setBitrate(Config.VIDEO_BITRATE)
                        .build())
                .build();
    }
}
