package com.example.smartcareshake;
import java.io.Serializable;

public class EstadoHistorico implements Serializable {

    private String estado;
    private int ocurrencias;

    public EstadoHistorico(String estado) {
        this.estado = estado;
        this.ocurrencias = 0; // Al principio, la ocurrencia es 1
    }

    public String getEstado() {
        return estado;
    }

    public int getOcurrencias() {
        return ocurrencias;
    }

    // Método para incrementar las ocurrencias
    public void incrementarOcurrencias() {
        this.ocurrencias+=1;
    }

    public void setOcurrencias(int ocurrencias) {
        this.ocurrencias = ocurrencias;
    }
}