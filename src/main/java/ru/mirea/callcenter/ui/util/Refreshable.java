package ru.mirea.callcenter.ui.util;

/** Контроллер окна, которое умеет перечитать данные из БД (вызывается после изменений в любом окне). */
public interface Refreshable {
    void refresh();
}
