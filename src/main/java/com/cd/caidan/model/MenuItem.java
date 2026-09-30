package com.cd.caidan.model;

import com.cd.caidan.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单中的一个物品及其点击指令配置。
 */
public class MenuItem {

    private int slot;
    private Material material;
    private String name;
    private List<String> lore = new ArrayList<>();
    private List<CommandEntry> commands = new ArrayList<>();

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore != null ? lore : new ArrayList<>();
    }

    public List<CommandEntry> getCommands() {
        return commands;
    }

    public void setCommands(List<CommandEntry> commands) {
        this.commands = commands != null ? commands : new ArrayList<>();
    }

    /** 渲染为物品展示。isAdmin 时追加左右键操作提示。 */
    public ItemStack toItemStack(boolean isAdmin) {
        List<String> lines = new ArrayList<>(lore);
        if (isAdmin) {
            lines.add("§7§m------------------");
            lines.add("§e左键 §7执行功能");
            lines.add("§e右键 §7进入设置");
        }
        return ItemBuilder.build(material, name, lines);
    }
}
