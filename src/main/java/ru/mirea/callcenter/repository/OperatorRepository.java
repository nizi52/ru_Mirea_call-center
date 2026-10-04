package ru.mirea.callcenter.repository;

import ru.mirea.callcenter.model.Operator;
import ru.mirea.callcenter.util.DatabaseManager;
import java.sql.*;
import java.util.*;

public class OperatorRepository implements Repository<Operator, Integer> {
    @Override public List<Operator> findAll() throws SQLException {
        List<Operator> result = new ArrayList<>();
        try (Connection c=DatabaseManager.getConnection(); PreparedStatement ps=c.prepareStatement("SELECT o.*, d.name AS department_name FROM operators o JOIN departments d ON d.id=o.department_id ORDER BY o.id"); ResultSet rs=ps.executeQuery()) {
            while(rs.next()) result.add(map(rs));
        } return result;
    }
    @Override public Optional<Operator> findById(Integer id) throws SQLException {
        try (Connection c=DatabaseManager.getConnection(); PreparedStatement ps=c.prepareStatement("SELECT o.*, d.name AS department_name FROM operators o JOIN departments d ON d.id=o.department_id WHERE o.id=?")) {
            ps.setInt(1,id); try(ResultSet rs=ps.executeQuery()) { return rs.next()?Optional.of(map(rs)):Optional.empty(); }
        }
    }
    @Override public Operator save(Operator o) throws SQLException {
        if(o.getId()==null) {
            String sql="INSERT INTO operators(full_name,login,department_id,is_active) VALUES(?,?,?,?)";
            try(Connection c=DatabaseManager.getConnection(); PreparedStatement ps=c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                fill(ps,o); ps.executeUpdate(); try(ResultSet keys=ps.getGeneratedKeys()){ if(keys.next())o.setId(keys.getInt(1)); }
            }
        } else {
            try(Connection c=DatabaseManager.getConnection(); PreparedStatement ps=c.prepareStatement("UPDATE operators SET full_name=?,login=?,department_id=?,is_active=? WHERE id=?")) {
                fill(ps,o); ps.setInt(5,o.getId()); ps.executeUpdate();
            }
        } return o;
    }
    @Override public boolean deleteById(Integer id) throws SQLException {
        try(Connection c=DatabaseManager.getConnection(); PreparedStatement ps=c.prepareStatement("DELETE FROM operators WHERE id=?")) { ps.setInt(1,id); return ps.executeUpdate()>0; }
    }
    private void fill(PreparedStatement ps,Operator o)throws SQLException { ps.setString(1,o.getFullName());ps.setString(2,o.getLogin());ps.setInt(3,o.getDepartmentId());ps.setBoolean(4,o.isActive()); }
    private Operator map(ResultSet rs)throws SQLException { return new Operator(rs.getInt("id"),rs.getString("full_name"),rs.getString("login"),rs.getInt("department_id"),rs.getString("department_name"),rs.getBoolean("is_active")); }
}
