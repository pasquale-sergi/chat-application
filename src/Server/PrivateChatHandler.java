package Server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class PrivateChatHandler implements Runnable {
    private final ClientHandler client1;
    private final ClientHandler client2;
    private final BufferedWriter out1;
    private final BufferedReader in1;
    private final BufferedWriter out2;
    private final BufferedReader in2;

    public PrivateChatHandler(ClientHandler client1, ClientHandler client2) {
        this.client1 = client1;
        this.client2 = client2;
        this.out1 = client1.getBufferedWriter();
        this.in1 = client1.getBufferedReader();
        this.out2 = client2.getBufferedWriter();
        this.in2 = client2.getBufferedReader();
    }

    @Override
    public void run() {
        Thread client1ToClient2Thread = new Thread(() -> handleClientMessages(in1, out2, client1.getClientUsername()));
        Thread client2ToClient1Thread = new Thread(() -> handleClientMessages(in2, out1, client2.getClientUsername()));

        client1ToClient2Thread.start();
        client2ToClient1Thread.start();

        try {
            client1ToClient2Thread.join();
            client2ToClient1Thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            endPrivateChat();
        }
    }

    private void handleClientMessages(BufferedReader in, BufferedWriter out, String senderUsername) {
        try {
            String message;
            while ((message = client2.splitInput(in.readLine())) != null) {
                if (message.equalsIgnoreCase("exit")) {
                    closeStreams();
                    break;
                }
                sendMessage(out, senderUsername, message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(BufferedWriter out, String senderUsername, String message) throws IOException {
        out.write(senderUsername + ": " + message);
        out.newLine();
        out.flush();
    }

    private void endPrivateChat() {
        try {
            out1.write("Private chat ended.");
            out1.newLine();
            out1.flush();
            out2.write("Private chat ended.");
            out2.newLine();
            out2.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeStreams();
        }
    }



    private void closeStreams() {
        try {
            client1.setIsInPrivateChat(false);
            client2.setIsInPrivateChat(false);
            in1.close();
            out1.close();
            in2.close();
            out2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

