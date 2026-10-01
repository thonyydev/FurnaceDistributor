package com.thonyy.furnacedistributor.logic;

/** Distributes only available items, redistributing shares that exceed a destination's capacity. */
public final class DistributionPlan {
    public static int[] allocate(int total, int[] capacities, boolean redistribute) {
        int[] amounts = new int[capacities.length];
        if (total <= 0 || capacities.length == 0) return amounts;
        if (!redistribute) {
            int share = total / capacities.length;
            int remainder = total % capacities.length;
            for (int i = 0; i < amounts.length; i++) {
                if (capacities[i] > 0) {
                    amounts[i] = Math.min(capacities[i], share + (remainder > 0 ? 1 : 0));
                    if (remainder > 0) remainder--;
                }
            }
            return amounts;
        }
        boolean[] pending = new boolean[capacities.length];
        for (int i = 0; i < pending.length; i++) pending[i] = capacities[i] > 0;
        int remaining = total;
        while (remaining > 0) {
            int available = 0;
            for (boolean value : pending) if (value) available++;
            if (available == 0) break;
            int share = remaining / available;
            boolean saturated = false;
            for (int i = 0; i < amounts.length; i++) {
                if (pending[i] && capacities[i] <= share) {
                    amounts[i] = capacities[i];
                    remaining -= amounts[i];
                    pending[i] = false;
                    saturated = true;
                }
            }
            // Recalculate after saturating destinations, before assigning remainder items.
            // This keeps unsaturated destinations within one item of each other.
            if (saturated) continue;
            int remainder = remaining % available;
            for (int i = 0; i < amounts.length; i++) {
                if (pending[i]) amounts[i] = share + (remainder-- > 0 ? 1 : 0);
            }
            break;
        }
        return amounts;
    }

    private DistributionPlan() { }
}
