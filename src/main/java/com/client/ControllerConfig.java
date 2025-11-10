package com.client;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ControllerConfig {
    @FXML
    public TextField textFieldProtocol, textFieldHost, textFieldPort, textFieldUsername;
    @FXML
    public Button buttonConnect, buttonLocal, buttonProxmox, buttonRandomUsername;
    @FXML
    public Label labelMessage;

    /*
     * TODO Hacer que los botones se deshabiliten cuando le das a Connect, y se habiliten cuando se resuelva la conexión (si esta falla)
     * 
     *  
     * 
     * 
     */ 

    // Nombres aleatorios por si el usuario no pone uno
    private static final List<String> PLAYER_NAMES = Arrays.asList(
        "Bulbasaur", "Charizard", "Blaziken", "Umbreon", "Mewtwo", "Pikachu", "Wartortle"
    );

    @FXML
    private void connectToServer() {
        if (textFieldUsername.getText().equals("")) {
            setRandomUsername();
        }
        Main.connectToServer();
    }

    @FXML
    private void setConfigLocal() {
        textFieldProtocol.setText("ws");
        textFieldHost.setText("localhost");
        textFieldPort.setText("3000");
    }

    @FXML
    private void setConfigProxmox() {
        textFieldProtocol.setText("wss");
        textFieldHost.setText("rbellidonavarro.ieti.site");
        textFieldPort.setText("443");
    }

    @FXML
    private void setRandomUsername() {
        int randomNumber = (int) (Math.random() * 100);
        textFieldUsername.setText(getRandomUsername() + String.valueOf(randomNumber)); // p.ej. Pikachu76
    }

    private String getRandomUsername() {
        Random random = new Random();
        int i = random.nextInt(PLAYER_NAMES.size());
        String randomName = PLAYER_NAMES.get(i);
        return randomName;
    }

    public String getUsername() {
        return textFieldUsername.getText();
    }
}
