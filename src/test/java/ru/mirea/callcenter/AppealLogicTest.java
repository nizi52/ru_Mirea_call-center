package ru.mirea.callcenter;

import org.junit.jupiter.api.Test;
import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.model.Appeal;
import ru.mirea.callcenter.model.AppealStatus;
import ru.mirea.callcenter.service.AppealService;
import ru.mirea.callcenter.service.AppealStatistics;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Тесты бизнес-логики, которым не нужна база данных. */
class AppealLogicTest {
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 9, 1, 9, 0);

    private static Appeal appeal(int id, String category, AppealStatus status, String operator, LocalDateTime closedAt) {
        Appeal a = new Appeal(id, operator == null ? null : 1, 1, "Клиент", "+7000", "Тема", category, status, T0, closedAt);
        a.setOperatorName(operator);
        return a;
    }

    @Test
    void statisticsCountsEveryIndicator() {
        List<Appeal> list = List.of(
                appeal(1, "TECH", AppealStatus.NEW, null, null),
                appeal(2, "TECH", AppealStatus.ESCALATED, "Анна", null),
                appeal(3, "BILLING", AppealStatus.CLOSED, "Анна", T0.plusMinutes(90)),
                appeal(4, "BILLING", AppealStatus.CLOSED, "Пётр", T0.plusMinutes(30)));

        AppealStatistics s = AppealStatistics.of(list);

        assertThat(s.total()).isEqualTo(4);
        assertThat(s.open()).isEqualTo(2);
        assertThat(s.byStatus()).containsEntry(AppealStatus.CLOSED, 2L).containsEntry(AppealStatus.NEW, 1L);
        assertThat(s.byCategory()).containsEntry("TECH", 2L).containsEntry("BILLING", 2L);
        assertThat(s.byOperator()).containsEntry("Анна", 2L).containsEntry("Пётр", 1L).doesNotContainKey(null);
        assertThat(s.avgCloseMinutes()).isEqualTo(60.0);
        assertThat(s.escalatedPercent()).isEqualTo(25.0);
    }

    @Test
    void statisticsOfEmptyListIsZero() {
        AppealStatistics s = AppealStatistics.of(List.of());

        assertThat(s.total()).isZero();
        assertThat(s.avgCloseMinutes()).isZero();
        assertThat(s.escalatedPercent()).isZero();
    }

    @Test
    void statusChainHasNoRollbackAndNoJumps() {
        AppealService service = new AppealService(null, null, null);

        assertThat(service.nextStatuses(AppealStatus.NEW)).containsExactly(AppealStatus.IN_PROGRESS);
        assertThat(service.nextStatuses(AppealStatus.IN_PROGRESS))
                .containsExactly(AppealStatus.ESCALATED, AppealStatus.RESOLVED);
        assertThat(service.nextStatuses(AppealStatus.ESCALATED)).containsExactly(AppealStatus.RESOLVED);
        assertThat(service.nextStatuses(AppealStatus.RESOLVED)).containsExactly(AppealStatus.CLOSED);
        assertThat(service.nextStatuses(AppealStatus.CLOSED)).isEmpty();
    }

    @Test
    void businessExceptionKeepsFieldCode() {
        BusinessException e = new BusinessException("topic", "Поле «Тема»: введите тему обращения.");

        assertThat(e.getField()).isEqualTo("topic");
        assertThat(new BusinessException("без поля").getField()).isNull();
    }
}
