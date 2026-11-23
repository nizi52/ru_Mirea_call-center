package ru.mirea.callcenter.service;
import ru.mirea.callcenter.error.BadRequestException;
/** Единый ключ клиента для REST, форм и CSV. */
public final class PhoneNormalizer {
 private PhoneNormalizer() {}
 public static String normalize(String value) {
  if(value==null || !value.trim().matches("\\+?[0-9() -]+")) throw new BadRequestException("Некорректный телефон");
  String digits=value.replaceAll("[^0-9]", "");
  if(digits.length()<7 || digits.length()>15) throw new BadRequestException("Телефон должен содержать от 7 до 15 цифр");
  if(digits.length()==11 && digits.startsWith("8")) digits="7"+digits.substring(1);
  return "+"+digits;
 }
}
