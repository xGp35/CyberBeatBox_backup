import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MusicServer {
    final List<ObjectOutputStream> clientOutputStreams = new ArrayList<>();

    public static void main(String[] args) {
        new MusicServer().go();
    }

    public void go() {
        try {
            ServerSocket serverSock = new ServerSocket(4242);
            ExecutorService threadPool = Executors.newCachedThreadPool();

            while (!serverSock.isClosed()) {
                // Keep listening for client connections;
                Socket clientSocket = serverSock.accept();
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                clientOutputStreams.add(out);
                // Create a new Socket and new Client Handler for each connected client.
                threadPool.submit(new ClientHandler(clientSocket));
                System.out.println("Got a connection");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void tellEveryone(Object usernameAndMessage, Object beatSequence) {
        for (ObjectOutputStream clientOutputStream : clientOutputStreams) {
            try {
                clientOutputStream.writeObject(usernameAndMessage);
                clientOutputStream.writeObject(beatSequence);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ClientHandler implements Runnable {
        private ObjectInputStream in;

        public ClientHandler (Socket socket) {
            try {
                in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            Object userName;
            Object bearSequence;
            try {
                while ((userName = in.readObject()) != null) {
                    beatSequence = in.readObject();
                    // First reads the username object then the beatSeqence object in tandem
                    // one after another, one after another in the while loop , until 
                    // the userName is null, at which point we need to stop

                    System.out.println("read two objects");
                    tellEveryone(userName, beatSequence);
                    // Once we got the message and the beat sequence, send these to all the clients
                    // (including this one)
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    // public class ClientHandler implements Runnable {
    //     BufferedReader reader;
    //     SocketChannel socket;

    //     public ClientHandler(SocketChannel clientSocket) {
    //         socket = clientSocket;
    //         reader = new BufferedReader(Channels.newReader(socket, UTF_8));
    //     }
        
    //     public void run() {
    //         String message;
    //         try {
    //             while ((message = reader.readLine()) != null) {
    //                 System.out.println("read " + message);
    //                 tellEveryone(message);
    //             }
    //         } catch (IOException ex) {
    //             ex.printStackTrace();
    //         }
    //     }
    // }
}
