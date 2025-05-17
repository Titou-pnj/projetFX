/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.projetinfofx;

/**
 *
 * @author titou
 */

public abstract class Equipement {
    protected String ref;
    protected String designation;

    public Equipement(String ref, String designation) {
        this.ref = ref;
        this.designation = designation;
    }

    public String getRef() {
        return ref;
    }

    public String getDesignation() {
        return designation;
    }

    @Override
    public String toString() {
        return ref + " - " + designation;
    }
}