package org.academiadecodigo.bootcamp55.GroupChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private LinkedList<ServerWorker> workerList;

    public static void main(String[] args){
        Scanner input = new Scanner(System.in);
        System.out.print("Port? ");
        int portNumber = Integer.parseInt(input.nextLine());

        ChatServer server = new ChatServer();
        server.startListening(portNumber);
    }

    private void startListening(int portNumber){
        try {
            System.out.println("Started listening on port " + portNumber);
            ServerSocket serverSocket = new ServerSocket(portNumber);
            ExecutorService cachedPool = Executors.newFixedThreadPool(4);
            workerList = new LinkedList<ServerWorker>();
            while(serverSocket.isBound()){
                Socket clientSocket = serverSocket.accept();
                ServerWorker serverWorker = new ServerWorker(clientSocket);
                workerList.add(serverWorker);
                cachedPool.submit(serverWorker);
            }
        } catch (IOException e) {
            System.err.println("IOException while listening");
        }
    }

    private void sendAll(String message){
        synchronized (workerList) {
            for (ServerWorker worker : workerList) {
                worker.send(message);
            }
        }
    }

    private class ServerWorker implements Runnable {

        private Socket clientSocket;

        public ServerWorker(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public void send(String message){
            try {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run(){
            try {
                while (clientSocket.isConnected()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String messageReceived = in.readLine();
                    if(messageReceived.equals("/quit")){
                        clientSocket.close();
                        workerList.remove(this);
                        break;
                    } else {
                        sendAll(messageReceived);
                    }
                }
            } catch (IOException ex) {
                System.err.println("IOException whilst running ServerWorker");
            }
        }
    }
}
