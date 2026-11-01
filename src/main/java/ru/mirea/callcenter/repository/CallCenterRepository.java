package ru.mirea.callcenter.repository;

import ru.mirea.callcenter.model.CallCenter;
import ru.mirea.callcenter.util.DatabaseManager;
import java.sql.*;
import java.util.*;

public class CallCenterRepository implements Repository<CallCenter,Integer> {
    @Override public List<CallCenter> findAll()throws SQLException{List<CallCenter> out=new ArrayList<>();try(Connection c=DatabaseManager.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM call_centers ORDER BY id");ResultSet r=p.executeQuery()){while(r.next())out.add(map(r));}return out;}
    @Override public Optional<CallCenter> findById(Integer id)throws SQLException{try(Connection c=DatabaseManager.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM call_centers WHERE id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}}
    @Override public CallCenter save(CallCenter x)throws SQLException{String sql=x.getId()==null?"INSERT INTO call_centers(name,address,is_active) VALUES(?,?,?)":"UPDATE call_centers SET name=?,address=?,is_active=? WHERE id=?";try(Connection c=DatabaseManager.getConnection();PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){p.setString(1,x.getName());p.setString(2,x.getAddress());p.setBoolean(3,x.isActive());if(x.getId()!=null)p.setInt(4,x.getId());p.executeUpdate();if(x.getId()==null)try(ResultSet k=p.getGeneratedKeys()){if(k.next())x.setId(k.getInt(1));}}return x;}
    @Override public boolean deleteById(Integer id)throws SQLException{try(Connection c=DatabaseManager.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM call_centers WHERE id=?")){p.setInt(1,id);return p.executeUpdate()>0;}}
    private CallCenter map(ResultSet r)throws SQLException{return new CallCenter(r.getInt("id"),r.getString("name"),r.getString("address"),r.getBoolean("is_active"));}
}
