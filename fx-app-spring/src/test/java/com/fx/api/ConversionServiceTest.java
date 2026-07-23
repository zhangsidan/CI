package com.fx.api;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fx.api.model.Rate;
import com.fx.api.repo.RateRepository;
import com.fx.api.service.ConversionService;
import com.fx.api.service.UnknownPairException;

@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {

    @Mock RateRepository repo;

    @Test void convertsAndRoundsToTwoDecimals() {
        when(repo.findLatestForPair("EUR", "USD"))
            .thenReturn(Optional.of(new Rate(1, "EUR", "USD", 1.0818, LocalDate.of(2026, 1, 12), null)));
        var result = new ConversionService(repo).convert("EUR", "USD", 123.45);
        assertEquals(133.55, result.converted(), 1e-9);   // 123.45 * 1.0818 = 133.5482 -> 133.55
        assertEquals("EUR/USD", result.pair());
        assertEquals(1.34, result.fee(), 1e-9);            // retail 1% of 133.55, rounded
        assertEquals(132.21, result.net(), 1e-9);
    }

    @Test void unknownPairThrows() {
        when(repo.findLatestForPair("AAA", "BBB")).thenReturn(Optional.empty());
        assertThrows(UnknownPairException.class,
            () -> new ConversionService(repo).convert("AAA", "BBB", 10));
    }
}
