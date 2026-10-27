package com.example.atividadesfisicas.ecras;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.atividadesfisicas.R;
import com.example.atividadesfisicas.dados.GestorExercicio;
import com.example.atividadesfisicas.modelo.Foto;
import com.example.atividadesfisicas.modelo.Telemetria;
import com.example.atividadesfisicas.modelo.Video;
import com.example.atividadesfisicas.modelo.VideoComTelemetria;
import com.example.atividadesfisicas.multimedia.MetricasFoto;
import com.example.atividadesfisicas.multimedia.MetricasVideo;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * UC7 e UC8: tirar fotos e gravar vídeos durante o exercício com a câmara do sistema,
 * e acrescentar-lhes as métricas do momento. Usada pela ExercicioActivity.
 */
final class CapturaMultimedia {

    private final AppCompatActivity ecra;
    private final GestorExercicio gestor = GestorExercicio.getInstance();
    private final ActivityResultLauncher<Uri> lancadorFoto;
    private final ActivityResultLauncher<Uri> lancadorVideo;

    // capturas ainda a ser processadas: o exercício não pode terminar até acabarem
    private int processamentosPendentes = 0;
    private File ficheiroPendente;
    private Telemetria telemetriaPendente;

    /** Pede ao Android a gravação de vídeo na qualidade máxima (por omissão pode ser baixa). */
    private static class CapturarVideoQualidadeMaxima extends ActivityResultContracts.CaptureVideo {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull Uri input) {
            Intent intent = super.createIntent(context, input);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            return intent;
        }
    }

    /**
     * Tem de ser criada junto com o ecrã (como atributo da Activity), porque regista os pedidos
     * à câmara e o Android só o permite antes de o ecrã ficar visível.
     */
    CapturaMultimedia(AppCompatActivity ecra) {
        this.ecra = ecra;
        lancadorFoto = ecra.registerForActivityResult(
                new ActivityResultContracts.TakePicture(), sucesso -> aoFotoTirada(sucesso));
        lancadorVideo = ecra.registerForActivityResult(
                new CapturarVideoQualidadeMaxima(), sucesso -> aoVideoGravado(sucesso));
    }

    /** Há fotos ou vídeos ainda a ser processados? */
    boolean temProcessamentosPendentes() {
        return processamentosPendentes > 0;
    }

    // ------------------------------------------------------------------ UC7 Tirar foto

    /** Abre a câmara para tirar uma foto. */
    void tirarFoto() {
        if (!gestor.isAtiva()) {
            return;
        }
        File ficheiro = novoFicheiro("foto_", ".jpg", Environment.DIRECTORY_PICTURES);
        ficheiroPendente = ficheiro;
        // as métricas da foto são as deste momento, não as de quando a câmara terminar
        telemetriaPendente = gestor.getTelemetriaAtual();
        lancadorFoto.launch(uriDe(ficheiro));
    }

    private void aoFotoTirada(boolean sucesso) {
        final File ficheiro = ficheiroPendente;
        final Telemetria telemetria = telemetriaPendente;
        ficheiroPendente = null;
        if (ficheiro == null) {
            return;
        }
        if (!sucesso) {
            ficheiro.delete();
            return;
        }

        processamentosPendentes++;
        // desenhar numa foto grande demora: faz-se numa thread de fundo
        new Thread(() -> {
            boolean comMetricas = true;
            try {
                MetricasFoto.aplicar(ficheiro, telemetria);
            } catch (IOException e) {
                comMetricas = false;
            }
            final boolean ok = comMetricas;
            ecra.runOnUiThread(() -> {
                processamentosPendentes--;
                gestor.adicionarMultimedia(new Foto(ficheiro.getAbsolutePath(), System.currentTimeMillis()));
                Toast.makeText(ecra, ok ? R.string.atividade_foto_guardada : R.string.atividade_foto_sem_metricas,
                        Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    // ------------------------------------------------------------------ UC8 Gravar vídeo com telemetria

    /** Abre a câmara para gravar um vídeo. */
    void gravarVideo() {
        if (!gestor.isAtiva()) {
            return;
        }
        File ficheiro = novoFicheiro("video_", ".mp4", Environment.DIRECTORY_MOVIES);
        ficheiroPendente = ficheiro;
        lancadorVideo.launch(uriDe(ficheiro));
    }

    private void aoVideoGravado(boolean sucesso) {
        final File original = ficheiroPendente;
        ficheiroPendente = null;
        if (original == null) {
            return;
        }
        if (!sucesso || !original.exists() || original.length() == 0) {
            original.delete();
            return;
        }

        // o vídeo acabou agora; o início calcula-se a partir da duração
        final long fim = System.currentTimeMillis();
        final long duracao = MetricasVideo.duracaoMs(original);
        final long inicio = fim - duracao;
        final List<Telemetria> amostras = gestor.copiaTelemetria();
        final File saida = novoFicheiro("video_tel_", ".mp4", Environment.DIRECTORY_MOVIES);

        processamentosPendentes++;
        Toast.makeText(ecra, R.string.atividade_video_a_processar, Toast.LENGTH_SHORT).show();

        try {
            MetricasVideo.processar(ecra, original, saida, amostras, inicio, new MetricasVideo.AoTerminar() {
                @Override
                public void onConcluido() {
                    processamentosPendentes--;
                    gestor.adicionarMultimedia(new VideoComTelemetria(saida.getAbsolutePath(), fim, duracao));
                    original.delete();
                    Toast.makeText(ecra, R.string.atividade_video_guardado, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onErro(Exception e) {
                    guardarVideoSemMetricas(original, fim, duracao);
                }
            });
        } catch (RuntimeException e) {
            guardarVideoSemMetricas(original, fim, duracao);
        }
    }

    private void guardarVideoSemMetricas(File original, long fim, long duracao) {
        processamentosPendentes--;
        gestor.adicionarMultimedia(new Video(original.getAbsolutePath(), fim, duracao));
        Toast.makeText(ecra, R.string.atividade_video_sem_metricas, Toast.LENGTH_LONG).show();
    }

    // ------------------------------------------------------------------ ficheiros

    /**
     * Cria um ficheiro vazio na pasta privada da app (não aparece na galeria e não precisa de
     * permissões), numa subpasta por exercício.
     */
    private File novoFicheiro(String prefixo, String sufixo, String tipoPasta) {
        File base = ecra.getExternalFilesDir(tipoPasta);
        if (base == null) {
            base = ecra.getFilesDir();
        }
        File pasta = new File(base, "atividade_" + gestor.getAtual().getDataHoraInicio());
        pasta.mkdirs();
        File ficheiro = new File(pasta, prefixo + System.currentTimeMillis() + sufixo);
        try {
            ficheiro.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ficheiro;
    }

    /** Endereço content:// do ficheiro, para a app da câmara o poder escrever (FileProvider). */
    private Uri uriDe(File ficheiro) {
        return FileProvider.getUriForFile(ecra, ecra.getPackageName() + ".fileprovider", ficheiro);
    }
}
