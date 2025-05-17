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

public class Poste extends Equipement {
    private List<Machine> machines;

    public Poste(String ref, String designation, List<Machine> machines) {
        super(ref, designation);
        this.machines = machines;
    }

    public List<Machine> getMachines() {
        return machines;
    }

    @Override
    public String toString() {
        return ref + " (Poste)";
    }
}


