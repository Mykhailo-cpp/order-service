package com.example.order_service.util;

import com.example.order_service.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestFactoryTest {

    @Test
    void defaultSort_isCreatedAtDescWithIdTieBreaker() {
        Pageable p = PageRequestFactory.of(0, 20, null);

        assertThat(p.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(p.getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void directionIsOptionalAndDefaultsToAsc() {
        Pageable p = PageRequestFactory.of(2, 50, "product");

        assertThat(p.getPageNumber()).isEqualTo(2);
        assertThat(p.getPageSize()).isEqualTo(50);
        assertThat(p.getSort().getOrderFor("product").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void directionIsCaseInsensitive() {
        Pageable p = PageRequestFactory.of(0, 10, "quantity,DESC");

        assertThat(p.getSort().getOrderFor("quantity").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void collectsAllErrorsAtOnce() {
        assertThatThrownBy(() -> PageRequestFactory.of(-1, 0, "nope"))
                .isInstanceOfSatisfying(ValidationException.class, ex ->
                        assertThat(ex.getErrors()).containsKeys("page", "size", "sort"));
    }

    @Test
    void rejectsBadDirection() {
        assertThatThrownBy(() -> PageRequestFactory.of(0, 10, "createdAt,sideways"))
                .isInstanceOfSatisfying(ValidationException.class, ex ->
                        assertThat(ex.getErrors().get("sort")).contains("asc"));
    }

    @Test
    void rejectsSizeAboveMax() {
        assertThatThrownBy(() -> PageRequestFactory.of(0, PageRequestFactory.MAX_SIZE + 1, null))
                .isInstanceOf(ValidationException.class);
    }
}