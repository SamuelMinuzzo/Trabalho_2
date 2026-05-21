package chat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Mensagem implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private String remetente;
    private String destinatario; // null para broadcast
    private String conteudo;
    private LocalDateTime horario;

    public Mensagem(String remetente, String destinatario, String conteudo) {
        this.remetente = remetente;
        this.destinatario = destinatario;
        this.conteudo = conteudo;
        this.horario = LocalDateTime.now();
    }

    public String getRemetente()    { return remetente;    }
    public String getDestinatario() { return destinatario; }
    public String getConteudo()     { return conteudo;     }
    public LocalDateTime getHorario() { return horario;    }

    @Override
    public String toString() {
        // Formato obrigatório: [Data e Hora] <Remetente> -> <Destinatário>: Mensagem , ver se dá para mudar isso
        String destino = (destinatario == null) ? "Todos" : destinatario;
        return "[" + horario.format(FORMATTER) + "] " + remetente + " -> " + destino + ": " + conteudo;
    }
}