package com.faceid.dto.RequestDTO;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class UserRequestDTO {

    @NotBlank(message = "O nome de usuario nao pode estar em branco")
    @Size(min = 3, max = 50, message = "O nome de usuario deve ter entre 3 e 50 caracteres")
    private String username;

    @NotBlank(message = "A senha nao pode estar em branco")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&^_\\-+=]{8,72}$",
        message = "Senha deve conter ao menos uma letra e um numero"
    )
    private String password;

    @Size(max = 100, message = "Nome completo deve ter no maximo 100 caracteres")
    private String fullName;

    @Pattern(regexp = "^\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}$",
             message = "CPF invalido. Use o formato XXX.XXX.XXX-XX ou somente digitos")
    private String cpf;

    @Email(message = "E-mail invalido")
    @Size(max = 100)
    private String email;

    @Pattern(regexp = "^\\+?[\\d\\s\\-().]{8,20}$",
             message = "Telefone invalido")
    private String phone;

    @Past(message = "Data de nascimento deve ser no passado")
    private LocalDate birthDate;

    public UserRequestDTO() {}

    public UserRequestDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername()  { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
}
