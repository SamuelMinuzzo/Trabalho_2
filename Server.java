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
				System.out.println("SERVIDOR Novo cliente conectado");

				ClientHandler cliente = new ClientHandler(socket);
				clientes.add(cliente);

				cliente.atualizarUsuarios();
				cliente.broadcast(new Mensagem("SERVIDOR", null, cliente.getNome() + " entrou no chat"));

				new Thread(cliente).start();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static class ClientHandler implements Runnable {

		private Socket socket;
		private ObjectInputStream in;
		private ObjectOutputStream out;
		private String nome;

		public ClientHandler(Socket socket) {
			try {
				this.socket = socket;

				out = new ObjectOutputStream(socket.getOutputStream());
				out.flush();
				in = new ObjectInputStream(socket.getInputStream());

				nome = (String) in.readObject();

			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		public String getNome() {
			return nome;
		}

		@Override
		public void run() {
			while (socket.isConnected()) {
				try {
					Object obj = in.readObject();

					if (obj instanceof Mensagem) {
						Mensagem msg = (Mensagem) obj;
						System.out.println(msg);

						if (msg.getDestinatario() == null) {
							broadcast(msg);
						} else {
							enviarPrivado(msg);
						}
					}

				} catch (IOException | ClassNotFoundException e) {
					fecharConexao();
					break;
				}
			}
		}

		public void broadcast(Mensagem msg) {
			for (ClientHandler c : clientes) {
				enviar(c, msg);
			}
		}

		private void enviarPrivado(Mensagem msg) {
			for (ClientHandler c : clientes) {
				if (c.nome.equals(msg.getDestinatario()) || c.nome.equals(msg.getRemetente())) {
					enviar(c, msg);
				}
			}
		}

		private void enviar(ClientHandler cliente, Object obj) {
			try {
				cliente.out.writeObject(obj);
				cliente.out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

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

		private void fecharConexao() {
			clientes.remove(this);
			atualizarUsuarios();
			broadcast(new Mensagem("SERVIDOR", null, nome + " saiu do chat"));
			try {
				if (socket != null) socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}