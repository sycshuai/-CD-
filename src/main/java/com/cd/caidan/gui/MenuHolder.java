package com.cd.caidan.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 菜单 / 设置界面的 InventoryHolder，用于在点击事件中还原上下文。
 */
public class MenuHolder implements InventoryHolder {

    public enum Type {
        MENU, SETTINGS
    }

    private Inventory inventory;
    private final Type type;
    private final int pageIndex;
    private final boolean editMode; // MENU 类型时有效：是否处于放置模式
    private final int itemSlot;     // SETTINGS 类型时有效：被设置的物品槽位

    public MenuHolder(Inventory inventory, Type type, int pageIndex, boolean editMode, int itemSlot) {
        this.inventory = inventory;
        this.type = type;
        this.pageIndex = pageIndex;
        this.editMode = editMode;
        this.itemSlot = itemSlot;
    }

    /** 创建 Inventory 后回填引用（createInventory 需要先传入 holder）。 */
    public void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Type getType() {
        return type;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public int getItemSlot() {
        return itemSlot;
    }
}
