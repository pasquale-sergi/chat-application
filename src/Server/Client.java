package Server;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;

    private Socket socket;
    private String username;







    public Client(Socket socket, String username){
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;

            //we send the username to the client handler so it can assign it
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();

        }catch (IOException e ){
            closeEverything();
        }
    }

    public void sendMessage() throws IOException {
        try {
            Scanner scanner = new Scanner(System.in);
            while(socket.isConnected()){
                String messageToSend = scanner.nextLine();
                if(messageToSend.equalsIgnoreCase("/quit")||messageToSend==null){
                    System.out.println("You disconnected from the chat");
                    bufferedWriter.write("/quit");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    closeEverything();
                }
                    bufferedWriter.write(username + ": " + messageToSend);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }

        }catch (IOException e){
            closeEverything();
        }
    }

    public void listenForMessage(){
        //new thread for the same reasons of before
        new Thread(()-> {
                String messageFromGroupChat;
                while(socket.isConnected()) {
                    try {
                        messageFromGroupChat = bufferedReader.readLine();
                        if(messageFromGroupChat==null){
                            closeEverything();
                        }
                        System.out.println(messageFromGroupChat);

                    } catch (IOException e) {
                        closeEverything();
                    }
                }
        }).start();
    }

    public void closeEverything(){
        try {

            if(bufferedReader != null){
                bufferedReader.close();
            }
            if(bufferedWriter!=null){
                bufferedWriter.close();
            }

            if(socket!=null){
                socket.close();
            }
        }catch (IOException e ){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Enter your username for the group chat: ");
            String chatUsername = bufferedReader.readLine();

            Socket socket = new Socket("localhost", 1234);
            Client client = new Client(socket, chatUsername);
            //no issue with this two methods cause thanks to threads we can run them at the same time
        try {
            client.listenForMessage();
            client.sendMessage();
        }catch (RuntimeException e){
            client.closeEverything();
        }
    }
}
