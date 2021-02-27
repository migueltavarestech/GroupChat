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

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.print("Hostname? ");
        String hostName = input.nextLine();

        System.out.print("Port? ");
        int portNumber = Integer.parseInt(input.nextLine());

        String userName = askForUsername(input);
        while (userName.contains(" ")) {
            System.out.println("Username can't have whitespace, please try again");
            userName = askForUsername(input);
        }

        ChatClient client = new ChatClient();
        client.connect(hostName, portNumber, input, userName);
    }

    private static String askForUsername(Scanner input){
        System.out.print("Pick your username: ");
        String result = input.nextLine();
        return result;
    }

    private void connect(String hostName, int portNumber, Scanner input, String userName) {
        try {
            clientSocket = new Socket(hostName, portNumber);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            System.out.println("Connection established with " + hostName + ":" + portNumber);

            ExecutorService newThread = Executors.newSingleThreadExecutor();
            newThread.submit(new Receiver());
            out.println(userName);

            while (!clientSocket.isClosed()) {
                String newInput = input.nextLine();
                if (newInput.equals("/quit")) {
                    out.println(userName + " has left the group chat");
                    out.println("/quit");
                    closeConnections();
                    System.exit(0);
                    break;
                } else if (newInput.startsWith("/rename")) {
                    userName = newInput.split(" ")[1];
                    out.println(newInput);
                } else if (newInput.equals("/list")) {
                    out.println(newInput);
                } else if(newInput.startsWith("/kick")) {
                    out.println(newInput);
                } else if (newInput.startsWith("@")) {
                    out.println(newInput);
                } else {
                    out.println(userName + ": " + newInput);
                }
            }

        } catch (IOException e) {
            closeConnections();
        }
    }

    private void closeConnections() {
        try {
            out.println("/quit");
            clientSocket.close();
            out.close();
            in.close();
            System.out.println("Connection closed");
        } catch (IOException ex) {
            System.err.println("IOException closing connections");
        }
    }

    private class Receiver implements Runnable {

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                while(!clientSocket.isClosed()) {
                     receiveMessage();
                }
            } catch (IOException e) {
                closeConnections();
            }
        }

        private synchronized void receiveMessage() {
            try {
                String messageReceived = in.readLine();
                if(messageReceived.equals("/quit")) {
                    closeConnections();
                    System.exit(0);
                } else {
                    System.out.println(messageReceived);
                }
            } catch (IOException ex) {
                System.err.println("IOException whilst receiving Message");
            }
        }
    }
}
