# FaceID - Sistema de Autenticação com Reconhecimento Facial

## 🚀 Descrição
O **FaceID** é um projeto em **Java Spring Boot** que implementa um sistema de autenticação de usuários utilizando **login tradicional (usuário/senha)** com **JWT** e suporte para **autenticação via reconhecimento facial (FaceID)**.  
O projeto segue boas práticas de **Clean Code**, arquitetura em camadas e modularização.

---

## 📂 Estrutura do Projeto
- **controller**: Controladores REST para endpoints
- **service**: Lógica de negócio
- **repository**: Acesso a dados (JPA/Hibernate)
- **model**: Entidades do banco (User, Role)
- **dto**: Objetos de transferência de dados
- **mapper**: Conversão entre DTOs e entidades
- **security**: Configurações de segurança, JWT e autenticação

---

## 🛠 Funcionalidades
- Cadastro e login de usuários com **usuário e senha**  
- Autenticação via **JWT**  
- Estrutura inicial para autenticação via **FaceID**  
- Boas práticas de modularização e Clean Code

---

## ⚙️ Requisitos
- Java 17+
- Maven ou Gradle
- Spring Boot 3+
- Banco de dados PostgreSQL ou outro compatível
- Postman ou ferramenta similar para testes de API

---

## ▶️ Como rodar o projeto
1. Clone o repositório:
```bash
git clone git@github.com:Emanuxl19/Face_ID.git
