package com.example.smartcareshake;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class HistoricoActivity extends AppCompatActivity{

    ArrayList barArraylist;
    private static final String TAG = "HistoricoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);

        Map<String, EstadoHistorico> contadorEstados = (Map<String, EstadoHistorico>) getIntent().getSerializableExtra("contadorEstados");

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        Button buttonBack = findViewById(R.id.button_back);
        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(HistoricoActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });


        int index = 0;
        // Iterar sobre el mapa para llenar las entradas del histograma
        for (Map.Entry<String, EstadoHistorico> entry : contadorEstados.entrySet()) {
            // Añadir una nueva barra con el índice y el valor de ocurrencias
            entries.add(new BarEntry(index, entry.getValue().getOcurrencias()));
            labels.add(entry.getKey());
            index++;
        }

        // Ordenar las entradas según las ocurrencias (opcional, según tus necesidades)
        Collections.sort(entries, new Comparator<BarEntry>() {
            @Override
            public int compare(BarEntry e1, BarEntry e2) {
                return Float.compare(e1.getY(), e2.getY());  // Ordenar por ocurrencias
            }
        });

        // Crear un DataSet de barras
        BarDataSet barDataSet = new BarDataSet(entries, "Ocurrencias de Estados");

        // Colores personalizados (variaciones de #A8DAB5)
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#A8DAB5")); // Color base
        colors.add(Color.parseColor("#8FC99E")); // Tonalidad más oscura
        colors.add(Color.parseColor("#C1E1D3")); // Tonalidad más clara
        colors.add(Color.parseColor("#A8DAB5"));
        colors.add(Color.parseColor("#8FC99E"));
        colors.add(Color.parseColor("#C1E1D3"));

        // Asignar los colores al DataSet
        barDataSet.setColors(colors);

        // Crear el objeto BarData con el DataSet
        BarData barData = new BarData(barDataSet);

        // Configurar el gráfico
        BarChart barChart = findViewById(R.id.barchart);
        barChart.setData(barData);

        // Ajustar el ancho de las barras
        barData.setBarWidth(0.5f); // Ajusta el ancho de las barras
        barChart.getXAxis().setAxisMinimum(-0.5f); // Asegurarse de que el eje X empiece desde un valor negativo para evitar que las barras se peguen al borde izquierdo

        barChart.invalidate(); // Actualizar el gráfico

        // Configuración adicional del gráfico (opcional)
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setGranularity(1f); // Asegurar que las etiquetas se muestren sin solaparse
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM); // Colocar etiquetas abajo

        // Configuración del eje Y
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f); // Asegurar que el eje Y empiece desde 0
        leftAxis.setGranularity(1f); // Ajustar la granularidad para los valores en el eje Y
        barChart.getAxisRight().setEnabled(false); // Deshabilitar el eje Y derecho (opcional)
        barChart.getDescription().setEnabled(false);

        // Mejorar la visibilidad y el diseño del gráfico (opcional)
        barDataSet.setValueTextColor(Color.BLACK); // Cambiar el color del texto de los valores
        barDataSet.setValueTextSize(10f); // Ajustar el tamaño del texto de los valores
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
