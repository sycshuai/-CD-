package com.cd.caidan.listener;

import com.cd.caidan.CaiDanPlugin;
import com.cd.caidan.gui.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 在线编辑的聊天输入处理：命名、指令、执行身份、重命名等。
 * AsyncPlayerChatEvent 在异步线程触发，编辑逻辑统一调度回主线程执行。
 */
public class ChatListener implements Listener {

    private final CaiDanPlugin plugin;
    private final MenuManager menuManager;

    public ChatListener(CaiDanPlugin plugin, MenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (menuManager.hasSession(player.getUniqueId())) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin,
                    () -> menuManager.handleChat(player, event.getMessage()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        menuManager.clearSession(event.getPlayer());
    }
}
