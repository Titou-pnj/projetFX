/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.projetinfofx;

/**
 *
 * @author titou
 */
public class Produit {
    private String code;
    private String designation;

    public Produit(String code, String designation) {
        this.code = code;
        this.designation = designation;
    }

    public String getCode() { return code; }
    public String getDesignation() { return designation; }

    public void afficheProduit() {
        System.out.println("Produit: " + code + ", " + designation);
    }

    public void modifierProduit(String code, String designation) {
        this.code = code;
        this.designation = designation;
    }

    public void supprimerProduit() {
        System.out.println("Produit " + code + " supprimé.");
    }
}
