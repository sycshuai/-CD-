package com.cd.caidan.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单中的一页（手动分页）。
 */
public class MenuPage {

    private String title;
    private final List<MenuItem> items = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<MenuItem> getItems() {
        return items;
    }

    /** 查找指定格子上的物品（槽位冲突时取第一个）。 */
    public MenuItem findItem(int slot) {
        for (MenuItem item : items) {
            if (item.getSlot() == slot) {
                return item;
            }
        }
        return null;
    }

    /** 按槽位删除物品，返回是否删除成功。 */
    public boolean removeItem(int slot) {
        return items.removeIf(item -> item.getSlot() == slot);
    }
}
