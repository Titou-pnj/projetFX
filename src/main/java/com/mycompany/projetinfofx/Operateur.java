/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.projetinfofx;

/**
 *
 * @author titou
 */
import java.util.List;

public class Operateur {
    private String code;
    private String nom;
    private String prenom;
    private List<String> competences;
    private boolean busy = false;

    public Operateur(String code, String nom, String prenom, List<String> competences) {
        this.code = code;
        this.nom = nom;
        this.prenom = prenom;
        this.competences = competences;
    }

    public String getCode() {
        return code;
    }
    public String getNom() {
        return nom;
    }
    public String getPrenom() {
        return prenom;
    }
    public List<String> getCompetences() {
        return competences;
    }
    public boolean isBusy() {
        return busy;
    }
    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    @Override
    public String toString() {
        return code + " - " + nom + " " + prenom + (busy ? " (occupé)" : " (libre)");
    }
}
