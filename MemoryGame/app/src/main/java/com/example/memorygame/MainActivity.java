package com.example.memorygame;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private TextView tv_puntaje, tv_nivel;

    private String[] ultimos_cuadros = {"vacio", "vacio"};
    private String[] ultimos_keys = {"", ""};
    private int idx_actual = 0;
    private HashMap<String, Button> nombres_botones = new HashMap<String, Button>();
    private HashMap<String, Integer> nombre_boton_id_img = new HashMap<String, Integer>();
    private int puntaje = 0;
    private int nivel = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nombres_botones.put("perro1", (Button) findViewById(R.id.botonPerro1));
        nombres_botones.put("perro2", (Button) findViewById(R.id.botonPerro2));
        nombres_botones.put("gato1", (Button) findViewById(R.id.botonGato1));
        nombres_botones.put("gato2", (Button) findViewById(R.id.botonGato2));

        nombre_boton_id_img.put("perro", R.drawable.dog);
        nombre_boton_id_img.put("gato", R.drawable.cat);

        tv_puntaje = (TextView) findViewById(R.id.textViewPuntaje);
        tv_nivel = (TextView) findViewById(R.id.textViewNivel);

        tv_puntaje.setText(String.valueOf(puntaje));
        tv_nivel.setText(String.valueOf(nivel));
    }

    private boolean cuadros_iguales() {
        if (ultimos_cuadros[0].equals(ultimos_cuadros[1])) {
            return true;
        }
        else {
            return false;
        }
    }

    private void actualizar_idx_actual() {
        if (idx_actual == 0) {
            idx_actual = 1;
        }
        else {
            idx_actual = 0;
        }
    }

    private void apagar_todos_botones() {
        for (Button boton : nombres_botones.values()) {
            boton.setClickable(false);
        }
    }

    private void encender_todos_botones() {
        for (Button boton : nombres_botones.values()) {
            boton.setClickable(true);
        }
    }

    private void mostrar_cuadro_actual() {
        Button boton = nombres_botones.get(ultimos_keys[idx_actual]);
        boton.setBackground(ContextCompat.getDrawable(this, nombre_boton_id_img.get(ultimos_cuadros[idx_actual])));
    }
    
    private void  ocultar_ambos_cuadros() {
        Button boton1 = nombres_botones.get(ultimos_keys[0]);
        boton1.setBackground(ContextCompat.getDrawable(this, R.drawable.question));
        Button boton2 = nombres_botones.get(ultimos_keys[1]);
        boton2.setBackground(ContextCompat.getDrawable(this, R.drawable.question));
    }

    private void apagar_cuadro_actual() {
        Button boton = nombres_botones.get(ultimos_keys[idx_actual]);
        boton.setClickable(false);
    }

    private void eliminar_ambos_cuadros() {
        nombres_botones.remove(ultimos_keys[0]);
        nombres_botones.remove(ultimos_keys[1]);
    }

    private void actualizar_puntaje() {
        puntaje += 10;
        tv_puntaje.setText(String.valueOf(puntaje));
    }

    private void procesar_boton(String nombre_id, String nombre_clase) {
        ultimos_cuadros[idx_actual] = nombre_clase;
        ultimos_keys[idx_actual] = nombre_id;
        if (idx_actual == 1) {
            boolean iguales = cuadros_iguales();
            if (iguales == true) {
                mostrar_cuadro_actual();
                apagar_cuadro_actual();
                eliminar_ambos_cuadros();
                actualizar_puntaje();
                Toast.makeText(this, "Jugada correcta", Toast.LENGTH_SHORT).show();

                if (puntaje == 20) {
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent i = new Intent(getApplicationContext(), MainActivity2.class);
                            i.putExtra("puntaje", puntaje);
                            startActivity(i);
                        }
                    }, 1000);
                }
            }
            else {
                mostrar_cuadro_actual();
                apagar_todos_botones();
                Toast.makeText(this, "Jugada incorrecta", Toast.LENGTH_SHORT).show();
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ocultar_ambos_cuadros();
                        encender_todos_botones();
                    }
                }, 1000);
            }
        }
        else {
            apagar_cuadro_actual();
            mostrar_cuadro_actual();
        }
    }

    public void cuadro_presionado(View view) {
        switch (view.getId()) {
            case R.id.botonPerro1:
                procesar_boton("perro1", "perro");
                break;
            case R.id.botonPerro2:
                procesar_boton("perro2", "perro");
                break;
            case R.id.botonGato1:
                procesar_boton("gato1", "gato");
                break;
            case R.id.botonGato2:
                procesar_boton("gato2", "gato");
                break;
        }
        actualizar_idx_actual();
    }
}