package it.hurts.metallurgy_reforged.effect;

/** Source1.4.1 missing-health curve, with issue388's attack ownership guard. */
public final class DivineGrace {
    public static float damage(float original, float health, float maximumHealth,
                               boolean qualifyingWeapon, boolean directMelee) {
        if (!qualifyingWeapon || !directMelee || maximumHealth <= 0) return original;
        float missing = Math.max(0, Math.min(1, 1 - health / maximumHealth));
        return original + original * missing * 2;
    }

    private DivineGrace() {}
}
