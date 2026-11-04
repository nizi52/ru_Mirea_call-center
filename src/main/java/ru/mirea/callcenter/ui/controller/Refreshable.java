package ru.mirea.callcenter.ui.controller;

/** Вкладка главного окна, которая перечитывает данные из БД при переключении на неё. */
public interface Refreshable {
    void refresh();
}
