package com.cd.caidan.config;

import com.cd.caidan.CaiDanPlugin;
import com.cd.caidan.model.CommandEntry;
import com.cd.caidan.model.MenuItem;
import com.cd.caidan.model.MenuPage;
import com.cd.caidan.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * 配置管理：加载 / 保存 / 重载 config.yml，并维护内存中的菜单数据。
 */
public class ConfigManager {

    private static final int MIN_ROWS = 1;
    private static final int MAX_ROWS = 6;

    private final CaiDanPlugin plugin;
    private final File file;
    private final NamespacedKey entryKey;

    private YamlConfiguration config;

    private int rows = 3;

    private boolean entryEnabled = true;
    private boolean entryForce = false;
    private int entrySlot = 8;
    private Material entryMaterial = Material.ENCHANTED_BOOK;
    private String entryName = "&b&l[CD] &r&e菜单入口";
    private List<String> entryLore = List.of("&7输入 &f/cd &7打开菜单");

    private final List<MenuPage> pages = new ArrayList<>();

    public ConfigManager(CaiDanPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "config.yml");
        this.entryKey = new NamespacedKey(plugin, "cd-entry");
    }

    /** 首次启用：写出默认配置并加载。 */
    public void init() {
        if (!file.exists()) {
            plugin.saveResource("config.yml", false);
        }
        load();
    }

    public void load() {
        config = YamlConfiguration.loadConfiguration(file);
        rows = Math.max(MIN_ROWS, Math.min(MAX_ROWS, config.getInt("rows", 3)));

        entryEnabled = config.getBoolean("entry.enabled", true);
        entryForce = config.getBoolean("entry.force", false);
        entrySlot = config.getInt("entry.slot", 8);
        entryMaterial = Material.matchMaterial(config.getString("entry.material", "ENCHANTED_BOOK"));
        if (entryMaterial == null) {
            entryMaterial = Material.ENCHANTED_BOOK;
        }
        entryName = config.getString("entry.name", "&b&l[CD] &r&e菜单入口");
        entryLore = config.getStringList("entry.lore");
        if (entryLore.isEmpty()) {
            entryLore = List.of("&7输入 &f/cd &7打开菜单");
        }

        pages.clear();
        // pages / items / commands 在配置中是 YAML 列表（- 开头），
        // 必须用 getMapList 读取；getConfigurationSection 只支持 Map 节点，
        // 对 List 返回 null，会导致 reload 后菜单被重置为初始状态。
        for (Map<?, ?> pageMap : config.getMapList("pages")) {
            MenuPage page = new MenuPage();
            page.setTitle(asString(pageMap.get("title"), "&e&l[CD] 菜单"));
            loadItems(pageMap, page);
            pages.add(page);
        }
        if (pages.isEmpty()) {
            MenuPage page = new MenuPage();
            page.setTitle("&e&l[CD] 菜单");
            pages.add(page);
        }
    }

    private void loadItems(Map<?, ?> pageMap, MenuPage page) {
        Object itemsObj = pageMap.get("items");
        if (!(itemsObj instanceof List)) {
            return;
        }
        for (Object itemObj : (List<?>) itemsObj) {
            if (!(itemObj instanceof Map<?, ?> itemMap)) {
                continue;
            }
            MenuItem item = new MenuItem();
            int slot = asInt(itemMap.get("slot"), -1);
            if (slot < 0 || slot >= getTotalSlots() || isNavigationSlot(slot)) {
                plugin.getLogger().warning("配置项 pages.items 的槽位 " + slot
                        + " 无效或与导航栏冲突，已跳过");
                continue;
            }
            Material material = Material.matchMaterial(asString(itemMap.get("material"), ""));
            if (material == null) {
                plugin.getLogger().warning("配置项 pages.items 的材质无效，已跳过");
                continue;
            }
            item.setSlot(slot);
            item.setMaterial(material);
            item.setName(asString(itemMap.get("name"), ""));
            item.setLore(asStringList(itemMap.get("lore")));
            item.setCommands(loadCommands(itemMap.get("commands")));
            page.getItems().add(item);
        }
    }

    private List<CommandEntry> loadCommands(Object commandsObj) {
        List<CommandEntry> commands = new ArrayList<>();
        if (!(commandsObj instanceof List)) {
            return commands;
        }
        for (Object cmdObj : (List<?>) commandsObj) {
            if (!(cmdObj instanceof Map<?, ?> cmdMap)) {
                continue;
            }
            String type = asString(cmdMap.get("type"), "player");
            String command = asString(cmdMap.get("command"), "");
            if (!command.isEmpty()) {
                commands.add(new CommandEntry(type, command));
            }
        }
        return commands;
    }

    private String asString(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private List<String> asStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object line : (List<?>) value) {
                result.add(String.valueOf(line));
            }
        }
        return result;
    }

    /** 把内存中的菜单数据写回 config.yml（在线编辑后调用）。 */
    public void save() {
        YamlConfiguration out = new YamlConfiguration();
        out.set("rows", rows);
        out.set("entry.enabled", entryEnabled);
        out.set("entry.force", entryForce);
        out.set("entry.slot", entrySlot);
        out.set("entry.material", entryMaterial.name());
        out.set("entry.name", entryName);
        out.set("entry.lore", entryLore);

        List<Map<String, Object>> pageList = new ArrayList<>();
        for (MenuPage page : pages) {
            Map<String, Object> pageMap = new LinkedHashMap<>();
            pageMap.put("title", page.getTitle());
            List<Map<String, Object>> itemList = new ArrayList<>();
            for (MenuItem item : page.getItems()) {
                Map<String, Object> itemMap = new LinkedHashMap<>();
                itemMap.put("slot", item.getSlot());
                itemMap.put("material", item.getMaterial().name());
                itemMap.put("name", item.getName());
                itemMap.put("lore", item.getLore());
                List<Map<String, Object>> cmdList = new ArrayList<>();
                for (CommandEntry ce : item.getCommands()) {
                    Map<String, Object> cmdMap = new LinkedHashMap<>();
                    cmdMap.put("type", ce.getType());
                    cmdMap.put("command", ce.getCommand());
                    cmdList.add(cmdMap);
                }
                itemMap.put("commands", cmdList);
                itemList.add(itemMap);
            }
            pageMap.put("items", itemList);
            pageList.add(pageMap);
        }
        out.set("pages", pageList);

        try {
            out.save(file);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "保存 config.yml 失败", ex);
        }
    }

    // ---------- 入口物品 ----------

    /** 构建带持久标记的入口物品（用于识别）。 */
    public ItemStack getEntryItem() {
        ItemStack item = ItemBuilder.build(entryMaterial, entryName, entryLore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(entryKey, PersistentDataType.INTEGER, 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isEntryItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(entryKey, PersistentDataType.INTEGER);
    }

    /** 打开菜单前确保玩家持有入口物品（按 entry 配置）。 */
    public void ensureEntryItem(Player player) {
        if (!entryEnabled) {
            return;
        }
        ItemStack entry = getEntryItem();
        PlayerInventory inv = player.getInventory();
        if (entryForce) {
            int slot = Math.max(0, Math.min(entrySlot, inv.getSize() - 1));
            ItemStack current = inv.getItem(slot);
            if (current == null || current.getType().isAir() || !isEntryItem(current)) {
                if (current != null && !current.getType().isAir()) {
                    int free = inv.firstEmpty();
                    if (free < 0) {
                        return; // 背包已满：保留原物品，本次不强制放入，避免覆盖
                    }
                    inv.setItem(free, current);
                }
                inv.setItem(slot, entry);
            }
        } else if (!hasEntryItem(inv)) {
            inv.addItem(entry);
        }
    }

    private boolean hasEntryItem(Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (isEntryItem(item)) {
                return true;
            }
        }
        return false;
    }

    // ---------- 导航槽位 ----------

    public int getTotalSlots() {
        return rows * 9;
    }

    public int getBottomStart() {
        return (rows - 1) * 9;
    }

    public int getPrevSlot() {
        return getBottomStart();
    }

    public int getNextSlot() {
        return getBottomStart() + 1;
    }

    public int getEditSlot() {
        return getBottomStart() + 7;
    }

    public int getCloseSlot() {
        return getBottomStart() + 8;
    }

    /** 是否属于导航栏固定槽位（物品不能放这里）。 */
    public boolean isNavigationSlot(int slot) {
        return slot == getPrevSlot() || slot == getNextSlot()
                || slot == getEditSlot() || slot == getCloseSlot();
    }

    // ---------- getter ----------

    public int getRows() {
        return rows;
    }

    public boolean isEntryEnabled() {
        return entryEnabled;
    }

    public boolean isEntryForce() {
        return entryForce;
    }

    public int getEntrySlot() {
        return entrySlot;
    }

    public List<MenuPage> getPages() {
        return pages;
    }

    public int getPageCount() {
        return pages.size();
    }

    public MenuPage getPage(int index) {
        if (index < 0 || index >= pages.size()) {
            return null;
        }
        return pages.get(index);
    }
}
