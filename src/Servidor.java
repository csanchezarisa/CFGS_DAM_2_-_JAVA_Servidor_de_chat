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

    /** Busca el socket dado y devuelve la posición en la que se encuentra
     * @param username Nombre de usuario que hay que buscar en relación al socket
     * @return int. Número 0 o positivo si se ha encontrado. -1 si no.*/
    public int findSocket(String username) {

        for (int i = 0; i < this.length(); i++) {
            if (this.getUsername(i).equalsIgnoreCase(username)) return i;
        }

        return -1;
    }

    /** Asigna un nombre de usuario a una posición de la lista de sockets
     * el cual hace referencia al própio socket que ocupa la posición
     * en la lista
     * @param username String con el nombre de usuario a añadir
     * @param n integer con el número de posición en la que añadir
     * el nombre de usuario. (Debería ser la posición que ocupa el socket)*/
    public void setUsername(String username, int n) {
        this.username[n] = username;
    }

    /** Devuelve el nombre de usuario de una posición concreta
     * @param n integer con la posición deseada */
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
                    // Mensaje normal, para todo el mundo
                    case "**Normal" -> enviarMensajesNormal(partesMensaje);

                    // Mensaje de login, primera vez que se connecta un usuario
                    case "**Login" -> funcionLogin(partesMensaje);

                    // Mensaje para recuperar todos los mensajes
                    case "**Todo" -> recuperarMensajes();

                    // Mensaje privado
                    case "**UnSol" -> enviarMensajePrivado(partesMensaje);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Muestra el mensaje de login a todos los usuarios
     * Además, guarda el nombre de usuario en el propio
     * socket y en la posición de la lista que ocupa
     * este socket
     * @param partesMensaje Array de strings con las siguientes
     * posiciones necesárias:
     * 1 - Username
     * >=2 - Mensaje */
    private void funcionLogin(String[] partesMensaje) {

        // Se recupera el nombre ed usuario y se le asigna en el socket y en la lista
        String username = partesMensaje[1];
        this.username = username;
        int posicionSocket = listaSockets.findSocket(socket);
        listaSockets.setUsername(username, posicionSocket);

        // Se recupera el resto del mensaje
        String mensaje = "";
        for (int i = 2; i < partesMensaje.length; i++) {
            mensaje += partesMensaje[i];
        }

        // Se añade al ArrayList del historial de mensajes del servidor
        Servidor.historialMensajes.add(mensaje);

        // Se envía a todos los sockets que hay registrados
        for (int i = 0; i < listaSockets.length(); i++) {
            listaSockets.getSalida(i).println(mensaje);
        }

    }

    /** Envía el mensaje a todos los Sockets que hay registrados
     * @param partesMensaje Array de strings con el cuerpo del
     * mensaje a partir de la posición 1 */
    private void enviarMensajesNormal(String[] partesMensaje) {

        // Se monta el mensaje
        String mensaje = "";
        for (int i = 1; i < partesMensaje.length; i++) {
            mensaje += partesMensaje[i];
        }

        // Se añade al Araylist del historial de mensajes del servidor
        Servidor.historialMensajes.add(mensaje);

        // Se envía a todos los sockets
        for (int i = 0; i < listaSockets.length(); i++) {
            listaSockets.getSalida(i).println(mensaje);
        }
    }

    /** Envía al socket todos los mensajes públicos que se han ido guardando */
    private void recuperarMensajes() throws IOException {

        // Se crea un PrintStream para poder hacer un println que apunte al
        // socket (cliente) que hace la petición
        PrintStream output = new PrintStream(socket.getOutputStream());

        // Se recorre el arraylist con el historial de mensajes y se van enviando al cliente
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

        // Se recuperan el usuario que envia y recibe el mensaje
        String from = partesMensaje[1];
        String to = partesMensaje[2];

        // Se busca en qué posicion de la lista de sockets se encuentra el socket
        // que hace referencia a cada usuario (Métodos personalizados)
        int posicionSocketDestino = listaSockets.findSocket(to);
        int posicioSocketFrom = listaSockets.findSocket(socket);

        // Si ambas posiciones son >= 0 quiere decir que ocupan un lugar en la lista
        // por lo tanto, existen. Se envia el mensaje correctamente
        if (posicionSocketDestino >= 0 && posicioSocketFrom >= 0) {

            // Se monta el mensaje con el resto del array y los prefijos
            String mensaje = "[PRIVADO] " + from + ": " + to + " > ";
            for (int i = 3; i < partesMensaje.length; i++) {
                mensaje += partesMensaje[i];
            }

            // Se envía el mensaje con la salida de los sockets de las dos posiciones de la lista
            listaSockets.getSalida(posicioSocketFrom).println(mensaje);
            listaSockets.getSalida(posicionSocketDestino).println(mensaje);

        }

        // Si solo es >= 0 el socket inicio, quiere decir que no se ha encontrado el destino.
        // Al socket inicio se le envia un mensaje notificando del problema y que el usuario
        // especificasdo no se ha encontrado
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