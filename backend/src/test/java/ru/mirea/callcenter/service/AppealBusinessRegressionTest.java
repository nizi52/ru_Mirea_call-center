package ru.mirea.callcenter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import ru.mirea.callcenter.config.AppProperties;
import ru.mirea.callcenter.domain.*;
import ru.mirea.callcenter.dto.AppealDtos.*;
import ru.mirea.callcenter.error.BadRequestException;
import ru.mirea.callcenter.filter.SmartFilterParser;
import ru.mirea.callcenter.repository.*;
import ru.mirea.callcenter.security.CurrentUserService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppealBusinessRegressionTest {
    AppealRepository appeals = mock(AppealRepository.class);
    CallCenterRepository centers = mock(CallCenterRepository.class);
    CallOperatorRepository operators = mock(CallOperatorRepository.class);
    AppealService service;
    CallCenter center = new CallCenter(UUID.randomUUID(), "TEST", "Test", 24);
    Appeal appeal = new Appeal(UUID.randomUUID(), "Topic", "Client", null);
    @BeforeEach void setup() {
        service = new AppealService(appeals, mock(AppealNoteRepository.class), centers,
            mock(CurrentUserService.class), new SmartFilterParser(), new AppealMapper(),
            new PriorityCalculator(), mock(AuditService.class), mock(AppProperties.class),
            operators, mock(PlatformTransactionManager.class), mock(CallClientRepository.class));
        appeal.setCallCenter(center);
        appeal.setOperatorId(1);
        ReflectionTestUtils.setField(appeal, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        when(centers.findById(center.getId())).thenReturn(Optional.of(center));
        var operator = mock(CallOperator.class);
        when(operator.isActive()).thenReturn(true);
        when(operators.lockById(1)).thenReturn(Optional.of(operator));
        when(operators.findById(1)).thenReturn(Optional.of(operator));
    }
    @Test void existingInactiveReferenceCanBeKeptButNotAssigned() {
        center.setActive(false);
        assertThat((CallCenter) ReflectionTestUtils.invokeMethod(service, "resolveCallCenter", center.getId(), appeal)).isSameAs(center);
        var fresh = new Appeal(UUID.randomUUID(), "New", "Client", null);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "resolveCallCenter", center.getId(), fresh)).isInstanceOf(BadRequestException.class);
    }
    @Test void repeatedBulkClosePreservesOriginalTimestamp() {
        appeal.setStatus(AppealStatus.CLOSED);
        Instant original = Instant.parse("2026-02-01T00:00:00Z");
        appeal.setClosedAt(original);
        ReflectionTestUtils.invokeMethod(service, "applyBulk", appeal,
            new BulkRequest(Set.of(appeal.getId()), BulkAction.SET_STATUS, AppealStatus.CLOSED, null), null);
        assertThat(appeal.getClosedAt()).isEqualTo(original);
    }
    @Test void archivedEditDoesNotConsumeCapacityButRestoreDoes() {
        appeal.setArchived(true);
        when(appeals.activeLoad(1, appeal.getId())).thenReturn(5L);
        assertThatCode(() -> ReflectionTestUtils.invokeMethod(service, "validateBusiness", appeal, AppealStatus.NEW, 1, center.getId(), false)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "validateBusiness", appeal, AppealStatus.NEW, 1, center.getId(), true)).isInstanceOf(BadRequestException.class);
    }
    @Test void previewRejectsOperatorAtCapacity() {
        when(appeals.findByExternalIdIgnoreCase("TEST")).thenReturn(Optional.of(appeal));
        when(appeals.activeLoad(1, appeal.getId())).thenReturn(5L);
        var request = new AppealRequest("TEST", "Topic", "Client", "TECH", null, "+79991112233", null, center.getId(), AppealStatus.NEW, BigDecimal.TEN, false, Set.of(), 0L, 1);
        assertThatThrownBy(() -> service.validateImport(request)).isInstanceOf(BadRequestException.class).hasMessageContaining("пять");
    }
}
