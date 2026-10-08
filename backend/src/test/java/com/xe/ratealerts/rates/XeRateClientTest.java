package com.xe.ratealerts.rates;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class XeRateClientTest {

    private MockRestServiceServer server;
    private XeRateClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new XeRateClient(restTemplate, "https://xe.test");
    }

    @Test
    void parsesAndRoundsRateAndReadsTimestamp() {
        server.expect(requestTo("https://xe.test/v1/convert_from.json/?from=USD&to=CAD"))
                .andRespond(withSuccess(
                        """
                                {"to":[{"mid":1.36517}],"timestamp":"2025-01-02T03:04:05Z"}
                                """,
                        MediaType.APPLICATION_JSON));

        Rate rate = client.getRate("USD/CAD");

        assertThat(rate.pair()).isEqualTo("USD/CAD");
        assertThat(rate.rate()).isEqualByComparingTo(new BigDecimal("1.3652"));
        assertThat(rate.asOf()).isEqualTo("2025-01-02T03:04:05Z");
        server.verify();
    }

    @Test
    void throwsRateUnavailableWhenServerReturnsError() {
        server.expect(requestTo("https://xe.test/v1/convert_from.json/?from=USD&to=CAD"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getRate("USD/CAD"))
                .isInstanceOf(RateUnavailableException.class)
                .hasCauseInstanceOf(org.springframework.web.client.HttpServerErrorException.class);
        server.verify();
    }

    @Test
    void throwsRateUnavailableWhenResponseHasNoRates() {
        server.expect(requestTo("https://xe.test/v1/convert_from.json/?from=USD&to=CAD"))
                .andRespond(withSuccess("{\"to\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getRate("USD/CAD"))
                .isInstanceOf(RateUnavailableException.class);
        server.verify();
    }

    @Test
    void rejectsPairWithoutExactlyTwoParts() {
        assertThatThrownBy(() -> client.getRate("USDCAD"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
