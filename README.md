# Face\_ID - Sistema de Autenticação com Reconhecimento Facial 🚀

## 📄 Descrição

**Face\_ID** é uma plataforma de API REST para cadastro e reconhecimento facial. O sistema permite que utilizadores sejam registados com dados pessoais e um conjunto de imagens do seu rosto. A partir destes dados, é possível realizar consultas por informações cadastrais ou por deteção facial, retornando rapidamente os dados do utilizador correspondente.

## 🎯 Objetivo

O **Face\_ID** foi desenvolvido para funcionar como um serviço de identidade, oferecendo uma solução de autenticação e gestão de dados baseada em biometria facial. Ele possibilita:

  * Cadastrar utilizadores com informações pessoais e imagens faciais.
  * Armazenar e gerir perfis biométricos de forma segura.
  * Consultar utilizadores via dados cadastrais ou por envio de imagem para verificação.

## 🛠️ Tecnologias Utilizadas

  * **Backend:** Java 17, Spring Boot
  * **Persistência de Dados:** Spring Data JPA, Hibernate
  * **Banco de Dados:** H2 (para desenvolvimento) e SQL Server (para produção)
  * **Visão Computacional:** OpenCV (via Bytedeco)
  * **Documentação da API:** Springdoc OpenAPI (Swagger UI)
  * **Build:** Apache Maven

## 🏗️ Arquitetura

O projeto segue uma arquitetura em camadas bem definida para garantir a separação de responsabilidades e a manutenibilidade do código:

```
Requisição HTTP
      ↓
[ 👤 Controller ]  (Endpoints REST)
      ↓
[ ⚙️ Service ]     (Lógica de Negócio, ex: comparação facial)
      ↓
[ 🗄️ Repository ]  (Acesso a Dados com Spring Data JPA)
      ↓
[ 💾 Database ]     (SQL Server / H2)
```

## ✨ Funcionalidades

  * ✅ Cadastro completo de utilizador com dados pessoais.
  * ✅ Upload de múltiplas imagens faciais por utilizador.
  * ✅ Armazenamento seguro de imagens e metadados em banco de dados.
  * ✅ Busca de utilizador por identificadores (CPF, nome, etc.).
  * ✅ Reconhecimento facial a partir do envio de uma nova imagem (via `multipart/form-data` ou `Base64`).
  * ✅ Retorno dos dados completos do utilizador correspondente após a identificação.

-----

## 🚀 Como Executar o Projeto

Siga os passos abaixo para configurar e executar a aplicação localmente.

### 1\. Pré-requisitos

  * **Java 17** ou superior
  * **Apache Maven**
  * **Git**
  * **SQL Server** (necessário para o perfil de produção)

### 2\. Clonar o Repositório

```bash
git clone https://github.com/Emanuxl19/Face_ID.git
cd Face_ID
```

### 3\. Configuração do Banco de Dados

  * **Ambiente de Desenvolvimento (`dev`):** Por padrão, a aplicação utiliza um banco de dados em memória **H2**, que não requer configuração adicional. Ele será criado e destruído automaticamente.

  * **Ambiente de Produção (`prod`):**

    1.  Certifique-se de que tem uma instância do SQL Server a correr.
    2.  Crie um banco de dados para a aplicação, por exemplo:
        ```sql
        CREATE DATABASE faceid_prod;
        ```
    3.  Crie um ficheiro `.env` na raiz do projeto para armazenar as suas credenciais de produção. Utilize o ficheiro `.env.example` (se existir) como modelo ou crie um com o seguinte formato:
        ```env
        DB_URL=jdbc:sqlserver://localhost:1433;databaseName=faceid_prod;encrypt=true;trustServerCertificate=true
        DB_USERNAME=seu_usuario_prod
        DB_PASSWORD=sua_senha_prod
        ```

### 4\. Executar a Aplicação

Use o Maven para compilar e iniciar o servidor Spring Boot.

```bash
mvn spring-boot:run
```

A aplicação iniciará, por padrão, no perfil `dev`.

### 5\. Aceder à Aplicação

  * **API:** A aplicação estará disponível em `http://localhost:8080`.
  * **Documentação Swagger UI:** Para testar os endpoints de forma interativa, aceda a:
      * `http://localhost:8080/swagger-ui/`

-----

## 📋 Requisitos do Projeto

\<details\>
\<summary\>\<strong\>Clique para ver os Requisitos Funcionais e Não Funcionais\</strong\>\</summary\>

### 📌 Requisitos Funcionais (RFs)

  * **RF01 — Cadastro de Usuário:** O sistema deve permitir o cadastro de utilizadores com: `CPF`, `nome completo`, `data de nascimento`, `idade`, `telefone`, `e-mail` e `senha`.
  * **RF02 — Cadastro de Imagens Faciais:** O sistema deve permitir o envio de 3 a 4 imagens faciais do utilizador para a criação de um perfil biométrico.
  * **RF03 — Armazenamento de Dados:** As informações cadastrais e as características faciais extraídas devem ser armazenadas de forma segura no banco de dados.
  * **RF04 — Consulta por Identificador:** O sistema deve permitir a consulta de utilizadores por `CPF`, `telefone` ou `e-mail`.
  * **RF05 — Reconhecimento Facial Aproximado:** O sistema deve identificar um utilizador a partir de uma nova imagem. A deteção é feita através da extração de vetores de características faciais (embeddings) e da comparação por similaridade, utilizando um limiar de confiança (ex: 80%).
  * **RF06 — Retorno das Informações:** Ao identificar um utilizador, o sistema deve retornar as suas informações cadastrais.

### ⚙️ Requisitos Não Funcionais (RNFs)

  * **RNF01:** O tempo de resposta para uma requisição de verificação deve ser de até 2 segundos.
  * **RNF02:** As imagens para upload devem ser no formato `.jpg` ou `.png`, com um limite de 1 MB por ficheiro.
  * **RNF03:** A aplicação deve ser acessível via API REST.
  * **RNF04:** O banco de dados deve suportar, no mínimo, 1000 utilizadores.
  * **RNF05:** O sistema deve seguir uma arquitetura em camadas (Controller, Service, Repository).

\</details\>
