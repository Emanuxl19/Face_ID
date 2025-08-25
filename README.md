# FaceID - Sistema de Autenticação com Reconhecimento Facial

##  Descrição
**Face_ID** é uma aplicação web simplificada para cadastro e reconhecimento facial. O sistema permite que usuários sejam cadastrados com dados pessoais (como CPF, nome, telefone, idade e data de nascimento) e 3 a 4 imagens faciais. A partir desses dados armazenados, é possível realizar consultas por informações cadastrais ou por detecção facial, retornando rapidamente os dados do usuário correspondente..

---
## Objetivo
O **Face_ID** foi desenvolvido como um serviço terceirizado para autenticação e gerenciamento de identidade e dados cadastrais baseada em reconhecimento facial. Ele possibilita:
- Cadastrar usuários com informações pessoais e imagens faciais.
- Armazenar e gerenciar essas informações de forma organizada.
- Consultar usuários via dados cadastrais ou por envio de imagem para detecção.


## 📂 Estrutura do Projeto
- **controller**: Controladores REST para endpoints
- **service**: Lógica de negócio
- **repository**: Acesso a dados (JPA/Hibernate)
- **model**: Entidades do banco (User, Role)
- **dto**: Objetos de transferência de dados
- **mapper**: Conversão entre DTOs e entidades
- **security**: Configurações de segurança, JWT e autenticação

---

## Funcionalidades
- Cadastro de usuário com dados pessoais.
- Upload de 3 a 4 imagens faciais por usuário.
- Armazenamento das imagens e dados em banco de dados.
- Busca de usuário por CPF ou nome.
- Reconhecimento facial a partir de uma nova imagem.
- Retorno dos dados do usuário correspondente.

---

# Requisitos - Face_ID

## 📌 Requisitos Funcionais

- **RF01 — Cadastro de Usuário**  
  O sistema deve permitir o cadastro de usuários com dados pessoais, incluindo: **CPF, nome completo, data de nascimento, idade, telefone, e-mail e senha**.  

- **RF02 — Cadastro de Imagens Faciais**  
  O sistema deve permitir o envio de no mínimo **3 a 4 imagens faciais** do usuário no momento do cadastro, para criação de um perfil biométrico confiável.  

- **RF03 — Armazenamento de Dados**  
  O sistema deve armazenar em **banco de dados seguro** todas as informações cadastrais e imagens faciais associadas ao usuário.  

- **RF04 — Consulta por Identificador**  
  O sistema deve permitir a consulta dos dados do usuário a partir de identificadores textuais (**CPF, telefone, e-mail, etc.**).  

- **RF05 — Reconhecimento Facial Aproximado**  
  O sistema deve permitir a identificação de um usuário a partir de uma nova imagem facial.  
  - A detecção será feita de forma **aproximada**, através da extração de **características faciais (embeddings/vetores)** das imagens cadastradas.  
  - O sistema deve comparar a imagem enviada com os perfis armazenados, retornando o usuário **com maior similaridade**, desde que a correspondência seja maior que um **limiar de confiança (ex: 80%)**.  

- **RF06 — Retorno das Informações**  
  Quando um usuário for identificado (via consulta textual ou reconhecimento facial), o sistema deve retornar as informações cadastrais associadas.  


## Requisitos Não Funcionais
- RNF01: O sistema deve responder a uma requisição em até 2 segundos.
- RNF02: As imagens devem ser armazenadas em formato .jpg ou .png com limite de até 1 MB por arquivo.
- RNF03: O sistema deve ser uma aplicação web acessível via navegador e API REST.
- RNF04: O banco de dados deve suportar pelo menos 1000 usuários cadastrados inicialmente.
- RNF05: O sistema deve utilizar arquitetura em camadas (Controller, Service, Repository, Model).

---

## Arquitetura
O projeto segue uma arquitetura em camadas:
- Controller → recebe requisições HTTP.
- Service → lógica de negócio (comparação facial).
- Repository → acesso ao banco de dados.
- Model → entidades de dados.

---

## Tecnologias
- Java 17
- Spring Boot
- OpenCV (para reconhecimento facial)
- Banco H2 (desenvolvimento) / SQL Server (produção)

---

## Como executar
1. Clone o repositório:
   ```bash
   git clone https://github.com/Emanuxl19/Face_ID.git
   cd Face_ID
2. Compile e rode:
   mvn spring-boot:run
3. Acesse a aplicação em:
   http://localhost:8080
