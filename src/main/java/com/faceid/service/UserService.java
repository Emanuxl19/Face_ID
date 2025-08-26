package com.faceid.service;

import com.faceid.dto.RequestDTO.UserRequestDTO;
import com.faceid.dto.ResponseDTO.UserResponseDTO;
import com.faceid.model.User;
import com.faceid.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserResponseDTO(user.getId(), user.getUsername()))
                .collect(Collectors.toList());
    }

    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado com ID: " + id));
        return new UserResponseDTO(user.getId(), user.getUsername());
    }

    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        // ATENCAO: Senhas devem ser encriptadas em um ambiente real!
        User user = new User(userRequestDTO.getUsername(), userRequestDTO.getPassword());
        User savedUser = userRepository.save(user);
        return new UserResponseDTO(savedUser.getId(), savedUser.getUsername());
    }

    public UserResponseDTO updateUser(Long id, UserRequestDTO userRequestDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado com ID: " + id));

        user.setUsername(userRequestDTO.getUsername());
        // ATENCAO: Senhas devem ser encriptadas em um ambiente real!
        user.setPassword(userRequestDTO.getPassword());

        User updatedUser = userRepository.save(user);
        return new UserResponseDTO(updatedUser.getId(), updatedUser.getUsername());
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuario nao encontrado com ID: " + id);
        }
        userRepository.deleteById(id);
    }
}
