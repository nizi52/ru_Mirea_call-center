package ru.mirea.callcenter.filter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.callcenter.domain.Appeal;
import ru.mirea.callcenter.domain.CallCenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Создаёт параметризованную JPA Specification из разобранного фильтра. */
public final class AppealSpecifications {
    private AppealSpecifications() { }

    public static Specification<Appeal> from(AppealFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Each center is singular; a matching tag is unique per appeal.
            // No DISTINCT: PostgreSQL may then sort by joined center.code.

            if (filter.archived() != null) predicates.add(cb.equal(root.get("archived"), filter.archived()));
            if (filter.status() != null) predicates.add(cb.equal(root.get("status"), filter.status()));
            if (filter.urgency() != null) predicates.add(cb.equal(root.get("urgent"), filter.urgency()));
            if (filter.priority() != null) predicates.add(cb.equal(root.get("priorityLevel"), filter.priority()));

            Join<Appeal, CallCenter> centerJoin = root.join("center", JoinType.LEFT);
            if (filter.center() != null) {
                predicates.add(cb.like(cb.lower(centerJoin.get("code")), "%" + escape(filter.center()) + "%", '\\'));
            }
            if (filter.tag() != null) {
                Join<Appeal, String> tagJoin = root.joinSet("tags", JoinType.LEFT);
                predicates.add(cb.equal(cb.lower(tagJoin), filter.tag().toLowerCase(Locale.ROOT)));
            }
            if (filter.freeText() != null && !filter.freeText().isBlank()) {
                String like = "%" + escape(filter.freeText()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("clientName")), like, '\\'),
                        cb.like(cb.lower(root.get("topic")), like, '\\'),
                        cb.like(cb.lower(root.get("category")), like, '\\'),
                        cb.like(cb.lower(root.get("phone")), like, '\\'),
                        cb.like(cb.lower(root.get("email")), like, '\\'),
                        cb.like(cb.lower(root.get("externalId")), like, '\\'),
                        cb.like(cb.lower(centerJoin.get("code")), like, '\\')));
            }
            filter.impacts().forEach(constraint -> predicates.add(switch (constraint.operator()) {
                case LT -> cb.lessThan(root.get("impactScore"), constraint.value());
                case LTE -> cb.lessThanOrEqualTo(root.get("impactScore"), constraint.value());
                case EQ -> cb.equal(root.get("impactScore"), constraint.value());
                case GTE -> cb.greaterThanOrEqualTo(root.get("impactScore"), constraint.value());
                case GT -> cb.greaterThan(root.get("impactScore"), constraint.value());
            }));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escape(String value) {
        return value.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
