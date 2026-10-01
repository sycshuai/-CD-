package com.cd.caidan;

import com.cd.caidan.command.CdCommand;
import com.cd.caidan.config.ConfigManager;
import com.cd.caidan.gui.MenuManager;
import com.cd.caidan.listener.ChatListener;
import com.cd.caidan.listener.EntryItemListener;
import com.cd.caidan.listener.MenuListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * [CD] 菜单插件主类 - Paper 1.21.11
 */
public class CaiDanPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MenuManager menuManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.init();

        menuManager = new MenuManager(this, configManager);

        // 注册 BungeeCord 消息通道：支持通过代理换服（server 指令）
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getCommand("cd").setExecutor(new CdCommand(this, configManager, menuManager));

        getServer().getPluginManager().registerEvents(new MenuListener(menuManager), this);
        getServer().getPluginManager().registerEvents(new EntryItemListener(this, configManager, menuManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, menuManager), this);

        getLogger().info("[CD] 菜单插件已启用 (Paper 1.21.11, v1.3)");
    }

    @Override
    public void onDisable() {
        if (menuManager != null) {
            menuManager.clearAllSessions();
        }
        getLogger().info("[CD] 菜单插件已禁用");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }
}
