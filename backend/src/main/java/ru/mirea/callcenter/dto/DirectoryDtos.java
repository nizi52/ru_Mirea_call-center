package ru.mirea.callcenter.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
public final class DirectoryDtos {
 public record ClientRequest(@NotBlank @Size(max=150) String fullName,
   @NotBlank @Pattern(regexp="^\\+?[0-9() -]{7,30}$") String phone,
   @Email @Size(max=150) String email, Long version) {}
 public record ClientResponse(Integer id,String fullName,String phone,String email,long version) {}
 public record OperatorRequest(@NotBlank @Size(max=150) String fullName,
   @NotBlank @Pattern(regexp="[a-zA-Z0-9_.-]{3,50}") String login,
   @NotNull UUID centerId, boolean active, Long version) {}
 public record OperatorResponse(Integer id,String fullName,String login,UUID centerId,String centerCode,boolean active,long version) {}
}
