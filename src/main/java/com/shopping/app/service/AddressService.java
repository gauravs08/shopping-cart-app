package com.shopping.app.service;

import com.shopping.app.dto.request.AddressRequest;
import com.shopping.app.dto.response.AddressResponse;
import com.shopping.app.entity.Address;
import com.shopping.app.entity.User;
import com.shopping.app.exception.ResourceNotFoundException;
import com.shopping.app.repository.AddressRepository;
import com.shopping.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(String userEmail) {
        User user = findUserByEmail(userEmail);
        return addressRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public AddressResponse addAddress(String userEmail, AddressRequest request) {
        User user = findUserByEmail(userEmail);

        if (request.isDefault()) {
            addressRepository.clearDefaultForUser(user.getId());
        }

        Address address = Address.builder()
                .user(user)
                .label(request.getLabel())
                .street(request.getStreet())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry() != null ? request.getCountry() : "Finland")
                .isDefault(request.isDefault())
                .build();

        address = addressRepository.save(address);
        return mapToResponse(address);
    }

    @Transactional
    public AddressResponse updateAddress(UUID addressId, String userEmail, AddressRequest request) {
        User user = findUserByEmail(userEmail);
        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        if (request.isDefault() && !address.isDefault()) {
            addressRepository.clearDefaultForUser(user.getId());
        }

        address.setLabel(request.getLabel());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        if (request.getCountry() != null) {
            address.setCountry(request.getCountry());
        }
        address.setDefault(request.isDefault());

        address = addressRepository.save(address);
        return mapToResponse(address);
    }

    @Transactional
    public void deleteAddress(UUID addressId, String userEmail) {
        User user = findUserByEmail(userEmail);
        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
        addressRepository.delete(address);
    }

    @Transactional(readOnly = true)
    public Optional<AddressResponse> getDefaultAddress(String userEmail) {
        User user = findUserByEmail(userEmail);
        return addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .map(this::mapToResponse);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private AddressResponse mapToResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .label(address.getLabel())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .isDefault(address.isDefault())
                .createdAt(address.getCreatedAt())
                .build();
    }
}
