package com.faceid.dto.RequestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRequestDTO {

    @NotBlank(message = "O nome de usuario nao pode estar em branco")
    @Size(min = 3, max = 50, message = "O nome de usuario deve ter entre 3 e 50 caracteres")
    private String username;

    @NotBlank(message = "A senha nao pode estar em branco")
    @Size(min = 6, message = "A senha deve ter no minimo 6 caracteres")
    private String password;


    public UserRequestDTO() {}


    public UserRequestDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
