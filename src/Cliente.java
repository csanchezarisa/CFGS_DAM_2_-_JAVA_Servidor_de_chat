/**
 2  * Cliente.java
 3  *
 4  * @author Luis Alejandro Bernal
 5  *
 6  * La parte cliente de un chat de un solo canal. Su objetivo es did�ctivo por lo que trata de
 7  * ser muy sencillo.
 8  */

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

import javax.swing.*;

class AccionEnviar implements ActionListener{
    private JTextField areaTexto;
    private PrintStream salida;
    private String login;

    public AccionEnviar(Socket s, JTextField at, String l){
        areaTexto = at;
        try {
            salida = new PrintStream(s.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        login = l;
    }

    public void actionPerformed(ActionEvent e){
        salida.println("**Normal~" + login + " > " + areaTexto.getText());
        areaTexto.setText("");
    }
}

class Talk {
    private Socket socket;
    private String login;

    public Talk(Socket s, String l){
        socket = s;
        login = l;
    }

    public void hablar(){

        JFrame marco = new JFrame(login);
        marco.setLayout(new BorderLayout());
        JTextArea areaTexto = new JTextArea("");
        areaTexto.setEditable(false);
        marco.add(areaTexto, "Center");
        JPanel panel = new JPanel(new FlowLayout());
        marco.add(panel, "South");
        JTextField campoTexto = new JTextField(30);
        panel.add(campoTexto);
        JButton botonEnviar = new JButton("Enviar");
        panel.add(botonEnviar);
        marco.setSize(600,800);
        marco.setVisible(true);
        AccionEnviar ae = new AccionEnviar(socket, campoTexto, login);
        botonEnviar.addActionListener(ae);
        BufferedReader entrada;
        PrintStream salida;
        try {
            salida = new PrintStream(socket.getOutputStream());
            salida.println("**Login~" +login + " se he conectado" );

            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String mensaje;
            while( (mensaje = entrada.readLine()) != null){
                areaTexto.setText(areaTexto.getText() + mensaje + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

public class Cliente {

    private static String direccion = "localhost";
    private static int puerto = 9999;
    private static String login = "";

    public static void main(String[] args)throws IOException {

        if(args.length >= 1){
            login = args[0];
        }
        if(args.length >= 2){
            direccion = args[1];
        }
        if(args.length >= 3){
            puerto = Integer.parseInt(args[2]);
        }

        login();


        while (Cliente.login == "") {
            System.out.println("Esperant login... Nom d'usuari " + login);
        }

        openTalk();
    }

    private static void login() throws IOException {
        JFrame frame = new JFrame("Login");
        frame.setLayout(new BorderLayout());
        JPanel jPanel = new JPanel(new FlowLayout());
        frame.add(jPanel, "Center");
        JLabel label = new JLabel("Nombre de usuario");
        jPanel.add(label);
        JTextField editText = new JTextField(30);
        jPanel.add(editText);

        JButton botonLogin = new JButton("Login");
        jPanel.add(botonLogin);
        frame.setSize(400, 200);
        frame.setVisible(true);

        botonLogin.addActionListener(e -> {
            String login = editText.getText();
            if (login.length() == 0) login = "Nadie";
            Cliente.login = login;
            frame.dispose();
        });
    }

    private static void openTalk() {
        Socket socket = setSocket();

        talk(socket);

        finalizeClient(socket);
    }

    private static Socket setSocket() {
        Socket socket = null;
        try {
            socket = new Socket(direccion, puerto);
        }
        catch (Exception e) {

        }
        return socket;
    }

    private static void talk(Socket socket) {
        Talk talk = new Talk(socket, login);
        talk.hablar();
    }

    private static void finalizeClient(Socket socket) {
        try {
            socket.close();
        }
        catch (Exception e) {
            System.exit(-1);
        }
        System.exit(0);
    }

}
