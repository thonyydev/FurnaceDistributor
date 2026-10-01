package com.thonyy.furnacedistributor.logic;

/** Inclusive bounds; long arithmetic prevents overflow from untrusted packet coordinates. */
public record AreaBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public static final int MAX_FURNACES = 64;
    public static final int MAX_VOLUME = 32768;

    public static AreaBounds between(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new AreaBounds(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    public boolean isWithinVolumeLimit() {
        long x = (long) maxX - minX + 1;
        long y = (long) maxY - minY + 1;
        long z = (long) maxZ - minZ + 1;
        return x > 0 && y > 0 && z > 0 && x <= MAX_VOLUME && y <= MAX_VOLUME
                && z <= MAX_VOLUME && x * y * z <= MAX_VOLUME;
    }

    public boolean isWithinDistance(double x, double y, double z, int range) {
        // The farthest corner bounds every block centre in the box, even for crossed endpoints.
        double dx = Math.max(Math.abs(minX + 0.5 - x), Math.abs(maxX + 0.5 - x));
        double dy = Math.max(Math.abs(minY + 0.5 - y), Math.abs(maxY + 0.5 - y));
        double dz = Math.max(Math.abs(minZ + 0.5 - z), Math.abs(maxZ + 0.5 - z));
        return dx * dx + dy * dy + dz * dz <= (double) range * range;
    }
}
