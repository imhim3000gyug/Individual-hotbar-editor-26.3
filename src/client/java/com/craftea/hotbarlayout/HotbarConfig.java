package com.craftea.hotbarlayout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HotbarConfig {
	private static final String CONFIG_FILE_NAME = "hotbar_layout_editor.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final Path configDir;
	private final Path configPath;
	private HotbarLayoutData data;

	public HotbarConfig(Path configDir) {
		this.configDir = configDir;
		this.configPath = configDir.resolve(CONFIG_FILE_NAME);
		this.data = new HotbarLayoutData();
	}

	public HotbarLayoutData getData() { return this.data; }

	public void setData(HotbarLayoutData data) {
		this.data = data != null ? data : new HotbarLayoutData();
	}

	public void load() {
		if (!Files.exists(this.configPath)) {
			this.data = new HotbarLayoutData();
			this.save();
			return;
		}
		try {
			String json = Files.readString(this.configPath);
			HotbarLayoutData loaded = GSON.fromJson(json, HotbarLayoutData.class);
			if (loaded != null) {
				this.data = loaded;
			} else {
				System.err.println("[HotbarLayoutEditor] Config file was empty, using defaults.");
				this.data = new HotbarLayoutData();
			}
		} catch (Exception e) {
			System.err.println("[HotbarLayoutEditor] Failed to load config, using defaults: " + e.getMessage());
			this.data = new HotbarLayoutData();
		}
	}

	public void save() {
		try {
			Files.createDirectories(this.configDir);
			Files.writeString(this.configPath, GSON.toJson(this.data));
		} catch (IOException e) {
			System.err.println("[HotbarLayoutEditor] Failed to save config: " + e.getMessage());
		}
	}

	public Path getConfigPath() { return this.configPath; }
}