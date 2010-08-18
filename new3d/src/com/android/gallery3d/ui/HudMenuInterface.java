package com.android.gallery3d.ui;

public interface HudMenuInterface {
    public MenuBar getTopMenuBar();
    public MenuItemBar getBottomMenuBar();
    public GLListView.Model getMenuModel(GLView item);
    public void onMenuItemSelected(GLListView.Model model, int index);
}
