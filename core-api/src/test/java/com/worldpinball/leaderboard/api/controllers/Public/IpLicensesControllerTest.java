package com.worldpinball.leaderboard.api.controllers.Public;

import com.worldpinball.leaderboard.application.features.iplicenses.Public.getalliplicenses.GetAllIpLicensesHandler;
import com.worldpinball.leaderboard.application.features.iplicenses.Public.getalliplicenses.GetAllIpLicensesQuery;
import com.worldpinball.leaderboard.application.features.iplicenses.Public.getalliplicenses.GetAllIpLicensesResult;
import com.worldpinball.leaderboard.application.features.iplicenses.Public.getalliplicenses.IpLicenseDto;
import com.worldpinball.leaderboard.application.features.iplicenses.Public.getalliplicenses.PublisherDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IpLicensesController Tests")
class IpLicensesControllerTest {

    @Mock
    private GetAllIpLicensesHandler handler;

    private IpLicensesController sut;

    @BeforeEach
    void setUp() {
        sut = new IpLicensesController(handler);
    }

    @Test
    @DisplayName("Returns 200 OK with IP licenses when licenses exist")
    void getAll_whenIpLicensesExist_shouldReturnOk() {
        List<IpLicenseDto> licenses = List.of(
                new IpLicenseDto(UUID.randomUUID(), "Godzilla", "godzilla", new PublisherDto(UUID.randomUUID(), "Stern", "stern"))
        );
        when(handler.handle(any(GetAllIpLicensesQuery.class)))
                .thenReturn(new GetAllIpLicensesResult.Success(licenses));

        ResponseEntity<?> response = sut.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(licenses);
    }

    @Test
    @DisplayName("Returns 404 NOT_FOUND when no IP licenses exist")
    void getAll_whenNoIpLicensesExist_shouldReturnNotFound() {
        when(handler.handle(any(GetAllIpLicensesQuery.class)))
                .thenReturn(new GetAllIpLicensesResult.NotFound());

        ResponseEntity<?> response = sut.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
