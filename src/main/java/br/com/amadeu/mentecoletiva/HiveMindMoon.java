package br.com.amadeu.mentecoletiva;

import net.minecraft.server.level.ServerLevel;

public final class HiveMindMoon {
    private HiveMindMoon() {}

    public static boolean isNight(ServerLevel world) {
        long t = world.getGameTime() % 24000L;
        return t >= 13000L && t <= 23000L;
    }

    public static int getMoonPhase(ServerLevel world) {
        long day = world.getGameTime() / 24000L;
        return (int) (day % 8L);
    }

    public static boolean isFullMoon(ServerLevel world) {
        return isNight(world) && getMoonPhase(world) == 0;
    }
}