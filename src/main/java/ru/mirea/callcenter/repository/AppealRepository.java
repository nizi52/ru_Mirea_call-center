package ru.mirea.callcenter.repository;

import ru.mirea.callcenter.model.*;
import ru.mirea.callcenter.util.DatabaseManager;
import java.sql.*;
import java.util.*;

public class AppealRepository implements Repository<Appeal, Integer> {
    @Override public List<Appeal> findAll() throws SQLException {
        List<Appeal> result=new ArrayList<>();
        try(Connection c=DatabaseManager.getConnection(); PreparedStatement ps=c.prepareStatement("SELECT * FROM appeals");ResultSet rs=ps.executeQuery()){while(rs.next())result.add(map(rs));} return result;
    }
    @Override public Optional<Appeal> findById(Integer id)throws SQLException {
        try(Connection c=DatabaseManager.getConnection();PreparedStatement ps=c.prepareStatement("SELECT * FROM appeals WHERE id=?")){ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){return rs.next()?Optional.of(map(rs)):Optional.empty();}}
    }
    @Override public Appeal save(Appeal a)throws SQLException {
        if(a.getId()==null){
            String sql="INSERT INTO appeals(operator_id,client_name,client_phone,topic,category,status,created_at,closed_at) VALUES(?,?,?,?,?,?,?,?)";
            try(Connection c=DatabaseManager.getConnection();PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){fill(ps,a);ps.executeUpdate();try(ResultSet k=ps.getGeneratedKeys()){if(k.next())a.setId(k.getInt(1));}}
        } else {
            String sql="UPDATE appeals SET operator_id=?,client_name=?,client_phone=?,topic=?,category=?,status=?,created_at=?,closed_at=? WHERE id=?";
            try(Connection c=DatabaseManager.getConnection();PreparedStatement ps=c.prepareStatement(sql)){fill(ps,a);ps.setInt(9,a.getId());ps.executeUpdate();}
        } return a;
    }
    @Override public boolean deleteById(Integer id)throws SQLException {try(Connection c=DatabaseManager.getConnection();PreparedStatement ps=c.prepareStatement("DELETE FROM appeals WHERE id=?")){ps.setInt(1,id);return ps.executeUpdate()>0;}}
    public long countActiveByOperator(int operatorId)throws SQLException {
        String sql="SELECT count(*) FROM appeals WHERE operator_id=? AND status IN ('NEW','IN_PROGRESS','ESCALATED')";
        try(Connection c=DatabaseManager.getConnection();PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,operatorId);try(ResultSet rs=ps.executeQuery()){rs.next();return rs.getLong(1);}}
    }
    private void fill(PreparedStatement ps,Appeal a)throws SQLException {
        if(a.getOperatorId()==null)ps.setNull(1,Types.INTEGER);else ps.setInt(1,a.getOperatorId()); ps.setString(2,a.getClientName());ps.setString(3,a.getClientPhone());ps.setString(4,a.getTopic());ps.setString(5,a.getCategory());ps.setString(6,a.getStatus().name());ps.setTimestamp(7,Timestamp.valueOf(a.getCreatedAt()));if(a.getClosedAt()==null)ps.setNull(8,Types.TIMESTAMP);else ps.setTimestamp(8,Timestamp.valueOf(a.getClosedAt()));
    }
    private Appeal map(ResultSet rs)throws SQLException {Timestamp closed=rs.getTimestamp("closed_at");return new Appeal(rs.getInt("id"),(Integer)rs.getObject("operator_id"),rs.getString("client_name"),rs.getString("client_phone"),rs.getString("topic"),rs.getString("category"),AppealStatus.valueOf(rs.getString("status")),rs.getTimestamp("created_at").toLocalDateTime(),closed==null?null:closed.toLocalDateTime());}
}
