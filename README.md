# [CD] 菜单插件 (CaiDan)

适用于 **Paper 1.21.11** 的多页 GUI 菜单插件。支持管理员在游戏内**在线编辑**菜单：放置物品、命名、绑定点击指令，所有修改实时写入配置文件。

- 插件文件：`[CD]caidan1.21.11paper1.2.jar`
- 运行要求：Paper 1.21.11 服务端，**Java 21+**

> **v1.2 更新（2026-09-30）**
> - 修复：`server <服务器>` 等 Velocity/BungeeCord 代理换服指令无法执行（`performCommand` 只作用于后端，命令不会到达代理）。现自动改走 BungeeCord `Connect` 消息通道，由代理执行换服
>
> **v1.1 更新（2026-09-30）**
> - 修复：放置模式中无法从物品栏拿起物品替换红色羊毛（点击事件误拦截底部物品栏，现仅拦截菜单顶部区域）
> - 修复：拖拽物品到菜单区域不再误放物品，并提示改用点击方式
> - 修复：GUI 标题中的 `&` 颜色代码未渲染的问题

---

## 一、功能特性

- **物品栏 GUI 菜单**：输入 `/cd` 打开，菜单行数可配置（默认 3 行）
- **手动分页**：配置中按页面分组，箭头翻页，屏障或 Esc 关闭
- **入口物品（附魔书）**：
  - 默认：`/cd` 打开菜单时自动给予附魔书，可自由保管，手持右键也可打开菜单
  - 强制模式（`entry.force: true`）：附魔书强制放入指定格子（默认第 9 格），玩家**无法移动、拿起、丢弃**，也无法放入铁砧、容器、合成台等任何地方；点击锁定的书即可打开菜单
- **管理员在线编辑**：OP 打开菜单后多出「书与笔」，进入放置模式，全程聊天栏引导
- **点击执行指令**：每个菜单物品可绑定多条指令，支持以**玩家自身**或**控制台**身份执行，指令中 `%player%` 自动替换为玩家名
- **已设置物品管理**：左键执行功能，右键进入设置（介绍 / 重命名 / 删除 / 修改指令 / 修改执行身份）
- 编辑结果实时保存到 `config.yml`，可用 `/cd reload` 热重载

---

## 二、安装

1. 将 `[CD]caidan1.21.11paper1.2.jar` 放入服务端 `plugins` 文件夹
2. 重启服务端（或使用支持热加载的工具）
3. 首次启动会自动生成 `plugins/CaiDan/config.yml`

---

## 三、命令与权限

| 命令 | 说明 | 权限 |
|---|---|---|
| `/cd` | 打开菜单 | `cd.use`（默认所有人） |
| `/cd help` | 查看帮助 | 无 |
| `/cd reload` | 重载配置文件 | `cd.admin`（默认 OP） |

权限节点：

- `cd.use` — 允许使用菜单（默认 `true`）
- `cd.admin` — 允许在线编辑与重载（默认 `op`，OP 自动拥有）

---

## 四、配置文件详解

默认配置生成于 `plugins/CaiDan/config.yml`：

```yaml
# 菜单行数（1~6，默认 3）
rows: 3

# 入口物品（打开菜单的附魔书）
entry:
  enabled: true        # 是否在打开菜单时自动给予入口物品
  force: false         # true = 强制锁定在指定格子，玩家无法移动/拿起/放入铁砧等
  slot: 8              # 强制放入的物品栏格子（0~35，8 = 第九格）
  material: ENCHANTED_BOOK
  name: '&b&l[CD] &r&e菜单入口'
  lore:
    - '&7输入 &f/cd &7打开菜单'

pages:
  - title: '&e&l[CD] 菜单 - 首页'
    items:
      - slot: 0
        material: DIAMOND_SWORD
        name: '&c&l示例武器'
        lore:
          - '&7点击获得一把钻石剑'
        commands:
          - type: player
            command: 'give %player% diamond_sword 1'
      - slot: 1
        material: DIAMOND
        name: '&b&l示例钻石'
        lore:
          - '&7点击获得 1 颗钻石（控制台执行）'
        commands:
          - type: console
            command: 'give %player% diamond 1'
  - title: '&e&l[CD] 菜单 - 第二页'
    items: []
```

### 字段说明

| 字段 | 说明 |
|---|---|
| `rows` | 菜单行数（1~6），修改后每页格子数随之变化 |
| `entry.enabled` | 是否在打开菜单时自动补发入口物品 |
| `entry.force` | `true` 后入口物品锁定在 `entry.slot` 格，不可移动/拿起/丢弃/放入铁砧与容器 |
| `entry.slot` | 强制放入的物品栏格（0~35，物品栏从左到右数，第 9 格 = 8） |
| `pages[].title` | 页面标题，支持 `&` 颜色代码 |
| `pages[].items[].slot` | 物品在菜单 GUI 中的格子（0 起，从左到右、从上到下） |
| `items[].material` | 物品材质（Bukkit 材质名，如 DIAMOND_SWORD） |
| `items[].name` / `lore` | 显示名称与说明，支持 `&` 颜色代码 |
| `items[].commands[]` | 点击执行的指令列表，可多条 |
| `commands[].type` | `player` = 以玩家身份执行；`console` = 以控制台身份执行 |
| `commands[].command` | 指令内容，`%player%` 替换为玩家名，**不要写前导 `/`** |

### 导航栏固定槽位（物品请勿占用）

每页**底部一行**固定为导航栏：

```
第 1 格 (底部行+0)   上一页（无上一页时显示灰色玻璃）
第 2 格 (底部行+1)   下一页（无下一页时显示灰色玻璃）
倒数第 2 格 (底部行+7) 编辑入口「书与笔」（仅 OP 可见）
最后 1 格 (底部行+8)  关闭（屏障，Esc 亦可）
```

示例（rows=3，共 27 格）：底部行 = 18~26，其中 18=上一页、19=下一页、25=编辑、26=关闭，其余 18~24 中间 5 格及前两行 18 格可放物品。

---

## 五、管理员在线编辑指南

### 放置新物品

1. OP 输入 `/cd` 打开菜单，点击底部倒数第 2 格的**「书与笔」**进入放置模式（可放置位置显示为**红色羊毛**）
2. 从下方物品栏**拿起**要放置的物品，点击红色羊毛位置，GUI 自动关闭
3. 聊天栏提示输入**物品名称**（支持 `&` 颜色代码）
4. 接着输入**点击后要执行的指令**（如 `give %player% diamond 1`）
5. 最后选择**执行身份**：输入 `1`（玩家自身）或 `2`（控制台）
6. 设置完成，重新打开菜单即可看到新物品（配置已同步写入 `config.yml`）

> 任一步输入 `cancel` 可取消编辑；30 秒无输入自动取消。

### 编辑已设置物品

- 在菜单中（普通或放置模式均可），OP **左键点击**物品 = 执行功能；**右键点击**物品 = 进入设置界面（物品 lore 中也有左右键提示）
- 设置界面从左到右：**介绍**（查看配置信息）、**重命名**（聊天输入新名称）、**删除**、**修改指令**、**修改执行身份**
- 返回按钮（最右侧箭头）返回菜单

### 重载配置

- 修改 `config.yml` 后，OP 执行 `/cd reload` 热重载；已打开的菜单会自动关闭，重新打开生效

---

## 六、注意事项

- 在线编辑保存时会**重写** `config.yml`，文件原有注释会丢失（结构不变）；初始模板的完整说明见本 README
- `entry.force: true` 时若指定格子被占用，插件会把原物品移到背包空位；**背包已满时不强制放入**，避免覆盖玩家物品
- 菜单物品仅支持「点击执行指令」一种功能类型（可按你的选择后续扩展）
- 指令以 `player` 身份执行时受该玩家自身权限限制；以 `console` 身份执行不受限制，仅管理员可配置
- **代理换服**：在 Velocity / BungeeCord 代理环境下，`server <服务器名>` 属于代理命令，后端执行无效。插件会自动识别以 `server ` 开头的指令并改走 BungeeCord `Connect` 通道交给代理换服（`type` 建议用 `player`）。若代理未开启该通道，请在 Velocity 的 `velocity.toml` 中确认 `bungee-plugin-message-channel = true`（默认开启）
- 插件仅面向 Paper（非 Spigot 兼容模式），请勿用于 CraftBukkit 等服务端

---

## 七、源码结构

```
caidan-plugin/
├── pom.xml                          # Maven 构建（Java 21，Paper API 1.21.11）
└── src/main/
    ├── java/com/cd/caidan/
    │   ├── CaiDanPlugin.java        # 主类
    │   ├── command/CdCommand.java   # /cd 命令
    │   ├── config/ConfigManager.java# 配置加载/保存/入口物品
    │   ├── gui/MenuManager.java     # GUI 与在线编辑状态机
    │   ├── gui/MenuHolder.java      # 界面上下文
    │   ├── listener/                # 菜单点击/入口书锁定/聊天编辑
    │   ├── model/                   # 菜单页/物品/指令模型
    │   └── util/ItemBuilder.java    # 物品构建
    └── resources/
        ├── plugin.yml
        └── config.yml
```

重新构建：`mvn -f caidan-plugin/pom.xml clean package`（需 JDK 21 与 Maven，产物在 `caidan-plugin/target/`）。
