/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.projetinfofx;

/**
 *
 * @author titou
 */
public class Machine extends Equipement {
    private String type;
    private float costHourly;
    private float x, y;
    private boolean automatic;
    private boolean operational = true;
    private boolean available = true;

    public Machine(String ref, String designation, String type, float costHourly, float x, float y) {
        super(ref, designation);
        this.type = type;
        this.costHourly = costHourly;
        this.x = x;
        this.y = y;
    }

    public String getType() {
        return type;
    }
    public float getCostHourly() {
        return costHourly;
    }
    public float getX() {
        return x;
    }
    public float getY() {
        return y;
    }
    public boolean isAutomatic() {
        return automatic;
    }
    public void setAutomatic(boolean automatic) {
        this.automatic = automatic;
    }
    public boolean isOperational() {
        return operational;
    }
    public void setOperational(boolean operational) {
        this.operational = operational;
    }
    public boolean isAvailable() {
        return available;
    }
    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return ref + " (" + designation + ")";
    }
}
