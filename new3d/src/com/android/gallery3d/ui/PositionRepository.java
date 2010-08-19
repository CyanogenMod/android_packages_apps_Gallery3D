package com.android.gallery3d.ui;

import java.util.HashMap;

public class PositionRepository {

    public static class Position implements Cloneable {
        public float x;
        public float y;
        public float z;
        public float theta;
        public float alpha;

        public Position() {
        }

        public Position(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.theta = 0f;
            this.alpha = 1f;
        }

        @Override
        public Position clone() {
            try {
                return (Position) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(); // we do support clone.
            }
        }

        public void set(Position another) {
            x = another.x;
            y = another.y;
            z = another.z;
            theta = another.theta;
            alpha = another.alpha;
        }

        public void set(float x, float y, float z, float theta, float alpha) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.theta = theta;
            this.alpha = alpha;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Position)) return false;
            Position position = (Position) object;
            return x == position.x && y == position.y && z == position.z
                    && theta == position.theta && alpha == position.alpha;
        }

        public static void interpolate(
                Position source, Position target, Position output, float interpolate) {
            if (interpolate < 1f) {
                output.set(interpolateScale(source.x, target.x, interpolate),
                        interpolateScale(source.y, target.y, interpolate),
                        interpolateScale(source.z, target.z, interpolate),
                        interpolateAngle(source.theta, target.theta, interpolate),
                        interpolateScale(source.alpha, target.alpha, interpolate));
            } else {
                output.set(target);
            }
        }
    }

    private HashMap<Long, Position> mData = new HashMap<Long, Position>();
    private float mOffsetX;
    private float mOffsetY;
    private float mOffsetZ;

    public Position get(Long identity) {
        return mData.get(identity);
    }

    public void setPositionOffset(int offsetX, int offsetY, int offsetZ) {
        float deltaX = offsetX - mOffsetX;
        float deltaY = offsetY - mOffsetY;
        float deltaZ = offsetZ - mOffsetZ;
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        mOffsetZ = offsetZ;
        for (Position position : mData.values()) {
            position.x += deltaX;
            position.y += deltaY;
            position.z += deltaZ;
        }
    }

    public void putPosition(Long identity, Position position) {
        mData.put(identity, position);
    }

    public void clear() {
        mData.clear();
    }

    private static float interpolateScale(
            float source, float target, float interpolate) {
        return source + interpolate * (target - source);
    }

    private static float interpolateAngle(
            float source, float target, float interpolate) {
        // interpolate the angle from source to target
        // We make the difference in the range of [-179, 180], this is the
        // shortest path to change source to target.
        float diff = target - source;
        if (diff < 0) diff += 360f;
        if (diff > 180) diff -= 360f;

        float result = source + diff * interpolate;
        return result < 0 ? result + 360f : result;
    }
}
