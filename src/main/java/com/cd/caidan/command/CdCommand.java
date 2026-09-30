package com.cd.caidan.command;

import com.cd.caidan.CaiDanPlugin;
import com.cd.caidan.config.ConfigManager;
import com.cd.caidan.gui.MenuHolder;
import com.cd.caidan.gui.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /cd 命令：打开菜单、重载配置、帮助。
 */
public class CdCommand implements CommandExecutor {

    private final CaiDanPlugin plugin;
    private final ConfigManager config;
    private final MenuManager menuManager;

    public CdCommand(CaiDanPlugin plugin, ConfigManager config, MenuManager menuManager) {
        this.plugin = plugin;
        this.config = config;
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c该命令只能由玩家执行");
            return true;
        }
        if (args.length == 0) {
            if (!player.hasPermission("cd.use")) {
                player.sendMessage("§c你没有权限使用 [CD] 菜单");
                return true;
            }
            config.ensureEntryItem(player);
            menuManager.openMenu(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(player);
            case "reload" -> {
                if (!menuManager.isAdmin(player)) {
                    player.sendMessage("§c你没有权限执行该操作（需要 OP）");
                    return true;
                }
                config.load();
                menuManager.clearAllSessions();
                closeOpenMenus();
                player.sendMessage("§a[CD] 配置已重载");
            }
            default -> player.sendMessage("§c用法: /cd [help|reload]");
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§e§l[CD] 菜单插件 v1.0");
        player.sendMessage(" §7/cd §f- 打开菜单");
        player.sendMessage(" §7/cd help §f- 查看帮助");
        if (menuManager.isAdmin(player)) {
            player.sendMessage(" §7/cd reload §f- 重载配置文件");
            player.sendMessage(" §7管理员编辑 §f- 打开菜单后点击底部的书与笔进入放置模式");
            player.sendMessage("  §7左键物品=执行功能，右键物品=进入设置");
        }
    }

    /** 重载后强制关闭所有已打开的菜单，避免显示旧数据。 */
    private void closeOpenMenus() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) {
                online.closeInventory();
                online.sendMessage("§e[CD] 配置已重载，请重新打开菜单");
            }
        }
    }
}
