package com.digitaltherapy.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RestPageTest {

    @Test
    @DisplayName("Default RestPage has empty/null state")
    void defaultRestPage_HasDefaultValues() {
        RestPage<String> page = new RestPage<>();
        assertThat(page.getContent()).isNull();
        assertThat(page.getTotalPages()).isZero();
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getNumber()).isZero();
        assertThat(page.getSize()).isZero();
        assertThat(page.getNumberOfElements()).isZero();
        assertThat(page.isFirst()).isFalse();
        assertThat(page.isLast()).isFalse();
        assertThat(page.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Setters and getters work correctly")
    void settersAndGetters_WorkCorrectly() {
        RestPage<String> page = new RestPage<>();
        List<String> content = List.of("item1", "item2", "item3");

        page.setContent(content);
        page.setTotalPages(5);
        page.setTotalElements(50);
        page.setNumber(2);
        page.setSize(10);
        page.setNumberOfElements(3);
        page.setFirst(false);
        page.setLast(false);
        page.setEmpty(false);

        assertThat(page.getContent()).isEqualTo(content);
        assertThat(page.getTotalPages()).isEqualTo(5);
        assertThat(page.getTotalElements()).isEqualTo(50);
        assertThat(page.getNumber()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(10);
        assertThat(page.getNumberOfElements()).isEqualTo(3);
        assertThat(page.isFirst()).isFalse();
        assertThat(page.isLast()).isFalse();
        assertThat(page.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("hasPrevious returns false when first page")
    void hasPrevious_FirstPage_ReturnsFalse() {
        RestPage<String> page = new RestPage<>();
        page.setFirst(true);
        assertThat(page.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("hasPrevious returns true when not first page")
    void hasPrevious_NotFirstPage_ReturnsTrue() {
        RestPage<String> page = new RestPage<>();
        page.setFirst(false);
        assertThat(page.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("hasNext returns false when last page")
    void hasNext_LastPage_ReturnsFalse() {
        RestPage<String> page = new RestPage<>();
        page.setLast(true);
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("hasNext returns true when not last page")
    void hasNext_NotLastPage_ReturnsTrue() {
        RestPage<String> page = new RestPage<>();
        page.setLast(false);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("First page of results has hasPrevious=false and hasNext=true")
    void firstPageOfMultiple_CorrectNavigation() {
        RestPage<String> page = new RestPage<>();
        page.setFirst(true);
        page.setLast(false);
        page.setNumber(0);
        page.setTotalPages(3);

        assertThat(page.hasPrevious()).isFalse();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("Last page of results has hasPrevious=true and hasNext=false")
    void lastPageOfMultiple_CorrectNavigation() {
        RestPage<String> page = new RestPage<>();
        page.setFirst(false);
        page.setLast(true);
        page.setNumber(2);
        page.setTotalPages(3);

        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("Single page has hasPrevious=false and hasNext=false")
    void singlePage_NoNavigation() {
        RestPage<String> page = new RestPage<>();
        page.setFirst(true);
        page.setLast(true);
        page.setNumber(0);
        page.setTotalPages(1);

        assertThat(page.hasPrevious()).isFalse();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("Empty page is marked as empty")
    void emptyPage_IsMarkedEmpty() {
        RestPage<String> page = new RestPage<>();
        page.setContent(List.of());
        page.setEmpty(true);
        page.setTotalElements(0);
        page.setFirst(true);
        page.setLast(true);

        assertThat(page.isEmpty()).isTrue();
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }
}
