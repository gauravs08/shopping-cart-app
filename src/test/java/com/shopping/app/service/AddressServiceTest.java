package com.shopping.app.service;

import com.shopping.app.dto.request.AddressRequest;
import com.shopping.app.dto.response.AddressResponse;
import com.shopping.app.entity.Address;
import com.shopping.app.entity.User;
import com.shopping.app.exception.ResourceNotFoundException;
import com.shopping.app.repository.AddressRepository;
import com.shopping.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressService Tests")
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressService addressService;

    private User testUser;
    private Address testAddress;
    private UUID userId;
    private UUID addressId;
    private String userEmail;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        userEmail = "user@test.com";

        testUser = User.builder()
                .id(userId)
                .email(userEmail)
                .firstName("Test")
                .lastName("User")
                .password("encoded-password")
                .build();

        testAddress = Address.builder()
                .id(addressId)
                .user(testUser)
                .label("Home")
                .street("123 Main Street")
                .city("Helsinki")
                .state("Uusimaa")
                .postalCode("00100")
                .country("Finland")
                .isDefault(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Get Addresses")
    class GetAddresses {

        @Test
        @DisplayName("Should return list of addresses for user")
        void getAddresses_ReturnsList() {
            // Arrange
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(addressRepository.findByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of(testAddress));

            // Act
            List<AddressResponse> responses = addressService.getAddresses(userEmail);

            // Assert
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getStreet()).isEqualTo("123 Main Street");
            assertThat(responses.get(0).getCity()).isEqualTo("Helsinki");
            verify(addressRepository).findByUserIdOrderByCreatedAtDesc(userId);
        }
    }

    @Nested
    @DisplayName("Add Address")
    class AddAddress {

        @Test
        @DisplayName("Should save address successfully")
        void addAddress_Success() {
            // Arrange
            AddressRequest request = AddressRequest.builder()
                    .label("Home")
                    .street("123 Main Street")
                    .city("Helsinki")
                    .state("Uusimaa")
                    .postalCode("00100")
                    .country("Finland")
                    .isDefault(false)
                    .build();

            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(addressRepository.save(any(Address.class))).thenReturn(testAddress);

            // Act
            AddressResponse response = addressService.addAddress(userEmail, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStreet()).isEqualTo("123 Main Street");
            assertThat(response.getCity()).isEqualTo("Helsinki");
            verify(addressRepository).save(any(Address.class));
        }

        @Test
        @DisplayName("Should clear existing default when adding default address")
        void addAddress_DefaultAddress_ClearsExistingDefault() {
            // Arrange
            Address defaultAddress = Address.builder()
                    .id(addressId)
                    .user(testUser)
                    .label("Home")
                    .street("123 Main Street")
                    .city("Helsinki")
                    .state("Uusimaa")
                    .postalCode("00100")
                    .country("Finland")
                    .isDefault(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            AddressRequest request = AddressRequest.builder()
                    .label("Home")
                    .street("123 Main Street")
                    .city("Helsinki")
                    .state("Uusimaa")
                    .postalCode("00100")
                    .country("Finland")
                    .isDefault(true)
                    .build();

            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(addressRepository.save(any(Address.class))).thenReturn(defaultAddress);

            // Act
            AddressResponse response = addressService.addAddress(userEmail, request);

            // Assert
            assertThat(response).isNotNull();
            verify(addressRepository).clearDefaultForUser(userId);
            verify(addressRepository).save(any(Address.class));
        }
    }

    @Nested
    @DisplayName("Update Address")
    class UpdateAddress {

        @Test
        @DisplayName("Should update address successfully")
        void updateAddress_Success() {
            // Arrange
            AddressRequest request = AddressRequest.builder()
                    .label("Work")
                    .street("456 Business Ave")
                    .city("Espoo")
                    .state("Uusimaa")
                    .postalCode("02100")
                    .country("Finland")
                    .isDefault(false)
                    .build();

            Address updatedAddress = Address.builder()
                    .id(addressId)
                    .user(testUser)
                    .label("Work")
                    .street("456 Business Ave")
                    .city("Espoo")
                    .state("Uusimaa")
                    .postalCode("02100")
                    .country("Finland")
                    .isDefault(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(addressRepository.findByIdAndUserId(addressId, userId))
                    .thenReturn(Optional.of(testAddress));
            when(addressRepository.save(any(Address.class))).thenReturn(updatedAddress);

            // Act
            AddressResponse response = addressService.updateAddress(addressId, userEmail, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStreet()).isEqualTo("456 Business Ave");
            assertThat(response.getCity()).isEqualTo("Espoo");
            verify(addressRepository).save(any(Address.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when address not found")
        void updateAddress_NotFound_ThrowsException() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            AddressRequest request = AddressRequest.builder()
                    .street("456 Business Ave")
                    .city("Espoo")
                    .postalCode("02100")
                    .build();

            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(addressRepository.findByIdAndUserId(nonExistentId, userId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> addressService.updateAddress(nonExistentId, userEmail, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Address");
        }
    }

    @Nested
    @DisplayName("Delete Address")
    class DeleteAddress {

        @Test
        @DisplayName("Should delete address successfully")
        void deleteAddress_Success() {
            // Arrange
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(addressRepository.findByIdAndUserId(addressId, userId))
                    .thenReturn(Optional.of(testAddress));

            // Act
            addressService.deleteAddress(addressId, userEmail);

            // Assert
            verify(addressRepository).delete(testAddress);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when address not found")
        void deleteAddress_NotFound_ThrowsException() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(addressRepository.findByIdAndUserId(nonExistentId, userId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> addressService.deleteAddress(nonExistentId, userEmail))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Address");
        }
    }
}
