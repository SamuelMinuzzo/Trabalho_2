# Chat TCP em Java

Aplicação de chat em tempo real baseada em comunicação TCP, desenvolvida em Java com interface gráfica Swing. Permite que múltiplos usuários troquem mensagens públicas e privadas por meio de um servidor centralizado.

---

## Estrutura do Projeto

```
chat/
├── Server.java      # Servidor TCP — aceita conexões e gerencia clientes
├── Client.java      # Cliente com interface gráfica (Swing)
└── Mensagem.java    # Classe serializável que representa uma mensagem
```

### `Mensagem.java`
Classe serializável que encapsula os dados de cada mensagem: remetente, destinatário, conteúdo e horário. Quando `destinatario == null`, a mensagem é tratada como broadcast (enviada a todos).

### `Server.java`
Servidor TCP que escuta na porta **1234**. Para cada cliente conectado, cria um `ClientHandler` em thread separada. Responsável por:
- Broadcast de mensagens públicas
- Roteamento de mensagens privadas
- Manutenção e atualização da lista de usuários online

### `Client.java`
Interface gráfica (Swing) que se conecta ao servidor. Possui uma thread dedicada (`ReceiverThread`) para receber mensagens em tempo real sem bloquear a interface. Suporta envio de mensagens públicas, privadas por comando e privadas por clique na lista de usuários.

---

## Requisitos

- **Java 11** ou superior
- Ambos os processos (servidor e cliente) devem ter acesso à mesma rede

---

## Como Executar

### 1. Compilar

O projeto usa o pacote `chat`, então a compilação deve ser feita a partir do diretório **pai** da pasta `chat/`.

A estrutura de pastas esperada é:
```
Trabalho2/
└── chat/
    ├── Server.java
    ├── Client.java
    └── Mensagem.java
```

Dentro de `Trabalho2/`, execute:

```bash
javac chat/*.java
```

### 2. Iniciar o servidor

```bash
java chat.Server
```

O servidor exibirá `SERVIDOR na porta 1234` e aguardará conexões.

### 3. Iniciar o(s) cliente(s)

```bash
java chat.Client
```

> **Atenção:** por padrão, o cliente tenta se conectar ao IP `192.168.1.50`. Para rodar localmente (mesmo computador), altere em `Client.java`:
> ```java
> // de:
> socket = new Socket("192.168.1.50", 1234);
> // para:
> socket = new Socket("localhost", 1234);
> ```

Abra quantas instâncias do cliente quiser — cada uma representa um usuário diferente.

---

## Funcionalidades

### Mensagem pública (broadcast)
Digite o texto no campo inferior e pressione **Enviar** ou **Enter**. A mensagem é entregue a todos os usuários conectados.

### Mensagem privada — via interface
Clique no nome de um usuário na lista lateral **Online**. O campo "Para:" será atualizado. As mensagens seguintes serão enviadas apenas para esse usuário. Para voltar ao broadcast, clique em **Todos**.

### Mensagem privada — via comando
```
/privado :<usuário> :<mensagem>
```
Exemplo:
```
/privado :Maria :Olá, tudo bem?
```

### Lista de usuários online
```
/usuarios
```
O servidor responde com a lista de todos os clientes conectados no momento.

---

## Formato das Mensagens

Todas as mensagens exibidas seguem o padrão:

```
[dd/MM/yyyy HH:mm:ss] <Remetente> -> <Destinatário>: Conteúdo
```

Exemplo:
```
[21/05/2026 14:32:10] João -> Todos: Olá pessoal!
[21/05/2026 14:32:45] João -> Maria: Mensagem privada aqui
```

---

## Tratamento de Erros

- Exceções de I/O na leitura/escrita de sockets são capturadas e exibidas no console sem derrubar o servidor.
- Quando um cliente se desconecta, os demais são notificados e a lista online é atualizada automaticamente.
- Se o destinatário de uma mensagem privada não for encontrado, o remetente recebe um aviso do servidor.
- Comandos com formato inválido exibem uma mensagem de uso correto na área de chat.

---

## Observações Técnicas

- O `ObjectOutputStream` é sempre criado **antes** do `ObjectInputStream` em ambos os lados da conexão, evitando deadlock na inicialização.
- A lista de clientes usa `CopyOnWriteArrayList`, garantindo segurança em acessos concorrentes entre threads.
- Atualizações na interface gráfica são sempre feitas via `SwingUtilities.invokeLater`, respeitando o Event Dispatch Thread do Swing.