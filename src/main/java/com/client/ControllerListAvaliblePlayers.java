package com.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ControllerListAvaliblePlayers {

    @FXML private Label labelPlayerName;
    @FXML private Button buttonChallenge;

    public void setPlayerName(String name) {
        labelPlayerName.setText(name);
    }

    public Button getButtonChallenge() {
        return buttonChallenge;
    }
}
