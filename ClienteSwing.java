package gui;

import chat.Mensagem;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class ClienteSwing extends JFrame {
    private JTextArea areaTexto;
    private JTextField campoEntrada;


    public ClienteSwing() {
        setTitle("Chat TCP - Cliente");
        setSize(500, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        areaTexto = new JTextArea();
        areaTexto.setEditable(false);
        add(new JScrollPane(areaTexto), BorderLayout.CENTER);

        campoEntrada = new JTextField();
        add(campoEntrada, BorderLayout.SOUTH);

        //Atribui um a ação ao campo, por exemplo toda vez digitar entra nessa rotina.
        campoEntrada.addActionListener(e -> {
        	//Lê o texto do campo de entrada
            String texto = campoEntrada.getText();
            if (!texto.isBlank()) {
               
            	// adiciona texto na area de texto
               areaTexto.append(texto +"\n");
                
               //adiciona texto no campo de Entrada
               campoEntrada.setText("");
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClienteSwing cliente = new ClienteSwing();
            cliente.setVisible(true);
        });
    }
}