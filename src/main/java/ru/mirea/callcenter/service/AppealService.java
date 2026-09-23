package ru.mirea.callcenter.service;

import ru.mirea.callcenter.exception.*;
import ru.mirea.callcenter.model.*;
import ru.mirea.callcenter.repository.AppealRepository;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class AppealService {
    private static final Set<String> CATEGORIES=Set.of("TECH","COMPLAINT","CONSULTATION","BILLING");
    private final AppealRepository repository;
    private final OperatorService operatorService;
    public AppealService(AppealRepository repository, OperatorService operatorService) { this.repository=repository;this.operatorService=operatorService; }
    public List<Appeal> getAll() throws SQLException { return repository.findAll(); }
    public Appeal getById(int id)throws SQLException {return repository.findById(id).orElseThrow(()->new EntityNotFoundException("Обращение с ID " + id + " не найдено."));}
    public Appeal create(Appeal appeal)throws SQLException { appeal.setStatus(AppealStatus.NEW);appeal.setCreatedAt(LocalDateTime.now());appeal.setClosedAt(null);validateFields(appeal);validateAssignment(appeal, null);return repository.save(appeal); }
    public Appeal updateDetails(Appeal edited)throws SQLException { Appeal old=getById(edited.getId()); edited.setStatus(old.getStatus());edited.setCreatedAt(old.getCreatedAt());edited.setClosedAt(old.getClosedAt());validateFields(edited);validateAssignment(edited, old);return repository.save(edited); }
    public Appeal changeStatus(int id, AppealStatus next)throws SQLException {
        Appeal appeal=getById(id); if(!allowed(appeal.getStatus(),next))throw new BusinessException("Переход из " + appeal.getStatus()+" в "+next+" запрещён.");
        if(next==AppealStatus.CLOSED&&appeal.getOperatorId()==null)throw new BusinessException("Нельзя закрыть обращение без назначенного оператора.");
        appeal.setStatus(next); if(next==AppealStatus.CLOSED) appeal.setClosedAt(LocalDateTime.now()); return repository.save(appeal);
    }
    public void delete(int id)throws SQLException {if(!repository.deleteById(id))throw new EntityNotFoundException("Обращение с ID " + id + " не найдено.");}
    public List<Appeal> searchClient(String query)throws SQLException {String q=query.toLowerCase();return getAll().stream().filter(a->a.getClientName().toLowerCase().contains(q)||a.getClientPhone().contains(query)).toList();}
    public List<Appeal> searchTopic(String query)throws SQLException {return getAll().stream().filter(a->a.getTopic().toLowerCase().contains(query.toLowerCase())).toList();}
    public List<Appeal> filterByStatus(AppealStatus status)throws SQLException{return getAll().stream().filter(a->a.getStatus()==status).toList();}
    public List<Appeal> filterByCreatedRange(LocalDateTime from,LocalDateTime to)throws SQLException{return getAll().stream().filter(a->!a.getCreatedAt().isBefore(from)&&!a.getCreatedAt().isAfter(to)).toList();}
    public List<Appeal> sortByCreatedAt()throws SQLException{return getAll().stream().sorted(Comparator.comparing(Appeal::getCreatedAt)).toList();}
    public List<Appeal> sortByStatus()throws SQLException{return getAll().stream().sorted(Comparator.comparing(Appeal::getStatus)).toList();}
    public String statistics()throws SQLException {
        List<Appeal> appeals=getAll(); Map<AppealStatus,Long> statuses=appeals.stream().collect(Collectors.groupingBy(Appeal::getStatus,()->new EnumMap<>(AppealStatus.class),Collectors.counting()));
        Map<String,Long> categories=appeals.stream().collect(Collectors.groupingBy(Appeal::getCategory,TreeMap::new,Collectors.counting()));
        Map<Integer,Long> workload=appeals.stream().filter(a->a.getOperatorId()!=null).collect(Collectors.groupingBy(Appeal::getOperatorId,TreeMap::new,Collectors.counting()));
        double avg=appeals.stream().filter(a->a.getClosedAt()!=null).mapToLong(a->java.time.Duration.between(a.getCreatedAt(),a.getClosedAt()).toMinutes()).average().orElse(0);
        String busiest=workload.entrySet().stream().max(Map.Entry.comparingByValue()).map(e->"оператор ID "+e.getKey()+" ("+e.getValue()+")").orElse("нет назначений");
        return "Всего обращений: "+appeals.size()+"\nПо статусам: "+statuses+"\nСреднее время обработки: %.1f мин.\nНагрузка по операторам: %s\nКатегории: %s\nНаибольшая нагрузка: %s".formatted(avg,workload,categories,busiest);
    }
    private void validateFields(Appeal a){if(blank(a.getClientName())||blank(a.getClientPhone())||blank(a.getTopic()))throw new BusinessException("Имя клиента, телефон и тема обязательны.");if(!CATEGORIES.contains(a.getCategory()))throw new BusinessException("Допустимые категории: "+CATEGORIES);}
    private void validateAssignment(Appeal a, Appeal previous)throws SQLException {if(a.getOperatorId()!=null){Operator operator=operatorService.getById(a.getOperatorId());if(!operator.isActive())throw new BusinessException("Нельзя назначить неактивного оператора.");boolean unchanged=previous!=null&&Objects.equals(previous.getOperatorId(),a.getOperatorId());if(!unchanged&&repository.countActiveByOperator(a.getOperatorId())>=5)throw new BusinessException("У оператора уже 5 активных обращений.");}}
    private boolean allowed(AppealStatus from,AppealStatus to){return (from==AppealStatus.NEW&&to==AppealStatus.IN_PROGRESS)||(from==AppealStatus.IN_PROGRESS&&(to==AppealStatus.ESCALATED||to==AppealStatus.RESOLVED))||(from==AppealStatus.ESCALATED&&to==AppealStatus.RESOLVED)||(from==AppealStatus.RESOLVED&&to==AppealStatus.CLOSED);}
    private boolean blank(String text){return text==null||text.isBlank();}
}
