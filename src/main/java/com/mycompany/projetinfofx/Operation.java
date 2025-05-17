/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.projetinfofx;

/**
 *
 * @author titou
 */
public class Operation {
    private String ref;
    private String designation;
    private Equipement equipement;
    private float duree;

    public Operation(String ref, String designation, Equipement equipement, float duree) {
        this.ref = ref;
        this.designation = designation;
        this.equipement = equipement;
        this.duree = duree;
    }

    public String getRef() { return ref; }
    public String getDesignation() { return designation; }
    public Equipement getEquipement() { return equipement; }
    public float getDuree() { return duree; }

    @Override
    public String toString() {
        return ref + " – " + designation;
    }
}


