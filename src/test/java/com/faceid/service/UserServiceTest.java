package com.faceid.service;

import com.faceid.dto.RequestDTO.UserRequestDTO;
import com.faceid.dto.ResponseDTO.UserResponseDTO;
import com.faceid.exception.ConflictException;
import com.faceid.exception.ResourceNotFoundException;
import com.faceid.model.User;
import com.faceid.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** Simula um usuário autenticado via JWT no SecurityContext. */
    private static void authenticateAs(String username) {
        var auth = new UsernamePasswordAuthenticationToken(username, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createUserShouldEncodePasswordAndTrimUsername() {
        when(userRepository.existsByUsername("ana")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponseDTO response = userService.createUser(new UserRequestDTO(" ana ", "senhaForte123"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getUsername()).isEqualTo("ana");
        assertThat(savedUser.getPassword()).isNotEqualTo("senhaForte123");
        assertThat(passwordEncoder.matches("senhaForte123", savedUser.getPassword())).isTrue();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("ana");
        assertThat(response.faceRegistered()).isFalse();
    }

    @Test
    void createUserShouldRejectDuplicateUsername() {
        when(userRepository.existsByUsername("ana")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(new UserRequestDTO("ana", "senhaForte123")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ja existe um usuario com esse nome.");
    }

    @Test
    void getUserByIdShouldExposeFaceRegistrationStatus() {
        authenticateAs("ana");
        User user = new User("ana", "hash");
        user.setId(7L);
        user.setFaceFeatures(new byte[]{1, 2, 3});

        when(userRepository.findById(7L)).thenReturn(java.util.Optional.of(user));

        UserResponseDTO response = userService.getUserById(7L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.username()).isEqualTo("ana");
        assertThat(response.faceRegistered()).isTrue();
    }

    @Test
    void updateUserShouldRejectAnotherUsersUsername() {
        authenticateAs("ana");
        User currentUser = new User("ana", "hash");
        currentUser.setId(1L);

        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(currentUser));
        when(userRepository.existsByUsername("bia")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, new UserRequestDTO("bia", "senhaForte123")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ja existe um usuario com esse nome.");
    }

    @Test
    void deleteUserShouldFailWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuario nao encontrado com ID: 99");
    }
}
