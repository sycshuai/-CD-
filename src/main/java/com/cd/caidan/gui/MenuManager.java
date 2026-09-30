package com.cd.caidan.gui;

import com.cd.caidan.CaiDanPlugin;
import com.cd.caidan.config.ConfigManager;
import com.cd.caidan.model.CommandEntry;
import com.cd.caidan.model.MenuItem;
import com.cd.caidan.model.MenuPage;
import com.cd.caidan.util.ItemBuilder;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 菜单 GUI 核心：打开菜单 / 设置界面、处理点击、执行指令、维护在线编辑状态机。
 */
public class MenuManager {

    private static final long SESSION_TIMEOUT_MS = 30_000L;

    private final CaiDanPlugin plugin;
    private final ConfigManager config;
    private final Map<UUID, EditSession> sessions = new ConcurrentHashMap<>();

    public MenuManager(CaiDanPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ==================== 打开界面 ====================

    /** 打开菜单（普通模式），pageIndex 越界时回退到首页。 */
    public void openMenu(Player player) {
        openMenu(player, 0, false);
    }

    public void openMenu(Player player, int pageIndex, boolean editMode) {
        MenuPage page = config.getPage(pageIndex);
        if (page == null) {
            pageIndex = 0;
            page = config.getPage(0);
        }
        if (page == null) {
            player.closeInventory();
            return;
        }
        int total = config.getTotalSlots();
        MenuHolder holder = new MenuHolder(null, MenuHolder.Type.MENU, pageIndex, editMode, -1);
        Inventory inv = Bukkit.createInventory(holder, total,
                ItemBuilder.truncateTitle(page.getTitle()));
        holder.attach(inv);

        boolean admin = isAdmin(player);
        for (MenuItem item : page.getItems()) {
            inv.setItem(item.getSlot(), item.toItemStack(admin));
        }
        inv.setItem(config.getPrevSlot(), buildPrevButton(pageIndex));
        inv.setItem(config.getNextSlot(), buildNextButton(pageIndex));
        inv.setItem(config.getCloseSlot(), buildCloseButton());
        if (admin) {
            inv.setItem(config.getEditSlot(), editMode ? buildEditReturnButton() : buildEditEnterButton());
        }
        if (editMode && admin) {
            for (int slot = 0; slot < total; slot++) {
                if (config.isNavigationSlot(slot) || page.findItem(slot) != null) {
                    continue;
                }
                inv.setItem(slot, buildPlaceholderWool());
            }
        }
        player.openInventory(inv);
    }

    /** 打开某物品的设置界面。 */
    public void openSettings(Player player, int pageIndex, int itemSlot) {
        MenuPage page = config.getPage(pageIndex);
        if (page == null) {
            return;
        }
        MenuItem target = page.findItem(itemSlot);
        if (target == null) {
            return;
        }
        String title = "§8设置 - " + ItemBuilder.stripColor(target.getName());
        MenuHolder holder = new MenuHolder(null, MenuHolder.Type.SETTINGS, pageIndex, false, itemSlot);
        Inventory inv = Bukkit.createInventory(holder, 9, ItemBuilder.truncateTitle(title));
        holder.attach(inv);

        inv.setItem(0, ItemBuilder.build(Material.BOOK, "&e介绍", List.of("&7查看该物品的配置信息")));
        inv.setItem(1, ItemBuilder.build(Material.NAME_TAG, "&e重命名", List.of("&7修改物品显示名称")));
        inv.setItem(2, ItemBuilder.build(Material.BARRIER, "&c删除", List.of("&7删除该菜单物品")));
        inv.setItem(3, ItemBuilder.build(Material.COMMAND_BLOCK, "&e修改指令", List.of("&7修改点击后执行的指令")));
        inv.setItem(4, ItemBuilder.build(Material.REPEATER, "&e修改执行身份", List.of("&7修改执行身份：玩家/控制台")));
        inv.setItem(8, ItemBuilder.build(Material.ARROW, "&7返回菜单", List.of("&7返回该页菜单")));
        player.openInventory(inv);
    }

    // ==================== 点击处理 ====================

    public void handleClick(Player player, MenuHolder holder, int rawSlot, ClickType click) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (rawSlot < 0 || rawSlot >= top.getSize()) {
            return; // 玩家背包区域，允许正常整理
        }
        if (holder.getType() == MenuHolder.Type.SETTINGS) {
            handleSettingsClick(player, holder, rawSlot);
            return;
        }
        int pageIndex = holder.getPageIndex();
        boolean editMode = holder.isEditMode();
        MenuPage page = config.getPage(pageIndex);
        if (page == null) {
            return;
        }
        if (rawSlot == config.getPrevSlot()) {
            if (pageIndex > 0) {
                openMenu(player, pageIndex - 1, editMode);
            }
            return;
        }
        if (rawSlot == config.getNextSlot()) {
            if (pageIndex < config.getPageCount() - 1) {
                openMenu(player, pageIndex + 1, editMode);
            }
            return;
        }
        if (rawSlot == config.getCloseSlot()) {
            player.closeInventory();
            return;
        }
        if (rawSlot == config.getEditSlot()) {
            if (isAdmin(player)) {
                openMenu(player, pageIndex, !editMode);
            }
            return;
        }

        MenuItem item = page.findItem(rawSlot);
        if (item != null) {
            if (isAdmin(player) && click.isRightClick()) {
                openSettings(player, pageIndex, item.getSlot());
            } else {
                runCommands(player, item);
            }
            return;
        }

        // 编辑（放置）模式下点击红羊毛：开始放置流程
        if (editMode && isAdmin(player)) {
            ItemStack cursor = player.getOpenInventory().getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                startPlacement(player, pageIndex, rawSlot, cursor.clone());
            } else {
                player.sendMessage("§c请先拿起要放置的物品，再点击红色羊毛位置");
            }
        }
    }

    private void handleSettingsClick(Player player, MenuHolder holder, int rawSlot) {
        int pageIndex = holder.getPageIndex();
        int itemSlot = holder.getItemSlot();
        MenuPage page = config.getPage(pageIndex);
        if (page == null) {
            return;
        }
        MenuItem target = page.findItem(itemSlot);
        if (target == null) {
            player.sendMessage("§c该物品已不存在");
            player.closeInventory();
            return;
        }
        switch (rawSlot) {
            case 0 -> showItemInfo(player, pageIndex, target);
            case 1 -> {
                player.closeInventory();
                EditSession session = new EditSession(pageIndex, -1, null, target, EditSession.STATE_RENAME);
                sessions.put(player.getUniqueId(), session);
                player.sendMessage("§e请输入新的显示名称（支持 & 颜色代码，输入 cancel 取消）：");
            }
            case 2 -> {
                page.removeItem(itemSlot);
                config.save();
                player.closeInventory();
                player.sendMessage("§a已删除该菜单物品");
            }
            case 3 -> {
                player.closeInventory();
                EditSession session = new EditSession(pageIndex, -1, null, target, EditSession.STATE_EDIT_COMMAND);
                sessions.put(player.getUniqueId(), session);
                player.sendMessage("§e请输入新的指令（如 give %player% diamond 1，输入 cancel 取消）：");
            }
            case 4 -> {
                player.closeInventory();
                EditSession session = new EditSession(pageIndex, -1, null, target, EditSession.STATE_EDIT_TYPE);
                sessions.put(player.getUniqueId(), session);
                player.sendMessage("§e请选择执行身份：输入 §f1§e（玩家自身）或 §f2§e（控制台）：");
            }
            case 8 -> openMenu(player, pageIndex, false);
            default -> { }
        }
    }

    private void showItemInfo(Player player, int pageIndex, MenuItem target) {
        player.sendMessage("§e§l物品信息");
        player.sendMessage(" §7槽位: §f" + (target.getSlot() + 1));
        player.sendMessage(" §7材质: §f" + target.getMaterial().name());
        player.sendMessage(" §7名称: §f" + ItemBuilder.color(target.getName()));
        if (target.getCommands().isEmpty()) {
            player.sendMessage(" §7指令: §f（无）");
        } else {
            for (int i = 0; i < target.getCommands().size(); i++) {
                CommandEntry ce = target.getCommands().get(i);
                String identity = ce.isConsole() ? "控制台" : "玩家自身";
                player.sendMessage(" §7指令" + (i + 1) + ": §f" + ce.getCommand()
                        + " §8[" + identity + "]");
            }
        }
        player.closeInventory();
    }

    // ==================== 执行指令 ====================

    /**
     * 执行菜单物品指令。
     * player 身份的命令通过 performCommand 在服务端执行；若命令以 "server " 开头
     * 且本地执行失败（代理换服命令在后端不存在），则改走 BungeeCord Connect
     * 消息通道，交由 Velocity / BungeeCord 代理执行换服。
     */
    public void runCommands(Player player, MenuItem item) {
        for (CommandEntry ce : item.getCommands()) {
            String command = ce.getCommand().replace("%player%", player.getName()).trim();
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            if (command.isEmpty()) {
                continue;
            }
            if (ce.isConsole()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                continue;
            }
            boolean executed = player.performCommand(command);
            if (!executed && isServerSwitchCommand(command)) {
                String serverName = command.substring("server ".length()).trim();
                sendConnect(player, serverName);
            }
        }
    }

    private boolean isServerSwitchCommand(String command) {
        return command.toLowerCase().startsWith("server ")
                && command.length() > "server ".length();
    }

    /** 通过 BungeeCord Connect 通道请求代理将玩家切换到指定服务器。 */
    private void sendConnect(Player player, String serverName) {
        if (serverName.isEmpty()) {
            return;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    // ==================== 在线编辑（聊天状态机） ====================

    /** 开始放置流程：记录待放置物品与目标槽位，关闭 GUI 进入聊天命名。 */
    private void startPlacement(Player player, int pageIndex, int slot, ItemStack item) {
        player.closeInventory();
        EditSession session = new EditSession(pageIndex, slot, item, null, EditSession.STATE_NAME);
        sessions.put(player.getUniqueId(), session);
        player.sendMessage("§e请为该菜单物品命名（支持 & 颜色代码，输入 cancel 取消）：");
    }

    /** 处理聊天输入（由监听器在收到消息时调用）。 */
    public void handleChat(Player player, String message) {
        UUID uuid = player.getUniqueId();
        EditSession session = sessions.get(uuid);
        if (session == null) {
            return;
        }
        if (System.currentTimeMillis() - session.timestamp > SESSION_TIMEOUT_MS) {
            sessions.remove(uuid);
            player.sendMessage("§c编辑超时，已取消");
            return;
        }
        String msg = message.trim();
        if (msg.equalsIgnoreCase("cancel") || msg.equalsIgnoreCase("leave")) {
            sessions.remove(uuid);
            player.sendMessage("§c已取消编辑");
            return;
        }
        switch (session.state) {
            case EditSession.STATE_NAME -> handleName(player, session, msg);
            case EditSession.STATE_COMMAND -> handleCommand(player, session, msg);
            case EditSession.STATE_TYPE -> handleType(player, session, msg);
            case EditSession.STATE_RENAME -> handleRename(player, session, msg);
            case EditSession.STATE_EDIT_COMMAND -> handleEditCommand(player, session, msg);
            case EditSession.STATE_EDIT_TYPE -> handleEditType(player, session, msg);
            default -> {
                sessions.remove(uuid);
                player.sendMessage("§c编辑状态异常，已取消");
            }
        }
    }

    private void handleName(Player player, EditSession session, String name) {
        if (name.isEmpty()) {
            player.sendMessage("§c名称不能为空，请重新输入：");
            return;
        }
        session.itemName = name;
        session.state = EditSession.STATE_COMMAND;
        player.sendMessage("§e请输入点击后要执行的指令（如 §fgive %player% diamond 1§e，输入 cancel 取消）：");
    }

    private void handleCommand(Player player, EditSession session, String command) {
        if (command.isEmpty()) {
            player.sendMessage("§c指令不能为空，请重新输入：");
            return;
        }
        session.commandBuffer = command.startsWith("/") ? command.substring(1) : command;
        session.state = EditSession.STATE_TYPE;
        player.sendMessage("§e请选择执行身份：输入 §f1§e（玩家自身）或 §f2§e（控制台）：");
    }

    private void handleType(Player player, EditSession session, String choice) {
        Boolean console = parseType(choice);
        if (console == null) {
            player.sendMessage("§c请输入 §f1§c 或 §f2§c：");
            return;
        }
        // 构建新菜单物品
        MenuItem item = new MenuItem();
        item.setSlot(session.slot);
        item.setMaterial(session.item.getType());
        item.setName(session.itemName);
        List<String> lore = new ArrayList<>();
        if (session.item.hasItemMeta() && session.item.getItemMeta().hasLore()) {
            lore.addAll(session.item.getItemMeta().getLore());
        }
        if (lore.isEmpty()) {
            lore.add("&7点击执行指令");
        }
        item.setLore(lore);
        item.setCommands(List.of(new CommandEntry(console ? "console" : "player", session.commandBuffer)));
        MenuPage page = config.getPage(session.pageIndex);
        if (page == null) {
            sessions.remove(player.getUniqueId());
            player.sendMessage("§c该页面已不存在，已取消");
            return;
        }
        page.removeItem(session.slot);
        page.getItems().add(item);
        config.save();
        sessions.remove(player.getUniqueId());
        player.sendMessage("§a设置完成！重新打开菜单即可看到新物品。");
    }

    private void handleRename(Player player, EditSession session, String name) {
        session.target.setName(name);
        config.save();
        sessions.remove(player.getUniqueId());
        player.sendMessage("§a已重命名为：§f" + ItemBuilder.color(name));
    }

    private void handleEditCommand(Player player, EditSession session, String command) {
        String clean = command.startsWith("/") ? command.substring(1) : command;
        String keepType = "player";
        if (!session.target.getCommands().isEmpty()) {
            keepType = session.target.getCommands().get(0).getType();
        }
        session.target.setCommands(List.of(new CommandEntry(keepType, clean)));
        config.save();
        sessions.remove(player.getUniqueId());
        player.sendMessage("§a指令已更新：§f" + clean);
    }

    private void handleEditType(Player player, EditSession session, String choice) {
        Boolean console = parseType(choice);
        if (console == null) {
            player.sendMessage("§c请输入 §f1§c 或 §f2§c：");
            return;
        }
        List<CommandEntry> updated = new ArrayList<>();
        for (CommandEntry ce : session.target.getCommands()) {
            updated.add(new CommandEntry(console ? "console" : "player", ce.getCommand()));
        }
        session.target.setCommands(updated);
        config.save();
        sessions.remove(player.getUniqueId());
        player.sendMessage("§a执行身份已更新为：§f" + (console ? "控制台" : "玩家自身"));
    }

    private Boolean parseType(String choice) {
        return switch (choice.trim()) {
            case "1", "player", "玩家", "玩家自身" -> Boolean.FALSE;
            case "2", "console", "控制台" -> Boolean.TRUE;
            default -> null;
        };
    }

    /** 清理某个玩家进行中的编辑状态（例如 /cd reload 时）。 */
    public void clearSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    /** 玩家是否处于进行中的编辑流程。 */
    public boolean hasSession(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public void clearAllSessions() {
        sessions.clear();
    }

    // ==================== 按钮构建 ====================

    private ItemStack buildPrevButton(int pageIndex) {
        if (pageIndex <= 0) {
            return ItemBuilder.build(Material.GRAY_STAINED_GLASS_PANE, "&7没有上一页", List.of());
        }
        return ItemBuilder.build(Material.ARROW, "&e上一页", List.of("&7点击返回上一页"));
    }

    private ItemStack buildNextButton(int pageIndex) {
        if (pageIndex >= config.getPageCount() - 1) {
            return ItemBuilder.build(Material.GRAY_STAINED_GLASS_PANE, "&7没有下一页", List.of());
        }
        return ItemBuilder.build(Material.ARROW, "&e下一页", List.of("&7点击进入下一页"));
    }

    private ItemStack buildCloseButton() {
        return ItemBuilder.build(Material.BARRIER, "&c关闭", List.of("&7点击关闭（或按 Esc）"));
    }

    private ItemStack buildEditEnterButton() {
        return ItemBuilder.build(Material.WRITABLE_BOOK, "&e编辑菜单", List.of("&7点击进入放置模式"));
    }

    private ItemStack buildEditReturnButton() {
        return ItemBuilder.build(Material.WRITABLE_BOOK, "&e返回菜单", List.of("&7点击返回普通菜单"));
    }

    private ItemStack buildPlaceholderWool() {
        return ItemBuilder.build(Material.RED_WOOL, "&c放置位置", List.of("&7手持物品点击这里放置"));
    }

    public boolean isAdmin(Player player) {
        return player.isOp() || player.hasPermission("cd.admin");
    }

    /** 在线编辑会话。 */
    public static class EditSession {
        public static final String STATE_NAME = "name";
        public static final String STATE_COMMAND = "command";
        public static final String STATE_TYPE = "type";
        public static final String STATE_RENAME = "rename";
        public static final String STATE_EDIT_COMMAND = "edit_command";
        public static final String STATE_EDIT_TYPE = "edit_type";

        final int pageIndex;
        final int slot;           // 新建时的目标槽位
        final ItemStack item;     // 新建时的物品
        final MenuItem target;    // 编辑已有时的目标
        final long timestamp;
        String state;
        String itemName = "";
        String commandBuffer = "";

        EditSession(int pageIndex, int slot, ItemStack item, MenuItem target, String state) {
            this.pageIndex = pageIndex;
            this.slot = slot;
            this.item = item;
            this.target = target;
            this.state = state;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
