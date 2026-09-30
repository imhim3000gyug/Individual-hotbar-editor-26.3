package com.craftea.hotbarlayout;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class HotbarLayoutMod implements ClientModInitializer {
	public static final String MOD_ID = "hotbar_layout_editor";
	private static HotbarConfig config;
	private static KeyMapping openEditorKey;

	@Override
	public void onInitializeClient() {
		config = new HotbarConfig(FabricLoader.getInstance().getConfigDir());
		config.load();
		openEditorKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.hotbar_layout_editor.open_editor",
				InputConstants.Type.KEYBOARD,
				72,
				KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "category"))));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (openEditorKey.consumeClick()) openEditorScreen(client);
		});
	}

	public static void openEditorScreen(Minecraft client) {
		if (client != null) client.gui.setScreen(new HotbarEditorScreen(client.gui.screen()));
	}

	public static HotbarConfig getConfig() { return config; }
}