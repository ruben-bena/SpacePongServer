package com.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ControllerWait {
    @FXML private Label labelChallengedName;

    @FXML
    public void updateChallengedName(String challengedName) {
        labelChallengedName.setText(challengedName);
    }

    public void cancelChallenge() {
        System.out.println("Entro en cancelChallenge");
        Main.sendCancelledChallenge(labelChallengedName.getText());
    }
}
