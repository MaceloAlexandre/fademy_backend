import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Cliente {
    static void main(String[] args) {
        String servidor = args.length > 0 ? args[0] : "localhost";
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : 5000;

        try (
            Socket socket = new Socket(servidor, porta);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter saida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            Scanner teclado = new Scanner(System.in)) {

            Thread receptor = new Thread(() -> {
                try {
                    String mensagem;

                    while ((mensagem = entrada.readLine()) != null) {
                        System.out.println(mensagem);
                    }

                    System.out.println("Conexão encerrada.");
                    System.exit(0);
                } catch (IOException e) {
                    System.out.println("Conexão encerrada.");
                }
            });

            receptor.setDaemon(true);
            receptor.start();

            while (teclado.hasNextLine()) {
                String mensagem = teclado.nextLine();

                saida.println(mensagem);
            }
        } catch (IOException e) {
            System.out.println("Não foi possível conectar ao servidor " + servidor + ":" + porta + ".");
        }
    }
}
