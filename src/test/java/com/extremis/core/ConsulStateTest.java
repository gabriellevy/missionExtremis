package com.extremis.core;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsulStateTest {

    private Instant now = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void recruitIsLimitedToOnePerDay() {
        ConsulState consul = new ConsulState();
        consul.recruit(new Character("a", "Alice"), now);
        assertThat(consul.canRecruit(now.plusSeconds(3600))).isFalse();
        assertThat(consul.canRecruit(now.plusSeconds(86400))).isTrue();
    }

    @Test
    void recruitingTwiceWithinCooldownThrows() {
        ConsulState consul = new ConsulState();
        consul.recruit(new Character("a", "Alice"), now);
        assertThatThrownBy(() -> consul.recruit(new Character("b", "Bob"), now.plusSeconds(60)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void jokersAccumulateAndCanBeSpent() {
        ConsulState consul = new ConsulState();
        consul.addJoker();
        assertThat(consul.jokers()).isEqualTo(2);
        assertThat(consul.spendJoker()).isTrue();
        assertThat(consul.jokers()).isEqualTo(1);
    }

    @Test
    void spendJokerFailsWhenNoneLeft() {
        ConsulState consul = new ConsulState();
        consul.spendJoker();
        assertThat(consul.spendJoker()).isFalse();
    }

    @Test
    void jokersSontPlafonnesATrois() {
        ConsulState consul = new ConsulState();
        for (int i = 0; i < 10; i++) {
            consul.addJoker();
        }
        assertThat(ConsulState.MAX_JOKERS).isEqualTo(3);
        assertThat(consul.jokers()).isEqualTo(3);
    }

    @Test
    void unJokerEstGagneParJourCalendaireAuPlus() {
        ConsulState consul = new ConsulState();
        assertThat(consul.gagnerJokerQuotidien(now)).isEqualTo(2);
        assertThat(consul.gagnerJokerQuotidien(now.plusSeconds(3600))).isEqualTo(2);
        assertThat(consul.gagnerJokerQuotidien(now.plus(java.time.Duration.ofDays(1)))).isEqualTo(3);
        assertThat(consul.gagnerJokerQuotidien(now.plus(java.time.Duration.ofDays(2)))).isEqualTo(3);
    }
}
