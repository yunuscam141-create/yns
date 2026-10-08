package dev.yns;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class YnsClient implements ClientModInitializer {
    public static final YnsStore STORE = new YnsStore();
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        STORE.load();
        // ab 1.21.9 braucht ein KeyBinding eine Category (Sprachschluessel: key.category.yns.main)
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.yns.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                KeyBinding.Category.create(Identifier.of("yns", "main"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new YnsScreen(null, STORE));
            }
        });
    }
}
