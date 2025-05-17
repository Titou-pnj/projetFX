package com.example;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.text.Font;

public class App extends Application {
    
    public void start(Stage stage) {
        Label message = new Label("JavaFX Application");
        message.setFont(new Font(40));
        
        Button helloButton = new Button("Bonjour");
        helloButton.setOnAction(evt -> {message.setText("Bonjour !");
                                        System.out.println("Bonjour !");
                                       });
        //helloButton.setOnAction(new EventHandler<ActionEvent>() {
        //    @Override
        //    public void handle(ActionEvent t) {
        //        message.setText("Bonjour !");
        //        System.out.println("Bonjour !");
        //    }
        //});

        Button byeButton = new Button("Au revoir");
        byeButton.setOnAction(evt -> {message.setText("Au revoir !");});
        
        HBox buttonBar = new HBox(20, helloButton, byeButton);
        buttonBar.setAlignment(Pos.CENTER);
        
        BorderPane root = new BorderPane();
        root.setCenter(message);
        root.setBottom(buttonBar);
        
        //StackPane root = new StackPane();
        //root.getChildren().addAll(message,buttonBar);

        //FlowPane root = new FlowPane();
        //root.getChildren().addAll(message,buttonBar);

        //HBox root = new HBox();
        //root.getChildren().addAll(message,buttonBar);

        //VBox root = new VBox();
        //root.getChildren().addAll(message,buttonBar);

        Scene scene = new Scene(root, 450, 200);
        stage.setScene(scene);
        stage.setTitle("JavaFX Exemple");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args); 
    }
}