package com.client;

import org.json.JSONObject;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    // Inicializar instancia de WebSocket antes de usarla
    public static UtilsWS wsClient;

    public static String clientName = "";

    public static ControllerConfig controllerConfig;
    public static ControllerPlayerSelection controllerPlayerSelection;
    public static ControllerWait controllerWait;
    public static ControllerCountdown controllerCountdown;
    public static ControllerGame controllerGame;
    public static ControllerGameOutcome controllerGameOutcome;

    public static void main(String[] args) {
        launch(args);
    }    

    @Override
    public void start(Stage stage) throws Exception {
        final int windowWidth = 900;
        final int windowHeight = 600;

        UtilsViews.parentContainer.setStyle("-fx-font: 14 arial;");
        UtilsViews.addView(getClass(), "ViewConfig", "/assets/viewConfig.fxml");
        UtilsViews.addView(getClass(), "ViewPlayerSelection", "/assets/viewPlayerSelection.fxml");
        UtilsViews.addView(getClass(), "ViewWait", "/assets/viewWait.fxml");
        UtilsViews.addView(getClass(), "ViewCountdown", "/assets/viewCountdown.fxml");
        UtilsViews.addView(getClass(), "ViewGame", "/assets/viewGame.fxml");
        UtilsViews.addView(getClass(), "ViewGameOutcome", "/assets/viewGameOutcome.fxml");

        controllerConfig = (ControllerConfig) UtilsViews.getController("ViewConfig");
        controllerPlayerSelection = (ControllerPlayerSelection) UtilsViews.getController("ViewPlayerSelection");
        controllerWait = (ControllerWait) UtilsViews.getController("ViewWait");
        controllerCountdown = (ControllerCountdown) UtilsViews.getController("ViewCountdown");
        controllerGame = (ControllerGame) UtilsViews.getController("ViewGame");
        controllerGameOutcome = (ControllerGameOutcome) UtilsViews.getController("ViewGameOutcome");

        Scene scene = new Scene(UtilsViews.parentContainer);
        
        stage.setScene(scene);
        stage.onCloseRequestProperty(); // Call close method when closing window
        stage.setTitle("Connect 4 - Client");
        stage.setMinWidth(windowWidth);
        stage.setMinHeight(windowHeight);
        // stage.setResizable(false);
        stage.show();

        // Cerrar correctamente el cliente al cerrar ventana
        stage.setOnCloseRequest(event -> {
            closeClient();
        });

        // Add icon only if not Mac
        if (!System.getProperty("os.name").contains("Mac")) {
            Image icon = new Image("file:/icons/icon.png");
            stage.getIcons().add(icon);
        }
    }

    /***** Conexión al servidor (se llama en ViewConfig) *****/
    public static void connectToServer() {

        // Cambiar label en ViewConfig
        controllerConfig.labelMessage.setText("Connecting ...");
    
        pauseDuring(1500, () -> { // Give time to show connecting message ...

            // Obtiene la URI introducida en ViewConfig
            String protocol = controllerConfig.textFieldProtocol.getText();
            String host = controllerConfig.textFieldHost.getText();
            String port = controllerConfig.textFieldPort.getText();

            // Generar instancia de wsClient con la URI registrada
            UtilsWS.resetSharedInstance(); // Asegura que si falla un intento de conexión (URI incorrecta), luego puede hacer otro intento correcto
            wsClient = UtilsWS.getSharedInstance(protocol + "://" + host + ":" + port);
    
            // Realiza una acción dependiendo del mensaje devuelto por Server
            wsClient.onOpen((response) -> { Platform.runLater(() -> { wsOpen(response); }); });
            wsClient.onMessage((response) -> { Platform.runLater(() -> { wsMessage(response); }); });
            wsClient.onError((response) -> { Platform.runLater(() -> { wsError(response); }); });

            wsClient.connect(); // Hay que hacer la conexión después de definir los handler para mensaje y error, sino utiliza el error definido en UtilsWS
        });
    }

    public static void log(String message) {
        System.out.println(message);
    }

    /***** Detiene el programa durante X milisegundos, y luego realiza un Runnable *****/
    public static void pauseDuring(long milliseconds, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(milliseconds));
        pause.setOnFinished(event -> Platform.runLater(action));
        pause.play();
    }

    private static void wsOpen(String response) {
        Platform.runLater(()->{ 
            // Enviar al servidor el nombre del cliente
            clientName = controllerConfig.getUsername();
            JSONObject json = new JSONObject();
            json.put("type", "register");
            json.put("clientName", clientName);
            wsClient.safeSend(json.toString());
        });
    }

    /***** Realiza una acción cuando recibe un mensaje del servidor *****/
    private static void wsMessage(String response) {
        Platform.runLater(()->{ 

            // Si no es un JSON, sólo lo imprime por pantalla
            boolean isJson = response.charAt(0) == '{';
            if (!isJson) {
                System.out.println("Entro en wsMessage; pero no recibo un JSON, sino esto:");
                System.out.println(response);
                return;
            }

            // Convertir respuesta a JSONObject
            JSONObject msgObj = new JSONObject(response);

            // Comprobar tipo de respuesta
            String type = msgObj.getString("type");
            switch (type) {
                // Intento de registro fallido por nombre ocupado
                case "clientNameNotAvalible":
                    System.out.println("Entro en case clientNameNotAvalible. Tendré que probar otro nombre");
                    String nameAlreadyUsed = "Name is already used by other client. Try another one";
                    controllerConfig.labelMessage.setTextFill(Color.RED);
                    controllerConfig.labelMessage.setText(nameAlreadyUsed);
                    pauseDuring(1500, () -> {
                        controllerConfig.labelMessage.setText("");
                    });
                    break;
                case "confirmedRegister":
                    System.out.println("Entro en case confirmedRegister. Cambio a ViewPlayerSelection");
                    UtilsViews.setViewAnimating("ViewPlayerSelection");
                    controllerPlayerSelection.setClientName(clientName);
                    setViewPlayerSelection();
                    break;
                // Recibir lista actualizada de clientes
                case "clients":
                    System.out.println("Entro en case clients. Actualizo lista de clientes disponibles");
                    controllerPlayerSelection.updateListOfClients(msgObj);
                    break;
                // Recibir reto de otro cliente
                case "challenge":
                    System.out.println("Entro en case challenge. Se habilita overlay para decidir aceptar o rechazar");
                    controllerPlayerSelection.processChallenge(msgObj);
                    break;
                // Recibir rechazo de un reto
                case "refusedMatch":
                    System.out.println("Entro en case refusedMatch. Cambio a vista ViewPlayerSelection");
                    UtilsViews.setViewAnimating("ViewPlayerSelection");
                    break;
                // Cliente que envió reto lo cancela
                case "cancelledChallenge":
                    System.out.println("Entro en case cancelledChallenge.");
                    controllerPlayerSelection.cancelledChallenge(msgObj);
                    break;
                // Server empieza el countdown
                case "startCountdown":
                    System.out.println("Entro en case startCountdown. Cambio a vista ViewCountdown");
                    controllerCountdown.setPlayerLabels(
                        msgObj.getString("player_1"),
                        msgObj.getString("player_2")
                    );
                    int initialSeconds = msgObj.getInt("value");
                    controllerCountdown.updateLabelCountdown(initialSeconds);
                    UtilsViews.setViewAnimating("ViewCountdown");
                    break;
                // Server envía estado actual del countdown
                case "remainingCountdown":
                    int remainingSeconds = msgObj.getInt("value");
                    controllerCountdown.updateLabelCountdown(remainingSeconds);
                    System.out.println("Entro en case remainingCountdown, remainingSeconds=" + remainingSeconds);
                    break;
                // Countdown ha terminado, y ahora empieza la partida
                case "startGame":
                    System.out.println("Entro en case startGame. Cambio a vista ViewGame");
                    controllerGame.resetGameState();
                    UtilsViews.setViewAnimating("ViewGame");
                    int gameId = msgObj.getInt("game_id");
                    controllerGame.setCurrentGameId(gameId);
                    break;
                // Partida ha acabado. Info necesaria para siguiente vista
                case "gameOutcome":
                    log("Entro en case gameOutcome. Cambio a vista ViewGameOutcome");
                    controllerGameOutcome.updateLabelGameOutcome(msgObj);
                    UtilsViews.setViewAnimating("ViewGameOutcome");
                    break;
                // Server manda orden con la info a dibujar
                case "drawOrder":
                    // System.out.println("orden de dibujar");
                    controllerGame.updateBoardPos(msgObj.getDouble("board_pos_x"),
                                                  msgObj.getDouble("board_pos_y"));
                    
                    controllerGame.updateDragChipsPos(msgObj.getDouble("red_chip_dragg_x"),
                                                    msgObj.getDouble("red_chip_dragg_y"),
                                                    msgObj.getDouble("yellow_chip_dragg_x"),
                                                    msgObj.getDouble("yellow_chip_dragg_y"));
                    controllerGame.draw(msgObj.getDouble("pos_x_1"), 
                                        msgObj.getDouble("pos_y_1"),
                                        msgObj.getDouble("pos_x_2"), 
                                        msgObj.getDouble("pos_y_2"));
                    controllerGame.updateCurrentChip(msgObj.getString("current_chip"));
                    controllerGame.updatePossibleMoves(msgObj.getString("possible_moves"));
                    controllerGame.updateGrid(msgObj.getJSONArray("grid"));
                    controllerGame.updateAnimChip(msgObj.getString("animChip"));
                    controllerGame.updateWinner(msgObj.getString("winner"));
                    controllerGame.updateWinnerLine(msgObj.getString("winner_line"));
                    controllerGame.updateCurrentPlayer(msgObj.getString("currentPlayerStr"));
                    break;
            }
        });
    }

    /***** Realiza una acción cuando hay un error de red o protocolo (p.ej. desconexión) *****/
    private static void wsError(String response) {

        System.out.println("Estoy en wsError");
        String connectionRefused = "S’ha refusat la connexió";
        if (response.indexOf(connectionRefused) != -1) {
            controllerConfig.labelMessage.setTextFill(Color.RED);
            controllerConfig.labelMessage.setText(connectionRefused);
            pauseDuring(1500, () -> {
                controllerConfig.labelMessage.setText("");
            });
        }
    }

    /***** Cierra el cliente *****/
    public static void closeClient() {
        System.out.println("Cerrando aplicación...");
    
        // Cierra el WebSocket si está abierto
        if (wsClient != null) {
            wsClient.forceExit();
        }

        Platform.exit();
        System.exit(0);
    }

    public static void sendChallenge(String challengedPlayer) {
        // Enviar al servidor el nombre del cliente
        JSONObject json = new JSONObject();
        json.put("type", "challenge");
        json.put("clientName", clientName);
        json.put("challengedClientName", challengedPlayer);
        wsClient.safeSend(json.toString());

        // Cambiar a la pantalla de espera
        controllerWait.updateChallengedName(challengedPlayer);
        UtilsViews.setViewAnimating("ViewWait");
    }

    public static void sendStartMatch(String challenger) {
        System.out.println("Entro en sendStartMatch");
        JSONObject matchJson = new JSONObject();
        matchJson.put("type", "startMatch");
        matchJson.put("player_1", challenger);
        matchJson.put("player_2", clientName);
        wsClient.safeSend(matchJson.toString());
    }

    public static void sendRefusedMatch(String challenger) {
        System.out.println("Entro en sendRefusedMatch");
        JSONObject refusedMatchJson = new JSONObject();
        refusedMatchJson.put("type", "refusedMatch");
        refusedMatchJson.put("challenger", challenger);
        wsClient.safeSend(refusedMatchJson.toString());
    }

    public static void sendCancelledChallenge(String challenged) {
        System.out.print("Entro en sendCancelledChallenge...");
        JSONObject cancelledChallengeJson = new JSONObject();
        cancelledChallengeJson.put("type", "cancelledChallenge");
        cancelledChallengeJson.put("challenged", challenged);
        wsClient.safeSend(cancelledChallengeJson.toString());

        System.out.println("Cambio a vista ViewPlayerSelection");
        UtilsViews.setViewAnimating("ViewPlayerSelection");
    }

    public static void sendPlayerMousePosInfo(String player, double x, double y, boolean dragging) {
        // System.out.println(clientName + " se mueve");
        JSONObject playerMouseInfo = new JSONObject();
        playerMouseInfo.put("type", "playerMouseInfo");
        playerMouseInfo.put("player", clientName);
        playerMouseInfo.put("pos_x", x);
        playerMouseInfo.put("pos_y", y);
        playerMouseInfo.put("dragging", dragging);
        playerMouseInfo.put("game_id", controllerGame.game_id);
        wsClient.safeSend(playerMouseInfo.toString());
    }

    public static void setViewPlayerSelection() {
        controllerPlayerSelection.hideChallengedOverlayIfShown();
        UtilsViews.setViewAnimating("ViewPlayerSelection");
    }

    public static void sendAvaliblePlayerMessage() {
        JSONObject avaliblePlayerJson = new JSONObject();
        avaliblePlayerJson.put("type", "avaliblePlayer");
        avaliblePlayerJson.put("clientName", clientName);
        wsClient.safeSend(avaliblePlayerJson.toString());
    }
}
