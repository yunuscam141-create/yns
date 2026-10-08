package dev.yns;

import net.minecraft.client.MinecraftClient;

public final class MinecraftClientAccess {
    private MinecraftClientAccess() {}

    public static void execute(Runnable task) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) client.execute(task);
    }
}
