package game;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static Map<String, PrintWriter> clients = new HashMap<>();
    private static Set<String> inGameClients = new HashSet<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server is running...");
        while (true) {
            Socket socket = serverSocket.accept();
            new ClientHandler(socket).start();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private String username;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                while (true) {
                    out.println("SUBMIT_NAME");
                    username = in.readLine();
                    if (username == null) {
                        return;
                    }
                    synchronized (clients) {
                        if (!clients.containsKey(username) && !inGameClients.contains(username)) {
                            clients.put(username, out);
                            break;
                        } else {
                            int count = 1;
                            String newUsername;
                            while (clients.containsKey(newUsername = username + "(" + count + ")") || inGameClients.contains(newUsername)) {
                                count++;
                            }
                            username = newUsername;
                            clients.put(username, out);
                            break;
                        }
                    }
                }

                out.println("NAME_ACCEPTED " + username);
                broadcastUserList();

            } catch (IOException e) {
                System.out.println(e);
            } finally {
                if (username != null) {
                    clients.remove(username);
                    broadcastUserList();
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        }


        private void broadcastUserList() {
            StringBuilder userList = new StringBuilder("USER_LIST");
            synchronized (clients) {
                for (String user : clients.keySet()) {
                    userList.append(" ").append(user);
                }
                for (PrintWriter writer : clients.values()) {
                    writer.println(userList.toString());
                }
            }
        }

    }
}