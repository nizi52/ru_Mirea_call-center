package ru.mirea.callcenter.ui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.exception.EntityNotFoundException;
import ru.mirea.callcenter.util.DbErrors;

import java.sql.SQLException;

public final class UiErrors {
    private static final Logger LOG = LoggerFactory.getLogger(UiErrors.class);

    private UiErrors() { }

    public static String message(Throwable t) {
        if (t instanceof BusinessException || t instanceof EntityNotFoundException) return t.getMessage();
        if (t instanceof java.io.IOException) {
            LOG.error("Ошибка ввода-вывода", t);
            return "Не удалось записать файл. Проверьте, что он не открыт в другой программе и что есть доступ к папке.";
        }
        if (t instanceof SQLException e) {
            LOG.error("Ошибка БД (SQLState={})", e.getSQLState(), e);
            return DbErrors.toMessage(e);
        }
        LOG.error("Непредвиденная ошибка", t);
        return "Произошла непредвиденная ошибка. Подробности записаны в журнал.";
    }
}
