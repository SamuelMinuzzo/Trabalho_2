package chat;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;

public class Client extends JFrame {

    private JTextArea areaChat;
    private JTextField campoMensagem;
    private JButton botaoEnviar;
    private JButton botaoTodos;
    private JLabel labelDestinatario;

    private JList<String> listaUsuarios;
    private DefaultListModel<String> modeloUsuarios;

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private String nome;
    private String destinatario = null;

    private String serverIp; 

    public Client() {
        JTextField campoNome = new JTextField(15);
        JCheckBox chkLocalhost = new JCheckBox("Usar Localhost", true);
        JTextField campoIP = new JTextField("172.30.14.107", 15);

      
        campoIP.setEnabled(false);

        // Ouvinte para ativar/desativar o campo de IP conforme o checkbox
        chkLocalhost.addActionListener(e -> campoIP.setEnabled(!chkLocalhost.isSelected()));

        // Organização dos componentes na caixinha de diálogo
        JPanel painelConfig = new JPanel(new GridLayout(0, 1, 5, 5));
        painelConfig.add(new JLabel("Digite seu nome:"));
        painelConfig.add(campoNome);
        painelConfig.add(chkLocalhost);
        painelConfig.add(new JLabel("IP do Servidor (se não for localhost):"));
        painelConfig.add(campoIP);

        int opcao = JOptionPane.showConfirmDialog(
                null, 
                painelConfig, 
                "Configurações de Conexão", 
                JOptionPane.OK_CANCEL_OPTION, 
                JOptionPane.PLAIN_MESSAGE
        );

        // Se o usuário clicar em OK, define as configurações
        if (opcao == JOptionPane.OK_OPTION) {
            nome = campoNome.getText().trim();
            if (nome.isEmpty()) {
                nome = "Anônimo";
            }

            // Define o IP com base na escolha do usuário
            if (chkLocalhost.isSelected()) {
                serverIp = "localhost";
            } else {
                serverIp = campoIP.getText().trim();
                if (serverIp.isEmpty()) {
                    serverIp = "172.30.14.107";
                }
            }
        } else {
            // Se fechar ou cancelar, encerra a aplicação
            System.exit(0);
        }

        setTitle("Chat - " + nome);
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ── Painel esquerdo: lista de usuários online ──────────────────────────
        modeloUsuarios = new DefaultListModel<>();
        listaUsuarios  = new JList<>(modeloUsuarios);
        listaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel painelEsquerdo = new JPanel(new BorderLayout());
        painelEsquerdo.add(new JLabel("Online", SwingConstants.CENTER), BorderLayout.NORTH);
        painelEsquerdo.add(new JScrollPane(listaUsuarios), BorderLayout.CENTER);

        botaoTodos = new JButton("Todos");
        painelEsquerdo.add(botaoTodos, BorderLayout.SOUTH);
        painelEsquerdo.setPreferredSize(new Dimension(150, 0));

        add(painelEsquerdo, BorderLayout.WEST);

        // ── Centro: área de exibição do chat ──────────────────────────────────
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        add(new JScrollPane(areaChat), BorderLayout.CENTER);

        // ── Painel inferior: campo de digitação e envio ───────────────────────
        JPanel painelInferior = new JPanel(new BorderLayout());
        labelDestinatario = new JLabel("  Para: Todos  ");
        campoMensagem     = new JTextField();
        botaoEnviar       = new JButton("Enviar");

        painelInferior.add(labelDestinatario, BorderLayout.WEST);
        painelInferior.add(campoMensagem,     BorderLayout.CENTER);
        painelInferior.add(botaoEnviar,       BorderLayout.EAST);

        add(painelInferior, BorderLayout.SOUTH);

        conectar();
        eventos();
        setVisible(true);
    }

    private void reconectarComNovoNome() {

        SwingUtilities.invokeLater(() -> {

            JOptionPane.showMessageDialog(
                    this,
                    "Este nome já está em uso.\nEscolha outro nome.",
                    "Nome duplicado",
                    JOptionPane.ERROR_MESSAGE
            );

            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            dispose();

            new Client();
        });
    }

    // ── Conexão ao servidor ────────────────────────────────────────────────────
    private void conectar() {
        try {
           
            //socket = new Socket("localhost", 1234);
            socket = new Socket(serverIp, 1234);

            // ObjectOutputStream ANTES do ObjectInputStream (evita deadlock)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in  = new ObjectInputStream(socket.getInputStream());

            // Envia o nome para o servidor se identificar
            out.writeObject(nome);
            out.flush();

            // Thread dedicada exclusivamente para escutar o servidor
            ReceiverThread receiver = new ReceiverThread(in, areaChat, this);
            receiver.start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar no servidor: " + e.getMessage());
        } finally {
            System.out.println("Cliente: tentativa de conexão concluída.");
        }
    }

    // ── Atualiza lista de usuários na sidebar ──────────────────────────────────
    public void atualizarListaUsuarios(String usuarios) {
        modeloUsuarios.clear();
        for (String u : usuarios.split(",")) {
            if (!u.isEmpty() && !u.equals(nome)) {
                modeloUsuarios.addElement(u);
            }
        }

        // Se o destinatário saiu do chat, volta para broadcast
        if (destinatario != null && modeloUsuarios.indexOf(destinatario) == -1) {
            destinatario = null;
            listaUsuarios.clearSelection();
            labelDestinatario.setText("  Para: Todos  ");
        }
    }

    // ── Registro de eventos de interface ─────────────────────────────────────
    private void eventos() {
        botaoEnviar.addActionListener(e -> enviarMensagem());
        campoMensagem.addActionListener(e -> enviarMensagem());

        // Clique na lista → seleciona destinatário privado
        listaUsuarios.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String selecionado = listaUsuarios.getSelectedValue();
                if (selecionado != null) {
                    destinatario = selecionado;
                    labelDestinatario.setText("  Para: " + destinatario + "  ");
                }
            }
        });

        // Botão "Todos" → volta para broadcast
        botaoTodos.addActionListener(e -> {
            destinatario = null;
            listaUsuarios.clearSelection();
            labelDestinatario.setText("  Para: Todos  ");
        });
    }

    // ── Envio de mensagem (com parsing de comandos) ───────────────────────────
    private void enviarMensagem() {
        try {
            String texto = campoMensagem.getText().trim();
            if (texto.isEmpty()) return;

            // ── Comando: /usuarios ─────────────────────────────────────────────
            // Solicita ao servidor a lista de usuários conectados
            if (texto.equalsIgnoreCase("/usuarios")) {
                Mensagem cmd = new Mensagem(nome, null, "/usuarios");
                out.writeObject(cmd);
                out.flush();
                campoMensagem.setText("");
                return;
            }

            // ── Comando: /privado :<usuário> :<mensagem> ───────────────────────
            // Formato esperado: /privado :João :Olá, tudo bem?
            if (texto.startsWith("/privado ")) {
                try {
                    // Remove o prefixo e separa nos dois-pontos
                    String resto = texto.substring(9).trim(); // retira "/privado "
                    if (!resto.startsWith(":")) {
                        throw new IllegalArgumentException("Formato inválido.");
                    }
                    // Divide no segundo ':' para separar usuário do conteúdo
                    int separador = resto.indexOf(":", 1);
                    if (separador == -1) {
                        throw new IllegalArgumentException("Formato inválido: falta o separador da mensagem.");
                    }
                    String destPrivado  = resto.substring(1, separador).trim();
                    String conteudo     = resto.substring(separador + 1).trim();

                    if (destPrivado.isEmpty() || conteudo.isEmpty()) {
                        throw new IllegalArgumentException("Usuário ou mensagem vazios.");
                    }

                    Mensagem msg = new Mensagem(nome, destPrivado, conteudo);
                    out.writeObject(msg);
                    out.flush();
                    campoMensagem.setText("");

                } catch (ArrayIndexOutOfBoundsException | NullPointerException | IllegalArgumentException ex) {
                    areaChat.append("Uso correto: /privado :<usuário> :<mensagem>\n");
                    System.out.println("Erro: Ocorreu uma exceção ao processar /privado - " + ex.getMessage());
                } finally {
                    System.out.println("Cliente: comando /privado processado.");
                }
                return;
            }

            // ── Mensagem normal (broadcast ou para destinatário selecionado) ───
            Mensagem msg = new Mensagem(nome, destinatario, texto);
            out.writeObject(msg);
            out.flush();
            campoMensagem.setText("");

        } catch (IOException e) {
            System.out.println("Erro: Ocorreu uma exceção ao enviar mensagem - " + e.getMessage());
            areaChat.append("Erro ao enviar mensagem.\n");
        } finally {
            System.out.println("Cliente: enviarMensagem() concluído.");
        }
    }

    // =========================================================================
    // Thread dedicada para receber mensagens do servidor em tempo real
    // =========================================================================
    static class ReceiverThread extends Thread {

        private ObjectInputStream in;
        private JTextArea areaChat;
        private Client client;

        // Cache da última lista de usuários (acesso sincronizado)
        private static String listaUsuarios = "";

        public ReceiverThread(ObjectInputStream in, JTextArea areaChat, Client client) {
            this.in       = in;
            this.areaChat = areaChat;
            this.client   = client;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    // Padrão obrigatório: ObjectInputStream.readObject()
                    Object obj = in.readObject();

                    if (obj instanceof String) {
                        String str = (String) obj;

                        if (str.startsWith("ONLINE:")) {
                            // Atualiza a lista de usuários 
                            synchronized (ReceiverThread.class) {
                                listaUsuarios = str.substring(7);
                            }
                            SwingUtilities.invokeLater(() ->
                                    client.atualizarListaUsuarios(getListaUsuarios())
                            );
                        }

                    } else if (obj instanceof Mensagem) {

                        Mensagem msg = (Mensagem) obj;

                        // Nome duplicado
                        if (msg.getRemetente().equals("SERVIDOR") && msg.getConteudo().contains("Nome já está em uso")) {
                            client.reconectarComNovoNome();
                            break;
                        }

                        SwingUtilities.invokeLater(() ->
                                areaChat.append(msg + "\n")
                        );
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Erro: Ocorreu uma exceção na thread receptora - " + e.getMessage());
                SwingUtilities.invokeLater(() ->
                        areaChat.append("\nDesconectado do servidor.\n")
                );
            } finally {
                System.out.println("Cliente: ReceiverThread encerrada.");
            }
        }

        public static synchronized String getListaUsuarios() {
            return listaUsuarios;
        }
    }

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}