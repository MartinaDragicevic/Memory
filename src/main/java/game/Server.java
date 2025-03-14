package game;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static Map<String, PrintWriter> clients = new HashMap<>();
    private static Set<String> inGameClients = new HashSet<>();
    private static List<Game> activeGames = new ArrayList<>();

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

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("LOGOUT")) {
                        break;
                    } else if (message.startsWith("GAME_REQUEST")) {
                        String[] tokens = message.split(" ", 2);
                        String targetUser = tokens[1];
                        synchronized (clients) {
                            PrintWriter targetOut = clients.get(targetUser);
                            if (targetOut != null) {
                                targetOut.println("GAME_REQUEST " + username);
                            }
                        }
                    } else if (message.startsWith("GAME_ACCEPTED")) {
                        String[] tokens = message.split(" ", 2);
                        String targetUser = tokens[1];
                        synchronized (clients) {
                            PrintWriter targetOut = clients.get(targetUser);
                            if (targetOut != null) {
                                String[] cards = new String[36];
                                for (int i = 0; i < 18; i++) {
                                    cards[i * 2] = String.valueOf(i) + "?";
                                    cards[i * 2 + 1] = String.valueOf(i) + "?";
                                }

                                Collections.shuffle(Arrays.asList(cards));
                                String board = "";
                                for (int i = 0; i < 36; i++) {
                                    board += cards[i] + " ";
                                }
                                Game game = new Game(board, out, username, targetOut, targetUser, 1);
                                activeGames.add(game);
                                targetOut.println("GAME_START" + "1 " + board);
                                out.println("GAME_START" + "0 " + board);
                                inGameClients.add(username);
                                inGameClients.add(targetUser);
                                clients.remove(username);
                                clients.remove(targetUser);
                                broadcastUserList();
                            }
                        }
                    } else if (message.startsWith("CHANGE_TURN")) {
                        String user = message.substring(12);
                        for (Game game : activeGames) {
                            if (game.getPlayer1username().equalsIgnoreCase(user.trim()) || game.getPlayer2username().equalsIgnoreCase(user.trim())) {
                                game.changeTurn();
                                break;
                            }
                        }
                    } else if (message.startsWith("BOARD")) {
                        String user = message.substring(132);
                        String board = message.substring(5, 132);
                        if (board.indexOf('?') == -1) {
                            for (Game game : activeGames) {
                                if (game.getPlayer1username().equalsIgnoreCase(user.trim()) || game.getPlayer2username().equalsIgnoreCase(user.trim())) {
                                    game.gameOver();
                                    activeGames.remove(game);
                                    break;
                                }
                            }
                        } else {
                            for (Game game : activeGames) {
                                if (game.getPlayer1username().equalsIgnoreCase(user.trim()) || game.getPlayer2username().equalsIgnoreCase(user.trim())) {
                                    game.boardUpdate(user, board);
                                    break;
                                }
                            }
                        }
                    } else if (message.startsWith("SCORE_CHANGE")) {
                        String s = message.substring(12);

                        String[] a = s.trim().split(" ");
                        String user = a[0];
                        int point = Integer.parseInt(a[1]);
                        for (Game game : activeGames) {
                            if (game.getPlayer1username().equalsIgnoreCase(user.trim()) || game.getPlayer2username().equalsIgnoreCase(user.trim())) {
                                game.setScore(user, point);
                                break;
                            }
                        }
                    } else if (message.startsWith("QUIT_GAME")) {
                        inGameClients.remove(username);
                        Game selectedGame = null;
                        for (Game game : activeGames) {
                            if (game.getPlayer1username().equalsIgnoreCase(username.trim()) || game.getPlayer2username().equalsIgnoreCase(username.trim())) {
                                selectedGame = game;

                                break;
                            }
                        }
                        synchronized (clients) {
                            clients.put(username, out);
                            broadcastUserList();
                        }
                        selectedGame.gameOverDisc();
                        if (activeGames.contains(selectedGame))
                            activeGames.remove(activeGames.indexOf(selectedGame));
                        broadcastUserList();
                    }
                }

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