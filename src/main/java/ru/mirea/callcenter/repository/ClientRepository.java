package ru.mirea.callcenter.repository;

import ru.mirea.callcenter.model.Client;
import ru.mirea.callcenter.util.DatabaseManager;
import java.sql.*;
import java.util.*;

public class ClientRepository implements Repository<Client, Integer> {
    @Override public List<Client> findAll() throws SQLException { List<Client> result = new ArrayList<>(); try (Connection c=DatabaseManager.getConnection(); PreparedStatement p=c.prepareStatement("SELECT * FROM clients ORDER BY id"); ResultSet r=p.executeQuery()) { while(r.next()) result.add(map(r)); } return result; }
    @Override public Optional<Client> findById(Integer id) throws SQLException { try(Connection c=DatabaseManager.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM clients WHERE id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}} }
    @Override public Client save(Client x) throws SQLException { String sql=x.getId()==null?"INSERT INTO clients(full_name,phone,email) VALUES(?,?,?)":"UPDATE clients SET full_name=?,phone=?,email=? WHERE id=?"; try(Connection c=DatabaseManager.getConnection();PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){p.setString(1,x.getFullName());p.setString(2,x.getPhone());p.setString(3,x.getEmail());if(x.getId()!=null)p.setInt(4,x.getId());p.executeUpdate();if(x.getId()==null)try(ResultSet k=p.getGeneratedKeys()){if(k.next())x.setId(k.getInt(1));}}return x; }
    @Override public boolean deleteById(Integer id)throws SQLException{try(Connection c=DatabaseManager.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM clients WHERE id=?")){p.setInt(1,id);return p.executeUpdate()>0;}}
    private Client map(ResultSet r)throws SQLException{return new Client(r.getInt("id"),r.getString("full_name"),r.getString("phone"),r.getString("email"));}
}
