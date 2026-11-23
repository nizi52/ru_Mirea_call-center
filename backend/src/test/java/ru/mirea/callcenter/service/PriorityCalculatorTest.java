package ru.mirea.callcenter.service;
import org.junit.jupiter.api.Test;
import ru.mirea.callcenter.domain.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
class PriorityCalculatorTest {
 private final PriorityCalculator calc=new PriorityCalculator();
 @Test void boundaries(){
 assertThat(calc.calculate(AppealStatus.NEW,new BigDecimal("80"),true)).isEqualTo(PriorityLevel.CRITICAL);
 assertThat(calc.calculate(AppealStatus.NEW,new BigDecimal("79.99"),true)).isEqualTo(PriorityLevel.HIGH);
 assertThat(calc.calculate(AppealStatus.NEW,new BigDecimal("39.99"),false)).isEqualTo(PriorityLevel.LOW);
 assertThat(calc.calculate(AppealStatus.NEW,new BigDecimal("40"),false)).isEqualTo(PriorityLevel.MEDIUM);
 assertThat(calc.calculate(AppealStatus.RESOLVED,new BigDecimal("100"),true)).isEqualTo(PriorityLevel.NOT_APPLICABLE);
 }
 @Test void statusRules(){
 assertThat(AppealService.allowed(AppealStatus.NEW,AppealStatus.CLOSED)).isFalse();
 assertThat(AppealService.allowed(AppealStatus.IN_PROGRESS,AppealStatus.RESOLVED)).isTrue();
 assertThat(AppealService.allowed(AppealStatus.CLOSED,AppealStatus.NEW)).isFalse();
 }
}
