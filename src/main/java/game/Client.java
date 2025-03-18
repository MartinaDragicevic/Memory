package game;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.*;
import javafx.application.Platform;

public class Client extends Application {
    private String username;
    private Stage mainStage;
    private ListView<String> userList = new ListView<>();
    private BufferedReader in;
    private PrintWriter out;

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
}
