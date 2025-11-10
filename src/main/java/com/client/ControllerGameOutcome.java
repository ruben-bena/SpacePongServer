package com.client;

import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ControllerGameOutcome {
    @FXML private Label labelGameOutcome;
    @FXML private Button buttonPlayerSelection, buttonExit;

    private final String TEXT_DRAW = "Draw. No one wins...";
    private final String TEXT_WIN = "¡You've won!";
    private final String TEXT_LOSE = "You've lost...";

    public void updateLabelGameOutcome(JSONObject msgObj) {
        System.out.println("Entro en updateLabelGameOutcome");
        boolean isDraw = msgObj.getBoolean("isDraw");
        if (isDraw) {
            labelGameOutcome.setText(TEXT_DRAW);
            return;
        }
        
        String winner = msgObj.getString("winnerName");
        boolean clientWon = winner.equals(Main.clientName);
        if (clientWon) {
            labelGameOutcome.setText(TEXT_WIN);
        } else {
            labelGameOutcome.setText(TEXT_LOSE);
        }
    }

    public void toViewPlayerSelection() {
        System.out.println("Entro en toViewPlayerSelection");
        Main.sendAvaliblePlayerMessage();
        Main.setViewPlayerSelection();
    }

    public void callExit() {
        System.out.println("Entro en callExit");
        Main.closeClient();
    }
}
