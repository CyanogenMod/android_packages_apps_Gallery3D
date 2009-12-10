package com.cooliris.media;

public final class Vector3f {
    public float x;
    public float y;
    public float z;

    public Vector3f() {

    }

    public Vector3f(float x, float y, float z) {
        set(x, y, z);
    }

    public Vector3f(Vector3f vector) {
        x = vector.x;
        y = vector.y;
        z = vector.z;
    }

    public void set(Vector3f vector) {
        x = vector.x;
        y = vector.y;
        z = vector.z;
    }

    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void add(Vector3f vector) {
        x += vector.x;
        y += vector.y;
        z += vector.z;
    }

    public void subtract(Vector3f vector) {
        x -= vector.x;
        y -= vector.y;
        z -= vector.z;
    }

    public boolean equals(Vector3f vector) {
        if (x == vector.x && y == vector.y && z == vector.z)
            return true;
        return false;
    }

    @Override
    public String toString() {
        return (new String("(" + x + ", " + y + ", " + z + ")"));
    }
}
