package ru.mirea.callcenter.controller;
import ru.mirea.callcenter.repository.CallOperatorRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.*;
@RestController @RequestMapping("/api/operators") public class OperatorController {
 private final CallOperatorRepository operators;
 public OperatorController(CallOperatorRepository operators){this.operators=operators;}
 public record OperatorResponse(Integer id,String fullName,boolean active,UUID centerId){}
 @GetMapping @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN')")
 public List<OperatorResponse> list(){return operators.findAll().stream().map(o->new OperatorResponse(o.getId(),o.getFullName(),o.isActive(),o.getCenterId())).toList();}
}
