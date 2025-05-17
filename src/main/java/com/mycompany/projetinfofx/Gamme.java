/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.projetinfofx;

/**
 *
 * @author titou
 */
import java.util.ArrayList;
import java.util.List;

public class Gamme {
    private String ref;
    private List<Operation> operations = new ArrayList<>();

    public Gamme(String ref) {
        this.ref = ref;
    }

    public String getRef() {
        return ref;
    }

    public void creerGamme(Operation op) {
        operations.add(op);
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public float dureeGamme() {
        float sum = 0;
        for (Operation op : operations) sum += op.getDuree();
        return sum;
    }

    public float coutGamme() {
        float total = 0;
        for (Operation op : operations) {
            if (op.getEquipement() instanceof Machine) {
                Machine m = (Machine) op.getEquipement();
                total += m.getCostHourly() * op.getDuree();
            }
        }
        return total;
    }

    @Override
    public String toString() {
        return ref;
    }
}
