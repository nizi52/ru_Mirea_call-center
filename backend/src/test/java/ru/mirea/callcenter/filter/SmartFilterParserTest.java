package ru.mirea.callcenter.filter;

import org.junit.jupiter.api.Test;
import ru.mirea.callcenter.domain.PriorityLevel;
import ru.mirea.callcenter.domain.AppealStatus;
import ru.mirea.callcenter.error.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Проверяет русские и английские команды умного фильтра. */
class SmartFilterParserTest {
    private final SmartFilterParser parser = new SmartFilterParser();

    @Test
    void parsesMixedSmartQuery() {
        AppealFilter result = parser.parse("Иванов колл-центр:CENTER-1 влияние>=80 срочно:да приоритет:высокий тег:сеть");
        assertThat(result.freeText()).isEqualTo("Иванов");
        assertThat(result.center()).isEqualTo("CENTER-1");
        assertThat(result.status()).isNull();
        assertThat(result.urgency()).isTrue();
        assertThat(result.priority()).isEqualTo(PriorityLevel.HIGH);
        assertThat(result.tag()).isEqualTo("сеть");
        assertThat(result.archived()).isFalse();
        assertThat(result.impacts()).hasSize(1);
    }

    @Test
    void supportsQuotedFreeTextAndStatus() {
        AppealFilter result = parser.parse("\"Иван Иванов\" status:in_progress archived:all");
        assertThat(result.freeText()).isEqualTo("Иван Иванов");
        assertThat(result.status()).isEqualTo(AppealStatus.IN_PROGRESS);
        assertThat(result.archived()).isNull();
    }

    @Test
    void rejectsOutOfRangeImpact() {
        assertThatThrownBy(() -> parser.parse("влияние>101"))
                .isInstanceOf(BadRequestException.class);
    }
}
