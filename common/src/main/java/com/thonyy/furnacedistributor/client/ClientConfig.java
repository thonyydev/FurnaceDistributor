package com.thonyy.furnacedistributor.client;

import com.thonyy.furnacedistributor.config.ConfigFile;
import dev.architectury.platform.Platform;

public final class ClientConfig {
    private static ClientConfig instance = new ClientConfig();
    private static ConfigFile<ClientConfig> file;
    public boolean showOutlines = true;
    public boolean showItemPreview = true;
    public boolean showSelectionHud = true;
    public boolean showContextualTips = true;
    public boolean enableUiAnimations = true;
    public int collectionTipSelections;
    public boolean collectionTipLearned;
    public int selectionDisplayTicks = 60;
    public int maxPreviewItems = 64;

    public static void load() {
        file = new ConfigFile<>(Platform.getConfigFolder().resolve("furnacedistributor-client.json"),
                ClientConfig.class, ClientConfig::validate);
        instance = file.load(ClientConfig::new);
    }

    public void validate() {
        selectionDisplayTicks = Math.clamp(selectionDisplayTicks, 0, 1200);
        maxPreviewItems = Math.clamp(maxPreviewItems, 1, 64);
        collectionTipSelections = Math.clamp(collectionTipSelections, 0, 3);
    }

    public static ClientConfig get() { return instance; }
    public static boolean save() { return file != null && file.save(instance); }
}
