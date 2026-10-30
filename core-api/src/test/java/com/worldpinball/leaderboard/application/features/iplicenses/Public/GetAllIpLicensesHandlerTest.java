package com.worldpinball.leaderboard.application.features.iplicenses.Public;

import com.worldpinball.leaderboard.application.features.iplicenses.Public.getalliplicenses.GetAllIpLicensesHandlerImpl;
import com.worldpinball.leaderboard.application.features.iplicenses.Public.getalliplicenses.GetAllIpLicensesQuery;
import com.worldpinball.leaderboard.application.features.iplicenses.Public.getalliplicenses.GetAllIpLicensesResult;
import com.worldpinball.leaderboard.domain.entities.IpLicense;
import com.worldpinball.leaderboard.domain.entities.Publisher;
import com.worldpinball.leaderboard.infrastructure.IpLicenseRepository;
import com.worldpinball.leaderboard.infrastructure.PublisherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAllIpLicensesHandler Tests")
class GetAllIpLicensesHandlerTest {

    @Mock
    private IpLicenseRepository ipLicenseRepository;

    @Mock
    private PublisherRepository publisherRepository;

    private GetAllIpLicensesHandlerImpl sut;

    @BeforeEach
    void setUp() {
        sut = new GetAllIpLicensesHandlerImpl(ipLicenseRepository, publisherRepository);
    }

    @Test
    @DisplayName("Returns Success with list of IP licenses when active licenses exist")
    void handle_whenIpLicensesExist_shouldReturnSuccess() {
        UUID publisherId = UUID.randomUUID();
        Publisher publisher = new Publisher("Stern", "stern", Instant.now(), publisherId);

        UUID ip1Id = UUID.randomUUID();
        UUID ip2Id = UUID.randomUUID();
        IpLicense ip1 = new IpLicense("Godzilla", "godzilla", publisherId, true, Instant.now(), ip1Id);
        IpLicense ip2 = new IpLicense("Iron Maiden", "iron-maiden", publisherId, true, Instant.now(), ip2Id);

        when(ipLicenseRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(ip1, ip2));
        when(publisherRepository.findAllById(anySet())).thenReturn(List.of(publisher));

        GetAllIpLicensesResult result = sut.handle(new GetAllIpLicensesQuery());

        assertThat(result).isInstanceOf(GetAllIpLicensesResult.Success.class);
        GetAllIpLicensesResult.Success success = (GetAllIpLicensesResult.Success) result;
        assertThat(success.ipLicenses()).hasSize(2);
        assertThat(success.ipLicenses().get(0).name()).isEqualTo("Godzilla");
        assertThat(success.ipLicenses().get(0).publisher().name()).isEqualTo("Stern");
    }

    @Test
    @DisplayName("Returns NotFound when repository contains no active IP licenses")
    void handle_whenNoIpLicensesExist_shouldReturnNotFound() {
        when(ipLicenseRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of());

        GetAllIpLicensesResult result = sut.handle(new GetAllIpLicensesQuery());

        assertThat(result).isInstanceOf(GetAllIpLicensesResult.NotFound.class);
    }
}
