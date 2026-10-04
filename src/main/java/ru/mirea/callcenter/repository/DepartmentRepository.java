package ru.mirea.callcenter.repository;

import ru.mirea.callcenter.model.Department;
import ru.mirea.callcenter.util.DatabaseManager;
import java.sql.*;
import java.util.*;

public class DepartmentRepository implements Repository<Department,Integer> {
    @Override public List<Department> findAll()throws SQLException{List<Department> out=new ArrayList<>();try(Connection c=DatabaseManager.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM departments ORDER BY id");ResultSet r=p.executeQuery()){while(r.next())out.add(map(r));}return out;}
    @Override public Optional<Department> findById(Integer id)throws SQLException{try(Connection c=DatabaseManager.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM departments WHERE id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}}
    @Override public Department save(Department x)throws SQLException{String sql=x.getId()==null?"INSERT INTO departments(name,description,is_active) VALUES(?,?,?)":"UPDATE departments SET name=?,description=?,is_active=? WHERE id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){p.setString(1,x.getName());p.setString(2,x.getDescription());p.setBoolean(3,x.isActive());if(x.getId()!=null)p.setInt(4,x.getId());p.executeUpdate();if(x.getId()==null)try(ResultSet k=p.getGeneratedKeys()){if(k.next())x.setId(k.getInt(1));}}return x;}
    @Override public boolean deleteById(Integer id)throws SQLException{try(Connection c=DatabaseManager.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM departments WHERE id=?")){p.setInt(1,id);return p.executeUpdate()>0;}}
    private Department map(ResultSet r)throws SQLException{return new Department(r.getInt("id"),r.getString("name"),r.getString("description"),r.getBoolean("is_active"));}
}
