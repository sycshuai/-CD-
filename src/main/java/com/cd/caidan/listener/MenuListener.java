package com.cd.caidan.listener;

import com.cd.caidan.gui.MenuHolder;
import com.cd.caidan.gui.MenuManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;

/**
 * 菜单 / 设置界面的点击处理。
 *
 * 注意：InventoryClickEvent#getInventory() 返回的是视图的顶部 Inventory，
 * 不能用来判断点击是否落在菜单区域；必须用 rawSlot 与顶部槽位数比较。
 * 顶部（菜单区域）点击一律拦截处理，底部（玩家物品栏）点击放行，
 * 否则玩家无法从物品栏拿起物品（放置模式中无法拿起物品替换红色羊毛）。
 */
public class MenuListener implements Listener {

    private final MenuManager menuManager;

    public MenuListener(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryView view = event.getView();
        int rawSlot = event.getRawSlot();
        int topSize = view.getTopInventory().getSize();
        // 只处理顶部菜单区域；底部玩家物品栏允许正常拿起/整理物品
        if (rawSlot < 0 || rawSlot >= topSize) {
            return;
        }
        InventoryHolder holder = view.getTopInventory().getHolder();
        if (!(holder instanceof MenuHolder menuHolder)) {
            return;
        }
        event.setCancelled(true);
        menuManager.handleClick(player, menuHolder, rawSlot, event.getClick());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryView view = event.getView();
        if (!(view.getTopInventory().getHolder() instanceof MenuHolder)) {
            return;
        }
        int topSize = view.getTopInventory().getSize();
        // 拖拽只要涉及顶部菜单区域就拦截，防止物品被拖入菜单
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < topSize) {
                event.setCancelled(true);
                player.sendMessage("§e请先点击拿起物品，再点击红色羊毛位置放置");
                return;
            }
        }
    }
}
