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
        for (ObjectOutputStream clientOutputStream : cimport java.io.*;
        import java.net.*;
        import java.util.*;
        import java.util.concurrent.*;

        import static java.nio.charset.StandardCharsets.UTF_8;

        public class MusicServer {

            // Shared list of output streams — one per connected client.
            // Used by tellEveryone() to broadcast messages to all clients.
            // TODO: Not thread-safe; concurrent writes from multiple ClientHandler threads
            // could cause issues. Consider using CopyOnWriteArrayList if needed.
            final List<ObjectOutputStream> clientOutputStreams = new ArrayList<>();

            public static void main(String[] args) {
                new MusicServer().go();
            }

            public void go() {
                try {
                    // Bind the server to port 4242 and wait for incoming client connections
                    ServerSocket serverSock = new ServerSocket(4242);

                    // newCachedThreadPool() creates new threads on demand and reuses idle ones.
                    // Good for short-lived tasks, but can spawn unbounded threads under heavy load.
                    ExecutorService threadPool = Executors.newCachedThreadPool();

                    while (!serverSock.isClosed()) {
                        // Blocks here until a client connects; returns a socket for that client.
                        // BUG: variable is named 'serverSock' above but referenced as 'serverSocket' here — won't compile.
                        Socket clientSocket = serverSocket.accept();

                        // ObjectOutputStream lets us send serialized Java objects (not just raw bytes/text)
                        // over the socket. Must be created BEFORE ObjectInputStream on the other end,
                        // otherwise both sides deadlock waiting for the stream header.
                        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                        clientOutputStreams.add(out);

                        // Hand off this client to a worker thread so the main loop can keep accepting new connections.
                        // BUG: ClientHandler expects a SocketChannel, but 'clientSocket' is a plain Socket — type mismatch.
                        threadPool.submit(new ClientHandler(clientSocket));
                        System.out.println("Got a connection");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            // Broadcasts both the username+message and the beat sequence to every connected client.
            // writeObject() serializes the Java object and sends it over the wire —
            // the receiving side must call readObject() in the same order to deserialize correctly.
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

            // Inner class — each connected client gets its own ClientHandler running on a separate thread.
            // Being a non-static inner class means it has implicit access to the outer MusicServer instance,
            // which is how it can call tellEveryone() directly.
            public class ClientHandler implements Runnable {
                BufferedReader reader;
                SocketChannel socket; // BUG: should be Socket (plain), not SocketChannel — mismatched with go()

                // BUG: constructor expects SocketChannel but go() passes a plain Socket.
                // Channels.newReader() wraps a NIO SocketChannel into a Reader with the given charset.
                // UTF_8 is explicitly specified to avoid platform-default charset issues.
                public ClientHandler(SocketChannel clientSocket) {
                    socket = clientSocket;
                    reader = new BufferedReader(Channels.newReader(socket, UTF_8));
                }

                public void run() {
                    String message;
                    try {
                        // readLine() blocks until a full line arrives or the connection closes (returns null).
                        // The loop naturally exits when the client disconnects.
                        while ((message = reader.readLine()) != null) {
                            System.out.println("read " + message);
                            // BUG: tellEveryone() requires two arguments (message + beatSequence),
                            // but only one is passed here — won't compile as-is.
                            tellEveryone(message);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
lientOutputStreams) {
            try {
                clientOutputStream.writeObject(usernameAndMessage);
                clientOutputStream.writeObject(beatSequence);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public class ClientHandler implements Runnable {
        BufferedReader reader;
        SocketChannel socket;

        public ClientHandler(SocketChannel clientSocket) {
            socket = clientSocket;
            reader = new BufferedReader(Channels.newReader(socket, UTF_8));
        }
        
        public void run() {
            String message;
            try {
                while ((message = reader.readLine()) != null) {
                    System.out.println("read " + message);
                    tellEveryone(message);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
