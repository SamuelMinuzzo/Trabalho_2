package chat;

import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    private static CopyOnWriteArrayList<ClientHandler> clientes = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(1234);
            System.out.println("SERVIDOR na porta 1234");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("SERVIDOR: Novo cliente conectado de " + socket.getInetAddress());

                ClientHandler cliente = new ClientHandler(socket);
                clientes.add(cliente);

                // Atualiza a lista de usuários para todos e anuncia a entrada
                cliente.atualizarUsuarios();
                cliente.broadcast(new Mensagem("SERVIDOR", null, cliente.getNome() + " entrou no chat"));

                new Thread(cliente).start();
            }

        } catch (IOException e) {
            System.out.println("Erro: Ocorreu uma exceção no servidor - " + e.getMessage());
        } finally {
            System.out.println("Servidor encerrado.");
        }
    }

    // -------------------------------------------------------------------------
    static class ClientHandler implements Runnable {

        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String nome;

        public ClientHandler(Socket socket) {
            try {
                this.socket = socket;

                // Padrão obrigatório: ObjectOutputStream antes do ObjectInputStream
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in  = new ObjectInputStream(socket.getInputStream());

                nome = (String) in.readObject();
                System.out.println("SERVIDOR: Cliente identificado como '" + nome + "'");

           
            } catch (EOFException | SocketException e) {
                // Captura especificamente as exceções de desconexão (fechar a janela)
                System.out.println("SERVIDOR: O cliente '" + nome + "' fechou o chat.");
            } catch (IOException | ClassNotFoundException e) {
                // Captura outros erros inesperados
                System.out.println("Erro: Ocorreu uma exceção ao ler mensagem de '" + nome + "' - " + e.getMessage());
            } finally {
                System.out.println("SERVIDOR: Handler criado para '" + nome + "'");
            }
            
        }

        public String getNome() { return nome; }

        @Override
        public void run() {
            while (socket.isConnected()) {
                try {
                    Object obj = in.readObject();

                    if (obj instanceof Mensagem) {
                        Mensagem msg = (Mensagem) obj;
                        System.out.println(msg);

                        // Suporte ao comando /usuarios: cliente pede a lista de usuários
                        if (msg.getConteudo().trim().equals("/usuarios")) {
                            enviarListaUsuarios();

                        } else if (msg.getDestinatario() == null) {
                            // Broadcast normal
                            broadcast(msg);

                        } else {
                            // Mensagem privada
                            enviarPrivado(msg);
                        }
                    }

                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("Erro: Ocorreu uma exceção ao ler mensagem de '" + nome + "' - " + e.getMessage());
                    fecharConexao();
                    break;
                } finally {
                    
                }
            }
        }

        // Envia a lista de usuários apenas para o cliente que pediu (/usuarios)
        private void enviarListaUsuarios() {
            StringBuilder sb = new StringBuilder("Usuários online: ");
            for (int i = 0; i < clientes.size(); i++) {
                sb.append(clientes.get(i).nome);
                if (i < clientes.size() - 1) sb.append(", ");
            }
            Mensagem resposta = new Mensagem("SERVIDOR", nome, sb.toString());
            enviar(this, resposta);
        }

        // Broadcast: envia para todos os clientes conectados
        public void broadcast(Mensagem msg) {
            for (ClientHandler c : clientes) {
                enviar(c, msg);
            }
        }

        // Mensagem privada: entrega ao destinatário e confirma ao remetente
        private void enviarPrivado(Mensagem msg) {
            boolean encontrado = false;
            for (ClientHandler c : clientes) {
                if (c.nome.equals(msg.getDestinatario()) || c.nome.equals(msg.getRemetente())) {
                    enviar(c, msg);
                    encontrado = true;
                }
            }
            // Se o destinatário não foi encontrado, notifica o remetente
            if (!encontrado) {
                Mensagem aviso = new Mensagem("SERVIDOR", nome,
                        "Usuário '" + msg.getDestinatario() + "' não encontrado.");
                enviar(this, aviso);
            }
        }

        // Envio genérico de qualquer objeto serializable
        private void enviar(ClientHandler cliente, Object obj) {
            try {
                cliente.out.writeObject(obj);
                cliente.out.flush();
            } catch (IOException e) {
                System.out.println("Erro: Ocorreu uma exceção ao enviar para '" + cliente.nome + "' - " + e.getMessage());
            } finally {
                System.out.println("SERVIDOR: Mensagem processada para '" + cliente.nome + "'");
            }
        }

        // Atualiza a lista ONLINE: para todos os clientes
        public void atualizarUsuarios() {
            StringBuilder sb = new StringBuilder();
            for (ClientHandler c : clientes) {
                sb.append(c.nome).append(",");
            }
            String lista = "ONLINE:" + sb;

            for (ClientHandler c : clientes) {
                enviar(c, lista);
            }
        }

        // Fecha a conexão e notifica os demais
        private void fecharConexao() {
            clientes.remove(this);
            atualizarUsuarios();
            broadcast(new Mensagem("SERVIDOR", null, nome + " saiu do chat"));
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.out.println("Erro: Ocorreu uma exceção ao fechar socket de '" + nome + "' - " + e.getMessage());
            } finally {
                System.out.println("SERVIDOR: Conexão de '" + nome + "' encerrada.");
            }
        }
    }
}