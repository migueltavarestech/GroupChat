package org.academiadecodigo.bootcamp55.GroupChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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
        private PrintWriter out;
        private String name;
        private BufferedReader in;

        public ServerWorker(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public void send(String message){
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getName(){
            return name;
        }

        public String listUsers(){
            StringBuilder result = new StringBuilder("\n===USER LIST===\n");
            for(ServerWorker worker:workerList){
                result.append(worker.getName()).append("\n");
            }
            return result.toString();
        }

        public void setName(String name){
            this.name = name;
        }

        @Override
        public void run(){
            try {
                int messagesReceived = 0;
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                while (!clientSocket.isClosed()) {

                    String messageReceived = in.readLine();
                    String[] messageArr = new String[2];
                    if (messageReceived.contains(" ")){
                        messageArr = messageReceived.split(" ");
                    }
                    messagesReceived++;

                    sendMessage(messageReceived, messageArr, messagesReceived);
                }
            } catch (IOException ex) {
                try {
                    clientSocket.close();
                    in.close();
                    workerList.remove(this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public synchronized void quit(){
            try {
                workerList.remove(this);
                send("/quit");
                clientSocket.close();
                in.close();
            } catch (IOException e) {
                System.out.println("IOException exiting in the ServerWorker");
            }
        }

        public synchronized void kick(String name){
            for(ServerWorker worker:workerList){
                if(worker.getName().equals(name)){
                    worker.quit();
                    return;
                }
            }
            send("This user does not exist.");
        }

        private void sendMessage(String messageReceived, String[] messageArr, int messagesReceived){
            if(messagesReceived == 1){
                this.name = messageReceived;
            } else if (messageReceived.equals("/quit")) {
                quit();
            } else if (messageReceived.equals("/list")) {
                send(listUsers());
            } else if (messageArr[0].equals("/rename")) {
                sendAll(name + " changed his username to " + messageReceived.substring(messageReceived.indexOf(" ")+1));
                setName(messageArr[1]);
            } else if (messageArr[0].equals("/kick")) {
                sendAll(name + " kicked " + messageArr[1]);
                kick(messageArr[1]);
            } else if (messageReceived.startsWith("@")) {
                String sendTo = messageReceived.substring(1,messageReceived.indexOf(" "));
                String messageToSend = messageReceived.substring(messageReceived.indexOf(" "));
                for(ServerWorker worker:workerList){
                    if(worker.getName().equals(sendTo)){
                        worker.send("PM from " + name + ":" + messageToSend);
                        send(name + " sent PM to " + worker.getName() + ":" + messageToSend);
                        return;
                    }
                }
                send("User doesn't exist");
            } else {
                sendAll(messageReceived);
            }
        }
    }
}
