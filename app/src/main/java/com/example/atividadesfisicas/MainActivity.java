package com.example.atividadesfisicas;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Encontrar a ListView no layout através do ID
        ListView listView = findViewById(R.id.listView);

        // 2. Criar uma lista de dados (nomes de bateristas)
        String[] drummers = {
                "Buddy Rich",
                "John Bonham",
                "Neil Peart",
                "Mike Portnoy",
                "Dave Grohl",
                "Travis Barker",
                "Ilan Rubin",
                "Vinnie Colaiuta"
        };

        ArrayList<String> drumList = new ArrayList<>(Arrays.asList(drummers));

        // 3. Criar um ArrayAdapter para adaptar os dados à vista da lista
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                drumList
        );

        // 4. Associar o adapter à ListView
        listView.setAdapter(adapter);

        // 5. Tratar o clique em cada item da lista
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDrummer = (String) parent.getItemAtPosition(position);
            Toast.makeText(MainActivity.this, "Selecionado: " + selectedDrummer, Toast.LENGTH_SHORT).show();
        });
    }
}