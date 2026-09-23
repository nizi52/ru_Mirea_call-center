package ru.mirea.callcenter.repository;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
public interface Repository<T, ID> {
    List<T> findAll() throws SQLException;
    Optional<T> findById(ID id) throws SQLException;
    T save(T entity) throws SQLException;
    boolean deleteById(ID id) throws SQLException;
}
