/**
 * Servidor.java
 *
 * @author Luis Alejandro Bernal
 *
 * La parte servidor de un chat de un solo canal. Su objetivo es did�ctico por lo que trata de
 * ser muy sencillo. Tiene un ciclo que siempre se cloquea en el accept, cuando se desbloquea
 * crea un Serv, que es un servidor. �ste ;ultimo lo que hece es estar leyendo del socket
 * de su respectivo cliente y cuando llega un mensaje lo envia a la Lista se sockets.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

class ListaSockets{
    private Socket[] socket; // Los otros sockets
    private PrintStream[] salida;
    private String[] username;
    private int num;

    public ListaSockets(int n) {
        socket = new Socket[n];
        salida = new PrintStream[n];
        username = new String[n];
        num = 0;
    }

    public void add(Socket s){
        try {
            salida[num] = new PrintStream(s.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket[num++] = s;
    }

    public int length(){
        return num;
    }

    public Socket get(int n) {
        return socket[n];
    }

    public PrintStream getSalida(int n) {
        return salida[n];

    }

    /** Busca el socket dado y devuelve la posición en la que se encuentra
     * @param socket Socket a buscar
     * @return int. Número 0 o positivo si se ha encontrado. -1 si no.*/
    public int findSocket(Socket socket) {

        for (int i = 0; i < this.length(); i++) {
            if (this.get(i) == socket) return i;
        }

        return -1;
    }

    public int findSocket(String username) {

        for (int i = 0; i < this.length(); i++) {
            if (this.getUsername(i).equalsIgnoreCase(username)) return i;
        }

        return -1;
    }

    public void setUsername(String username, int n) {
        this.username[n] = username;
    }

    public String getUsername(int n) {
        return username[n];
    }
}

class Serv implements Runnable{
    private Socket socket; // Socket propio.
    private ListaSockets listaSockets; // Los otros sockets
    private String username = "";

    public Serv(Socket s, ListaSockets ls) {
        socket = s;
        listaSockets = ls;
    }

    public void run() {
        BufferedReader entrada;

        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String mensaje;
            while( (mensaje = entrada.readLine()) != null){

                String[] partesMensaje = mensaje.split("~");

                switch (partesMensaje[0]) {
                    case "**Normal" -> enviarMensajesNormal(partesMensaje);
                    case "**Login" -> funcionLogin(partesMensaje);
                    case "**Todo" -> recuperarMensajes();
                    case "**UnSol" -> enviarMensajePrivado(partesMensaje);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void funcionLogin(String[] partesMensaje) {
        String username = partesMensaje[1];
        this.username = username;
        int posicionSocket = listaSockets.findSocket(socket);
        listaSockets.setUsername(username, posicionSocket);

        String mensaje = "";

        for (int i = 2; i < partesMensaje.length; i++) {
            mensaje += partesMensaje[i];
        }

        Servidor.historialMensajes.add(mensaje);

        for (int i = 0; i < listaSockets.length(); i++) {
            listaSockets.getSalida(i).println(mensaje);
        }

    }

    /** Envía el mensaje a todos los Sockets que hay registrados */
    private void enviarMensajesNormal(String[] partesMensaje) {
        String mensaje = "";

        for (int i = 1; i < partesMensaje.length; i++) {
            mensaje += partesMensaje[i];
        }

        Servidor.historialMensajes.add(mensaje);

        for (int i = 0; i < listaSockets.length(); i++) {
            listaSockets.getSalida(i).println(mensaje);
        }
    }

    /** Envía al socket todos los mensajes públicos que se han ido guardando */
    private void recuperarMensajes() throws IOException {
        PrintStream output = new PrintStream(socket.getOutputStream());

        for (String mensaje:
             Servidor.historialMensajes) {
            output.println(mensaje);
        }

    }

    /** Permet enviar un missatge privat
     * @param partesMensaje Array de String con las siguientes partes:
     * Posicion 1 -> Nombre del usuario que envía el mensaje
     * Posicion 2 -> Nombre del usuario que recibirá el mensaje privado
     * Posicion >= 3 -> Partes del mensaje a enviar */
    private void enviarMensajePrivado(String[] partesMensaje) {
        String from = partesMensaje[1];
        String to = partesMensaje[2];

        int posicionSocketDestino = listaSockets.findSocket(to);
        int posicioSocketFrom = listaSockets.findSocket(socket);

        if (posicionSocketDestino >= 0 && posicioSocketFrom >= 0) {

            String mensaje = "[PRIVADO] " + from + ": " + to + " > ";

            for (int i = 3; i < partesMensaje.length; i++) {
                mensaje += partesMensaje[i];
            }


            listaSockets.getSalida(posicioSocketFrom).println(mensaje);
            listaSockets.getSalida(posicionSocketDestino).println(mensaje);

        }
        else if (posicioSocketFrom >= 0){
            listaSockets.getSalida(posicioSocketFrom).println("No se ha podido enviar el mensaje. No se ha encontrado el usuario " + to);
        }

    }

}

public class Servidor {
    public static ArrayList<String> historialMensajes = new ArrayList<>();
    private static final int puerto = 9999;
    private static final int max = 10000;

    public static void main(String[] args) {
        ServerSocket serverSocket;
        ListaSockets listaSockets = new ListaSockets(max);
        Serv[] serv = new Serv[max];
        Thread[] thread = new Thread[max];
        try {
            serverSocket = new ServerSocket(puerto);
            for(int i = 0; i < max; i++){
                Socket socket = serverSocket.accept();
                listaSockets.add(socket);
                serv[i] = new Serv(socket, listaSockets);
                thread[i] = new Thread(serv[i]);
                thread[i].start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}