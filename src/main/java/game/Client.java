package game;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert.AlertType;

import java.io.*;
import java.net.*;
import java.util.*;
import javafx.application.Platform;

public class Client extends Application {
    private String username;
    private Stage mainStage;
    private ListView<String> userList = new ListView<>();
    private BufferedReader in;
    private PrintWriter out;
    private MemoryGame activeGame;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        mainStage = primaryStage;
        showLoginStage(primaryStage);
    }

    private void showLoginStage(Stage primaryStage) {
        VBox loginRoot = new VBox(10);
        loginRoot.setPadding(new Insets(20, 20, 20, 20));
        loginRoot.setAlignment(Pos.CENTER);
        Scene loginScene = new Scene(loginRoot, 400, 250);
        TextField usernameField = new TextField();
        usernameField.setPromptText("Unesi korisnicko ime:");

        Button loginButton = new Button("Uloguj se");
        loginButton.setOnAction(event -> {
            username = usernameField.getText().trim();
            if (!username.isEmpty()) {
                connectToServer(primaryStage);
                primaryStage.setScene(createMainScene());
            }
        });

        loginRoot.getChildren().addAll(new Label("Korisnicko ime:"), usernameField, loginButton);
        primaryStage.setScene(loginScene);
        primaryStage.setTitle("Igra memorije");
        primaryStage.show();
    }

    private Scene createMainScene() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(20, 20, 20, 20));
        Scene scene = new Scene(root, 500, 400);
        Button requestGameButton = new Button("Posalji zahtjev");
        requestGameButton.setOnAction(event -> {
            String selectedUser = userList.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                out.println("GAME_REQUEST " + selectedUser);
            }
        });

        root.getChildren().addAll(new Label("Aktivni korisnici:"), userList, requestGameButton);
        return scene;
    }

    private void connectToServer(Stage primaryStage) {
        try {
            Socket socket = new Socket("localhost", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            new Thread(() -> {
                try {
                    while (true) {
                        String line = in.readLine();
                        if (line.startsWith("SUBMIT_NAME")) {
                            out.println(username);
                        } else if (line.startsWith("NAME_ACCEPTED")) {
                            Platform.runLater(() -> setTitle(primaryStage, line.substring(13)));
                            this.username = line.substring(13);
                        } else if (line.startsWith("USER_LIST")) {
                            Platform.runLater(() -> updateUserList(line.substring(10)));
                        } else if (line.startsWith("GAME_REQUEST")) {
                            String requester = line.substring(13);
                            Platform.runLater(() -> showGameRequestDialog(requester));
                        } else if (line.startsWith("GAME_START")) {
                            String board = line.substring(11);
                            if (line.charAt(10) == '1') {
                                Platform.runLater(() -> showGameWindow(board, 1));
                            } else {
                                Platform.runLater(() -> showGameWindow(board, 0));

                            }
                        } else if (line.startsWith("YOUR_TURN")) {
                            Platform.runLater(() -> activeGame.setMyTurn(true));
                        } else if (line.startsWith("BOARD")) {
                            String board = line.substring(5);
                            board = board.trim();
                            activeGame.board = board;
                            Platform.runLater(() -> activeGame.boardClearance());
                        } else if (line.startsWith("SCORE_CHANGE")) {
                            int score = Integer.parseInt(line.substring(12).trim());
                            Platform.runLater(() -> activeGame.opponentScore(score));

                        } else if (line.startsWith("GAME_OVER")) {
                            Platform.runLater(() -> activeGame.finishGame(line.substring(9)));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        primaryStage.setOnCloseRequest(event -> {
            if (out != null) {
                out.println("LOGOUT");
            }
            System.exit(0);
        });
    }

    private void setTitle(Stage stage, String username) {
        stage.setTitle("Igra memorije - " + username);
    }

    private void updateUserList(String users) {
        String[] userArray = users.split(" ");
        userList.getItems().clear();
        for (String user : userArray) {
            if (!user.trim().equals(username.trim())) {
                userList.getItems().add(user);
            }
        }
    }

    private void showGameRequestDialog(String requester) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(mainStage);
        VBox vbox = new VBox(new Text("Game request from " + requester), new Button("Accept"));
        vbox.setSpacing(10);

        Scene scene = new Scene(vbox, 200, 100);
        dialogStage.setScene(scene);
        dialogStage.show();

        ((Button) vbox.getChildren().get(1)).setOnAction(event -> {
            out.println("GAME_ACCEPTED " + requester);
            dialogStage.close();
        });
    }

    private void showGameWindow(String board, int plays) {
        mainStage.hide();
        MemoryGame game = new MemoryGame(mainStage, out, board, plays);
        activeGame = game;
        game.start(new Stage());
    }

    public class MemoryGame extends Application {
        private Stage mainStage;
        private PrintWriter out;
        private boolean myTurn = true;
        private int myPoints = 0;
        private int opponentPoints = 0;
        private Label statusLabel;
        private Label myPointsLabel;
        private Label opponentPointsLabel;
        private List<Button> buttons = new ArrayList<>();
        private String[] cards;
        private String board;
        private GridPane gridPane = new GridPane();
        private Button firstSelected = null;
        private Button secondSelected = null;
        private Stage stage = null;

        public MemoryGame(Stage mainStage, PrintWriter out, String board, int plays) {
            this.mainStage = mainStage;
            this.out = out;
            this.board = board;
            if (plays == 0) {
                this.myTurn = false;
            }
        }

        @Override
        public void start(Stage gameStage) {
            VBox root = new VBox(10);
            this.stage = gameStage;
            Scene gameScene = new Scene(root, 600, 700);
            gameStage.setTitle(username);
            statusLabel = new Label(myTurn ? "Tvoj potez" : "Protivnikov potez");
            myPointsLabel = new Label("Tvoji poeni: 0");
            opponentPointsLabel = new Label("Protivnikovi poeni: 0");

            this.createBoard();
            root.getChildren().addAll(statusLabel, myPointsLabel, opponentPointsLabel, gridPane);
            gameStage.setScene(gameScene);
            gameStage.show();

            gameStage.setOnCloseRequest(event -> {
                out.println("QUIT_GAME");
                mainStage.show();
            });
        }

        public void finishGame(String winner) {
            activeGame = null;
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Game over");
            alert.setHeaderText(null);
            if (winner.trim().equals("X")) {
                alert.setContentText("Game over. It's tied.");
            } else if (winner.trim().equals("D")) {
                alert.setContentText("Game over beacuse of disconnection.");
            } else {
                alert.setContentText("Game over. The winner is " + winner);

            }
            alert.showAndWait();
            stage.close();
            mainStage.show();
        }

        private void boardClearance() {
            gridPane.getChildren().clear();
            initializeCards(this.board);
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 6; j++) {
                    Button button = createCardButton(cards[i * 6 + j]);
                    buttons.add(button);
                    gridPane.add(button, j, i);
                }
            }
        }

        private void createBoard() {
            boardClearance();
        }

        private void initializeCards(String board) {
            cards = new String[36];
            board = board.trim();
            cards = board.split(" ");
        }

        private Button createCardButton(String cardValue) {
            Button button = null;
            if (cardValue.charAt(cardValue.length() - 1) == '?') {
                button = new Button("?");
                button.setMinSize(100, 100);
                final Button button2 = button;
                button.setOnAction(event -> handleCardClick(button2, cardValue.substring(0, cardValue.length() - 1)));
            } else {
                button = new Button(cardValue.substring(0, cardValue.length() - 1));
                button.setMinSize(100, 100);
                button.setDisable(true);
                final Button button2 = button;
                button.setOnAction(event -> handleCardClick(button2, cardValue.substring(0, cardValue.length() - 1)));
            }
            return button;
        }

        private void opponentScore(int score) {
            opponentPoints = score;
            opponentPointsLabel.setText("Protivnikovi poeni: " + opponentPoints);
        }

        private void sendBoard() {
            StringBuilder sb = new StringBuilder();
            for (String card : cards) {
                sb.append(card + " ");
            }
            out.println("BOARD " + sb.toString() + " " + username);
        }

        private void handleCardClick(Button button, String cardValue) {
            if (!myTurn) return;
            if (firstSelected == null) {
                firstSelected = button;
                int column = gridPane.getColumnIndex(button);
                int row = gridPane.getRowIndex(button);
                String s = cards[row * 6 + column];

                String b = s.replace('?', '!');
                cards[row * 6 + column] = b;
                sendBoard();
                button.setText(cardValue);
            } else if (secondSelected == null && button != firstSelected) {
                secondSelected = button;
                button.setText(cardValue);
                out.println("CHANGE_TURN " + username);
                int column = gridPane.getColumnIndex(button);
                int row = gridPane.getRowIndex(button);
                String s = cards[row * 6 + column];
                String b = s.replace('?', '!');
                cards[row * 6 + column] = b;
                sendBoard();
                button.setText(cardValue);
                sendBoard();
                checkMatch();
                myTurn = !myTurn;
            }
        }

        private void checkMatch() {
            if (firstSelected.getText().equals(secondSelected.getText())) {
                firstSelected.setDisable(true);
                secondSelected.setDisable(true);
                if (myTurn) {
                    myPoints++;
                    myPointsLabel.setText("Your points: " + myPoints);
                    out.println("SCORE_CHANGE " + username + " " + myPoints);
                }
                resetTurn();
            } else {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            firstSelected.setText("?");
                            secondSelected.setText("?");
                            Button button = firstSelected;
                            int column = gridPane.getColumnIndex(button);
                            int row = gridPane.getRowIndex(button);
                            String s = cards[row * 6 + column];
                            String b = s.replace('!', '?');
                            cards[row * 6 + column] = b;
                            button = secondSelected;
                            column = gridPane.getColumnIndex(button);
                            row = gridPane.getRowIndex(button);
                            s = cards[row * 6 + column];
                            b = s.replace('!', '?');
                            cards[row * 6 + column] = b;
                            sendBoard();
                            resetTurn();
                        });
                    }
                }, 1000);
            }
        }

        public boolean isMyTurn() {
            return myTurn;
        }

        public void setMyTurn(boolean myTurn) {
            this.myTurn = myTurn;
            statusLabel.setText(myTurn ? "Tvoj potez" : "Protivnikov potez");
        }

        private void resetTurn() {
            firstSelected = null;
            secondSelected = null;
            statusLabel.setText(myTurn ? "Tvoj potez" : "Protivnikov potez");
        }
    }
}
