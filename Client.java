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

    public Client() {
        nome = JOptionPane.showInputDialog("Digite seu nome:");

        setTitle("Chat - " + nome);
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        modeloUsuarios = new DefaultListModel<>();
        listaUsuarios = new JList<>(modeloUsuarios);
        listaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel painelEsquerdo = new JPanel(new BorderLayout());
        painelEsquerdo.add(new JLabel("Online", SwingConstants.CENTER), BorderLayout.NORTH);
        painelEsquerdo.add(new JScrollPane(listaUsuarios), BorderLayout.CENTER);

        botaoTodos = new JButton("Todos");
        painelEsquerdo.add(botaoTodos, BorderLayout.SOUTH);
        painelEsquerdo.setPreferredSize(new Dimension(150, 0));

        add(painelEsquerdo, BorderLayout.WEST);

        areaChat = new JTextArea();
        areaChat.setEditable(false);
        add(new JScrollPane(areaChat), BorderLayout.CENTER);


        JPanel painelInferior = new JPanel(new BorderLayout());
        labelDestinatario = new JLabel("  Para: Todos  ");
        campoMensagem = new JTextField();
        botaoEnviar = new JButton("Enviar");

        painelInferior.add(labelDestinatario, BorderLayout.WEST);
        painelInferior.add(campoMensagem, BorderLayout.CENTER);
        painelInferior.add(botaoEnviar, BorderLayout.EAST);

        add(painelInferior, BorderLayout.SOUTH);

        conectar();
        eventos();
        setVisible(true);
    }

    private void conectar() {
        try {
            socket = new Socket("localhost", 1234);

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(nome);
            out.flush();

            ReceiverThread receiver = new ReceiverThread(in, areaChat, this);
            receiver.start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar no servidor");
        }
    }

    public void atualizarListaUsuarios(String usuarios) {
        modeloUsuarios.clear();
        for (String u : usuarios.split(",")) {
            if (!u.isEmpty() && !u.equals(nome)) {
                modeloUsuarios.addElement(u);
            }
        }

        if (destinatario != null && modeloUsuarios.indexOf(destinatario) == -1) {
            destinatario = null;
            listaUsuarios.clearSelection();
            labelDestinatario.setText("  Para: Todos  ");
        }
    }

    private void eventos() {
        botaoEnviar.addActionListener(e -> enviarMensagem());
        campoMensagem.addActionListener(e -> enviarMensagem());

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

        botaoTodos.addActionListener(e -> {
            destinatario = null;
            listaUsuarios.clearSelection();
            labelDestinatario.setText("  Para: Todos  ");
        });
    }

    private void enviarMensagem() {
        try {
            String texto = campoMensagem.getText().trim();
            if (!texto.isEmpty()) {
                Mensagem msg = new Mensagem(nome, destinatario, texto);
                out.writeObject(msg);
                out.flush();
                campoMensagem.setText("");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ReceiverThread extends Thread {

        private ObjectInputStream in;
        private JTextArea areaChat;
        private Client client;

        private static String listaUsuarios = "";

        public ReceiverThread(ObjectInputStream in, JTextArea areaChat, Client client) {
            this.in = in;
            this.areaChat = areaChat;
            this.client = client;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Object obj = in.readObject();

                    if (obj instanceof String) {
                        String str = (String) obj;
                        if (str.startsWith("ONLINE:")) {

                            synchronized (ReceiverThread.class) {
                                listaUsuarios = str.substring(7);
                            }

                            SwingUtilities.invokeLater(() ->
                                    client.atualizarListaUsuarios(getListaUsuarios())
                            );
                        }

                    } else if (obj instanceof Mensagem) {
                        Mensagem msg = (Mensagem) obj;
                        SwingUtilities.invokeLater(() ->
                                areaChat.append(msg + "\n")
                        );
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                SwingUtilities.invokeLater(() ->
                        areaChat.append("\nDesconectado do servidor.\n")
                );
            }
        }

        public static synchronized String getListaUsuarios() {
            return listaUsuarios;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}