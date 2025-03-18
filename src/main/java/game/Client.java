package game;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;

public class Client extends Application {
    private String username;
    private Stage mainStage;

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
        return new Scene(root, 400, 300);
    }
}
