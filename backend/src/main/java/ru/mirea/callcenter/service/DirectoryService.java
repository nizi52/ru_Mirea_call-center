package ru.mirea.callcenter.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.mirea.callcenter.domain.*;
import ru.mirea.callcenter.dto.DirectoryDtos.*;
import ru.mirea.callcenter.repository.*;
import ru.mirea.callcenter.error.*;
import java.util.*;
@Service @Transactional
public class DirectoryService {
 private final CallClientRepository clients; private final CallOperatorRepository operators;
 private final CallCenterRepository centers; private final JdbcTemplate jdbc; private final AuditService audit;
 public DirectoryService(CallClientRepository c,CallOperatorRepository o,CallCenterRepository centers,JdbcTemplate jdbc,AuditService audit){clients=c;operators=o;this.centers=centers;this.jdbc=jdbc;this.audit=audit;}
 private Pageable page(int page,int size){if(page<0||size<1||size>100)throw new BadRequestException("Некорректный размер страницы");return PageRequest.of(page,size,Sort.by("fullName","id"));}
 @Transactional(readOnly=true) public Page<ClientResponse> clients(String q,int page,int size){String term="%"+escape(q)+"%";return clients.findAll((root,query,cb)->cb.or(cb.like(cb.lower(root.get("fullName")),term,'\\'),cb.like(root.get("phone"),term,'\\'),cb.like(cb.lower(root.get("email")),term,'\\')),page(page,size)).map(this::clientResponse);}
 @Transactional(readOnly=true) public Page<OperatorResponse> operators(String q,int page,int size){String term="%"+escape(q)+"%";return operators.findAll((root,query,cb)->cb.or(cb.like(cb.lower(root.get("fullName")),term,'\\'),cb.like(cb.lower(root.get("login")),term,'\\')),page(page,size)).map(this::operatorResponse);}
 private static String escape(String q){return q.trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_");}
 private void version(Long supplied,long actual){if(supplied==null)throw new BadRequestException("Требуется version");if(supplied!=actual)throw new ConflictException("Запись уже изменена. Обновите список");}
 public ClientResponse saveClient(Integer id,ClientRequest r){
  String phone=PhoneNormalizer.normalize(r.phone());
  if(phone.replaceAll("[^0-9]","").length()<7)throw new BadRequestException("В телефоне должно быть не менее 7 цифр");
  CallClient c=id==null?new CallClient(r.fullName().trim(),phone,r.email()):clients.findById(id).orElseThrow(()->new NotFoundException("Клиент не найден"));
  if(id!=null)version(r.version(),c.getVersion());
  clients.findOtherByNormalizedPhone(phone,id==null?-1:id).ifPresent(other->{throw new ConflictException("Телефон уже принадлежит другому клиенту");});
  c.update(r.fullName().trim(),phone,r.email()==null||r.email().isBlank()?null:r.email().trim());
  c=clients.saveAndFlush(c);record("CLIENT",c.getId(),id==null?"CREATED":"UPDATED");return clientResponse(c);
 }
 public OperatorResponse saveOperator(Integer id,OperatorRequest r){
  var c=centers.findById(r.centerId()).orElseThrow(()->new BadRequestException("Колл-центр не найден"));
  CallOperator o=id==null?new CallOperator(r.fullName().trim(),r.login(),r.centerId(),r.active()):operators.lockById(id).orElseThrow(()->new NotFoundException("Оператор не найден"));
  if(id!=null)version(r.version(),o.getVersion());
  if(!c.isActive()&&(id==null||!c.getId().equals(o.getCenterId())))throw new BadRequestException("Нельзя назначить отключённый колл-центр");
  if(jdbc.queryForObject("select count(*) from operators where login=? and id<>?",Long.class,r.login(),id==null?-1:id)>0)throw new ConflictException("Логин уже занят");
  o.update(r.fullName().trim(),r.login(),r.centerId(),r.active());
  o=operators.saveAndFlush(o);record("OPERATOR",o.getId(),id==null?"CREATED":"UPDATED");return operatorResponse(o);
 }
 public void deleteClient(int id,long v){var c=clients.findById(id).orElseThrow(()->new NotFoundException("Клиент не найден"));version(v,c.getVersion());
  if(jdbc.queryForObject("select (select count(*) from appeal where legacy_client_id=?)+(select count(*) from appeals where client_id=?)",Long.class,id,id)>0)throw new ConflictException("У клиента есть обращения. Удаление запрещено для сохранения истории");
  clients.delete(c);clients.flush();record("CLIENT",id,"DELETED");}
 public void deleteOperator(int id,long v){var o=operators.lockById(id).orElseThrow(()->new NotFoundException("Оператор не найден"));version(v,o.getVersion());
  if(jdbc.queryForObject("select (select count(*) from appeal where operator_id=?)+(select count(*) from appeals where operator_id=?)",Long.class,id,id)>0)throw new ConflictException("У оператора есть обращения. Отключите его вместо удаления");
  operators.delete(o);operators.flush();record("OPERATOR",id,"DELETED");}
 private void record(String type,int id,String action){audit.record(type+"_"+action,type,null,"Изменение справочника: "+id,Map.of("recordId",id));}
 private ClientResponse clientResponse(CallClient c){return new ClientResponse(c.getId(),c.getFullName(),c.getPhone(),c.getEmail(),c.getVersion());}
 private OperatorResponse operatorResponse(CallOperator o){return new OperatorResponse(o.getId(),o.getFullName(),o.getLogin(),o.getCenterId(),centers.findById(o.getCenterId()).map(CallCenter::getCode).orElse("—"),o.isActive(),o.getVersion());}
}
