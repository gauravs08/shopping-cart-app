package com.shopping.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.app.dto.request.AddressRequest;
import com.shopping.app.dto.response.AddressResponse;
import com.shopping.app.security.CustomUserDetailsService;
import com.shopping.app.service.AddressService;
import com.shopping.app.support.SecuredControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.shopping.app.support.SecurityTestHelper.userJwt;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AddressController.class)
@SecuredControllerTest
@DisplayName("AddressController Tests")
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AddressService addressService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private AddressResponse testAddressResponse;
    private AddressRequest testAddressRequest;
    private UUID addressId;

    @BeforeEach
    void setUp() {
        addressId = UUID.randomUUID();

        testAddressResponse = AddressResponse.builder()
                .id(addressId)
                .label("Home")
                .street("Keilasatama 5")
                .city("Espoo")
                .state("Uusimaa")
                .postalCode("02150")
                .country("Finland")
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .build();

        testAddressRequest = AddressRequest.builder()
                .label("Home")
                .street("Keilasatama 5")
                .city("Espoo")
                .state("Uusimaa")
                .postalCode("02150")
                .country("Finland")
                .isDefault(true)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/addresses")
    class GetAddresses {

        @Test
        @DisplayName("Should return 200 with addresses list")
        void getAddresses_Returns200() throws Exception {
            when(addressService.getAddresses(anyString()))
                    .thenReturn(List.of(testAddressResponse));

            mockMvc.perform(get("/api/v1/addresses")
                            .with(userJwt("user@test.com")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].street", is("Keilasatama 5")))
                    .andExpect(jsonPath("$.data[0].city", is("Espoo")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/addresses")
    class AddAddress {

        @Test
        @DisplayName("Should return 201 with new address")
        void addAddress_Returns201() throws Exception {
            when(addressService.addAddress(anyString(), any(AddressRequest.class)))
                    .thenReturn(testAddressResponse);

            mockMvc.perform(post("/api/v1/addresses")
                            .with(userJwt("user@test.com"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testAddressRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id", is(addressId.toString())))
                    .andExpect(jsonPath("$.data.street", is("Keilasatama 5")))
                    .andExpect(jsonPath("$.data.country", is("Finland")));
        }

        @Test
        @DisplayName("Should return 400 for invalid input")
        void addAddress_InvalidInput_Returns400() throws Exception {
            AddressRequest invalidRequest = AddressRequest.builder()
                    .label("Home")
                    .street("")
                    .city("Espoo")
                    .postalCode("02150")
                    .build();

            mockMvc.perform(post("/api/v1/addresses")
                            .with(userJwt("user@test.com"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/addresses/{id}")
    class UpdateAddress {

        @Test
        @DisplayName("Should return 200 with updated address")
        void updateAddress_Returns200() throws Exception {
            AddressResponse updatedResponse = AddressResponse.builder()
                    .id(addressId)
                    .label("Office")
                    .street("Vaisalantie 19")
                    .city("Vantaa")
                    .state("Uusimaa")
                    .postalCode("01670")
                    .country("Finland")
                    .isDefault(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(addressService.updateAddress(eq(addressId), anyString(), any(AddressRequest.class)))
                    .thenReturn(updatedResponse);

            AddressRequest updateRequest = AddressRequest.builder()
                    .label("Office")
                    .street("Vaisalantie 19")
                    .city("Vantaa")
                    .state("Uusimaa")
                    .postalCode("01670")
                    .country("Finland")
                    .isDefault(false)
                    .build();

            mockMvc.perform(put("/api/v1/addresses/{id}", addressId)
                            .with(userJwt("user@test.com"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.label", is("Office")))
                    .andExpect(jsonPath("$.data.street", is("Vaisalantie 19")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/addresses/{id}")
    class DeleteAddress {

        @Test
        @DisplayName("Should return 200 when address deleted")
        void deleteAddress_Returns200() throws Exception {
            doNothing().when(addressService).deleteAddress(eq(addressId), anyString());

            mockMvc.perform(delete("/api/v1/addresses/{id}", addressId)
                            .with(userJwt("user@test.com")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));
        }
    }
}
