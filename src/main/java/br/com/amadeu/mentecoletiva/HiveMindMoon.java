package br.com.amadeu.mentecoletiva;

import net.minecraft.server.world.ServerWorld;

public final class HiveMindMoon {
    private HiveMindMoon() {}

    public static boolean isNight(ServerWorld world) {
        long t = world.getTimeOfDay() % 24000L;
        return t >= 13000L && t <= 23000L;
    }

    public static int getMoonPhase(ServerWorld world) {
        long day = world.getTimeOfDay() / 24000L;
        return (int)(day % 8L);
    }

    public static boolean isFullMoon(ServerWorld world) {
        return isNight(world) && getMoonPhase(world) == 0;
    }
}
