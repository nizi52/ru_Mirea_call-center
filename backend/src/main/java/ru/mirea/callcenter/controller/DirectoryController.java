package ru.mirea.callcenter.controller;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import ru.mirea.callcenter.dto.DirectoryDtos.*;
import ru.mirea.callcenter.service.DirectoryService;
@RestController @RequestMapping("/api/directory")
@PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN')")
public class DirectoryController {
 private final DirectoryService service;
 public DirectoryController(DirectoryService s){service=s;}
 @GetMapping("/clients") public Page<ClientResponse> clients(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return service.clients(q,page,size);}
 @GetMapping("/operators") public Page<OperatorResponse> operators(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return service.operators(q,page,size);}
 @PostMapping("/clients") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('EDITOR','ADMIN')") public ClientResponse createClient(@Valid @RequestBody ClientRequest r){return service.saveClient(null,r);}
 @PutMapping("/clients/{id}") @PreAuthorize("hasAnyRole('EDITOR','ADMIN')") public ClientResponse updateClient(@PathVariable int id,@Valid @RequestBody ClientRequest r){return service.saveClient(id,r);}
 @PostMapping("/operators") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')") public OperatorResponse createOperator(@Valid @RequestBody OperatorRequest r){return service.saveOperator(null,r);}
 @PutMapping("/operators/{id}") @PreAuthorize("hasRole('ADMIN')") public OperatorResponse updateOperator(@PathVariable int id,@Valid @RequestBody OperatorRequest r){return service.saveOperator(id,r);}
 @DeleteMapping("/clients/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") public void deleteClient(@PathVariable int id,@RequestParam long version){service.deleteClient(id,version);}
 @DeleteMapping("/operators/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") public void deleteOperator(@PathVariable int id,@RequestParam long version){service.deleteOperator(id,version);}
}
