package com.cd.caidan.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品构建工具：统一处理颜色代码转换。
 */
public final class ItemBuilder {

    private ItemBuilder() {
    }

    public static ItemStack build(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isEmpty()) {
                meta.setDisplayName(color(name));
            }
            if (lore != null) {
                List<String> colored = new ArrayList<>();
                for (String line : lore) {
                    colored.add(color(line));
                }
                meta.setLore(colored);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /** 将 & 颜色代码转为 Minecraft 颜色代码，并按可见字符截断 GUI 标题到 32 字符（不切断颜色代码序列）。 */
    public static String truncateTitle(String title) {
        String colored = color(title);
        StringBuilder sb = new StringBuilder();
        int visible = 0;
        for (int i = 0; i < colored.length(); i++) {
            char c = colored.charAt(i);
            if (c == ChatColor.COLOR_CHAR && i + 1 < colored.length()) {
                sb.append(c).append(colored.charAt(++i)); // 颜色代码不计入可见长度
                continue;
            }
            if (visible >= 32) {
                break;
            }
            sb.append(c);
            visible++;
        }
        return sb.toString();
    }

    public static String stripColor(String text) {
        return ChatColor.stripColor(color(text));
    }
}
