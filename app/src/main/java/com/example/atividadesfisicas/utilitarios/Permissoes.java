package com.example.atividadesfisicas.utilitarios;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Permissões de runtime usadas pela aplicação.*/

public final class Permissoes {

    private Permissoes() {
    }

    /** Permissões a pedir ao utilizador. */
    public static String[] permissoesPedidas() {
        List<String> lista = new ArrayList<>();
        lista.add(Manifest.permission.ACCESS_FINE_LOCATION);
        lista.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lista.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return lista.toArray(new String[0]);
    }

    public static boolean temLocalizacao(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean temNotificacoes(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }
}