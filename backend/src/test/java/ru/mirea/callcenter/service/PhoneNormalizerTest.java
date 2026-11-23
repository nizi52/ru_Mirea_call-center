package ru.mirea.callcenter.service;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class PhoneNormalizerTest {
 @Test void russianFormatsMatch(){
  for(String p:new String[]{"+79995550122","+7-999-555-01-22","8 (999) 555-01-22","79995550122"})
   assertThat(PhoneNormalizer.normalize(p)).isEqualTo("+79995550122");
 }
 @Test void internationalCodePreserved(){assertThat(PhoneNormalizer.normalize("+44 20 7946 0958")).isEqualTo("+442079460958");}
 @Test void invalidPhonesRejected(){for(String p:new String[]{"-------","123","+7abc9995550122","+1234567890123456"})assertThatThrownBy(()->PhoneNormalizer.normalize(p)).isInstanceOf(ru.mirea.callcenter.error.BadRequestException.class);}
}
