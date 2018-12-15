package com.example.jhbra.android_project;

public enum Transportation {

    WALK(0, "walk", 3F),
    CAR(1, "car", 40F),
    BUS(2, "bus", 25F);

    private final int num;
    private final String dbText;
    private final float velocity;

    Transportation(int num, String dbText, float velocity) {
        this.num = num;
        this.dbText = dbText;
        this.velocity = velocity;
    }

    public int getNum() {
        return num;
    }

    public String getDBText() {
        return dbText;
    }

    public float getVelocity() {
        return velocity;
    }

}
