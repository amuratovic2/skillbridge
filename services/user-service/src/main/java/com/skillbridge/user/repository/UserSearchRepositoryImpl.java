package com.skillbridge.user.repository;

import com.skillbridge.user.model.Skill;
import com.skillbridge.user.model.User;
import com.skillbridge.user.model.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class UserSearchRepositoryImpl implements UserSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<User> searchActiveUsers(
        String query,
        UserRole role,
        String country,
        String skill,
        Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<User> userQuery = cb.createQuery(User.class);
        Root<User> user = userQuery.from(User.class);
        List<Predicate> predicates = buildPredicates(cb, userQuery, user, query, role, country, skill);

        userQuery.select(user)
            .where(predicates.toArray(Predicate[]::new))
            .distinct(skill != null);

        if (pageable.getSort().isSorted()) {
            List<Order> orders = pageable.getSort().stream()
                .map(sortOrder -> sortOrder.isAscending()
                    ? cb.asc(user.get(sortOrder.getProperty()))
                    : cb.desc(user.get(sortOrder.getProperty())))
                .toList();
            userQuery.orderBy(orders);
        }

        List<User> users = entityManager.createQuery(userQuery)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        long total = countUsers(cb, query, role, country, skill);
        return new PageImpl<>(users, pageable, total);
    }

    private long countUsers(
        CriteriaBuilder cb,
        String query,
        UserRole role,
        String country,
        String skill
    ) {
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<User> user = countQuery.from(User.class);
        List<Predicate> predicates = buildPredicates(cb, countQuery, user, query, role, country, skill);

        countQuery.select(skill == null ? cb.count(user) : cb.countDistinct(user))
            .where(predicates.toArray(Predicate[]::new));

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private List<Predicate> buildPredicates(
        CriteriaBuilder cb,
        CriteriaQuery<?> criteriaQuery,
        Root<User> user,
        String query,
        UserRole role,
        String country,
        String skill
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isTrue(user.get("isActive")));

        if (query != null) {
            String pattern = "%" + query.toLowerCase() + "%";
            predicates.add(cb.or(
                cb.like(cb.lower(user.get("username")), pattern),
                cb.like(cb.lower(user.get("email")), pattern),
                cb.like(cb.lower(cb.coalesce(user.get("firstName"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(user.get("lastName"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(user.get("bio"), "")), pattern)
            ));
        }

        if (role != null) {
            predicates.add(cb.equal(user.get("role"), role));
        }

        if (country != null) {
            predicates.add(cb.equal(cb.lower(cb.coalesce(user.get("country"), "")), country.toLowerCase()));
        }

        if (skill != null) {
            Join<User, Skill> skills = user.join("skills", JoinType.INNER);
            predicates.add(cb.equal(cb.lower(skills.get("name")), skill.toLowerCase()));
            criteriaQuery.distinct(true);
        }

        return predicates;
    }
}
