package com.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ControllerCountdown {

    @FXML private Label labelCountdown, labelPlayer1, labelPlayer2;

    public void updateLabelCountdown(int value) {
        if (labelCountdown != null) {
            labelCountdown.setText(String.valueOf(value));
        }
    }

    public void setPlayerLabels(String player_1, String player_2) {
        labelPlayer1.setText(player_1);
        labelPlayer2.setText(player_2);
    }
}
