package com.cd.caidan.listener;

import com.cd.caidan.CaiDanPlugin;
import com.cd.caidan.config.ConfigManager;
import com.cd.caidan.gui.MenuManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 入口物品（附魔书）管理：
 * force=true 时锁定在指定格子，玩家无法移动、拿起、丢弃、放入铁砧等容器；
 * 点击锁定的入口书或手持入口书右键可打开菜单。
 */
public class EntryItemListener implements Listener {

    private final CaiDanPlugin plugin;
    private final ConfigManager config;
    private final MenuManager menuManager;

    public EntryItemListener(CaiDanPlugin plugin, ConfigManager config, MenuManager menuManager) {
        this.plugin = plugin;
        this.config = config;
        this.menuManager = menuManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!config.isEntryEnabled() || !config.isEntryForce()) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean touchesEntry = config.isEntryItem(clicked) || config.isEntryItem(cursor);
        if (!touchesEntry) {
            return;
        }
        event.setCancelled(true);
        // 纯背包界面（按 E 打开）点击锁定槽位的入口书 → 打开菜单
        if (event.getView().getType() == InventoryType.CRAFTING
                && config.isEntryItem(clicked)
                && event.getSlot() == config.getEntrySlot()
                && (cursor == null || cursor.getType().isAir())) {
            config.ensureEntryItem(player);
            menuManager.openMenu(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!config.isEntryEnabled() || !config.isEntryForce()) {
            return;
        }
        boolean touchesEntry = config.isEntryItem(event.getOldCursor());
        if (!touchesEntry) {
            int entryRaw = event.getView().convertSlot(config.getEntrySlot());
            touchesEntry = event.getRawSlots().contains(entryRaw);
        }
        if (touchesEntry) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!config.isEntryEnabled() || !config.isEntryForce()) {
            return;
        }
        if (config.isEntryItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!config.isEntryItem(hand)) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
        }
        config.ensureEntryItem(player);
        menuManager.openMenu(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        if (config.isEntryEnabled() && config.isEntryForce()) {
            config.ensureEntryItem(event.getPlayer());
        }
    }
}
