package org.academiadecodigo.bootcamp55.GroupChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatClient {

    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean isShutdown;

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.print("Hostname? ");
        String hostName = input.nextLine();

        System.out.print("Port? ");
        int portNumber = Integer.parseInt(input.nextLine());

        System.out.print("What's your name? ");
        String userName = input.nextLine();

        ChatClient client = new ChatClient();
        client.connect(hostName, portNumber, input, userName);
    }

    private void connect(String hostName, int portNumber, Scanner input, String userName) {
        try {
            clientSocket = new Socket(hostName, portNumber);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            System.out.println("Connection established with " + hostName + ":" + portNumber);

            ExecutorService cachedPool = Executors.newFixedThreadPool(2);
            cachedPool.submit(new Receiver());

            while (clientSocket.isConnected()) {
                String newInput = input.nextLine();
                if (newInput.equals("/quit")) {
                    out.println("/quit");
                    closeConnections();
                    cachedPool.shutdownNow();
                    break;
                }
                out.println(userName + ": " + newInput);
            }

        } catch (IOException e) {
            System.err.println("IOException whilst connecting to server and listening for input");
        }
    }

    private void closeConnections() {
        try {
            isShutdown = true;
            clientSocket.close();
            in.close();
            out.close();
            System.out.println("Connection closed");
        } catch (IOException ex) {
            System.err.println("IOException closing connections");
        }
    }

    private class Receiver implements Runnable {

        @Override
        public void run() {
            receiveMessage();
        }

        private synchronized void receiveMessage() {
            try {
                while (isShutdown) {
                    wait();
                }
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String messageReceived = in.readLine();
                System.out.println(messageReceived);
            } catch (IOException ex) {
                System.err.println("IOException whilst receiving Message");
            } catch (InterruptedException ex) {
                System.err.println("Interrupted exception whilst receiving Message");
            }
        }
    }
}
