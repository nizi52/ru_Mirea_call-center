package ru.mirea.callcenter.service;
import org.springframework.stereotype.Component;
import ru.mirea.callcenter.domain.*;
import java.math.BigDecimal;
/** Приоритет: влияние 0..100, срочность и текущий этап обработки. */
@Component public class PriorityCalculator {
 public PriorityLevel calculate(AppealStatus status, BigDecimal impact, boolean urgent) {
  if(status==AppealStatus.CLOSED || status==AppealStatus.RESOLVED) return PriorityLevel.NOT_APPLICABLE;
  double score=impact==null?0:impact.doubleValue();
  if(urgent && score>=80) return PriorityLevel.CRITICAL;
  if(urgent || score>=80 || status==AppealStatus.ESCALATED) return PriorityLevel.HIGH;
  return score>=40 ? PriorityLevel.MEDIUM : PriorityLevel.LOW;
 }
}
