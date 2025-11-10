package com.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;

import com.server.GameMatch.Game;

/**
 * Servidor WebSocket amb routing simple de missatges, sense REPL.
 *
 * El servidor arrenca, registra un shutdown hook i es queda a l'espera
 * fins que el procés rep un senyal de terminació (SIGINT, SIGTERM).
 *
 * Missatges suportats:
 *  - bounce: eco del missatge a l’emissor
 *  - broadcast: envia a tots excepte l’emissor
 *  - private: envia a un destinatari pel seu nom
 *  - clients: llista de clients connectats
 *  - error / confirmation: missatges de control
 */
public class Main extends WebSocketServer {

    /** Port per defecte on escolta el servidor. */
    public static final int DEFAULT_PORT = 3000;

    /** Registro de clientes */
    public static ClientRegistry clients;

    /***** Registro de partidas *****/
    public static List<GameMatch> gameMatches;
    public int game_id = 0;

    // Claus JSON
    private static final String K_TYPE = "type";
    private static final String K_MESSAGE = "message";
    private static final String K_ORIGIN = "origin";
    private static final String K_DESTINATION = "destination";
    private static final String K_ID = "id";
    private static final String K_LIST = "list";
    private static final String K_CLIENT_NAME = "clientName";

    // Tipus de missatge
    private static final String T_REGISTER = "register";
    private static final String T_AVALIBLE_PLAYER = "avaliblePlayer";
    private static final String T_BOUNCE = "bounce";
    private static final String T_BROADCAST = "broadcast";
    private static final String T_PRIVATE = "private";
    private static final String T_CLIENTS = "clients";
    private static final String T_ERROR = "error";
    private static final String T_CONFIRMATION = "confirmation";
    private static final String T_CHALLENGE = "challenge";
    private static final String T_START_MATCH = "startMatch";
    private static final String T_REFUSED_MATCH = "refusedMatch";
    private static final String T_CANCELLED_CHALLENGE = "cancelledChallenge";
    private static final String T_PLAYER_MOUSE_INFO = "playerMouseInfo";
    private static final String T_KOTLIN_ADD_CHIP = "kotlinAddChip";
    private static final String T_START_COUNTDOWN = "startCountdown";
    private static final String T_REMAINING_COUNTDOWN = "remainingCountdown";
    private static final String T_GAME_OUTCOME = "gameOutcome";

    /**
     * Crea un servidor WebSocket que escolta a l'adreça indicada.
     *
     * @param address adreça i port d'escolta del servidor
     */
    public Main(InetSocketAddress address) {
        super(address);
        clients = new ClientRegistry();
        gameMatches = new ArrayList<>();
    }

    /**
     * Crea un objecte JSON amb el camp type inicialitzat.
     *
     * @param type valor per a type
     * @return instància de JSONObject amb el tipus establert
     */
    private static JSONObject msg(String type) {
        return new JSONObject().put(K_TYPE, type);
    }

    /**
     * Afegeix clau-valor al JSONObject si el valor no és null.
     *
     * @param o objecte JSON destí
     * @param k clau
     * @param v valor (ignorat si és null)
     */
    private static void put(JSONObject o, String k, Object v) {
        if (v != null) o.put(k, v);
    }

    /**
     * Envia de forma segura un payload i, si el socket no està connectat,
     * el neteja del registre.
     *
     * @param to socket destinatari
     * @param payload cadena JSON a enviar
     */
    private static void sendSafe(WebSocket to, String payload) {
        if (to == null) return;
        try {
            to.send(payload);
        } catch (WebsocketNotConnectedException e) {
            String name = clients.cleanupDisconnected(to);
            log("Client desconectado durante sendSafe() -> " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Envia la llista actualitzada de clients a tots els clients connectats.
     */
    private void sendClientsListToAll() {
        JSONArray list = clients.currentAvaliblePlayersNames();
        for (Map.Entry<WebSocket, String> e : clients.snapshot().entrySet()) {
            JSONObject rst = msg(T_CLIENTS);
            put(rst, K_ID, e.getValue());
            put(rst, K_LIST, list);
            sendSafe(e.getKey(), rst.toString());
        }
    }

    // Envía la info a los clientes al acabar la partida, para que pasen de vista
    public static void sendGameOutcome(int id) {
        System.out.print("Entro en sendGameOutcome...");

        GameMatch gm = null;

        for (GameMatch gamem : gameMatches) {
            if(gamem.id_game == id) {
                gm = gamem;
            }
        }

        Game game = gm.game;
        
        String winnerName;
        if (game.winner != null) {
            winnerName = game.winner.name;
        } else {
            winnerName = "";
        }
        System.out.print("winnerName=" + winnerName + ", isDraw=" + game.isDraw + "...");

        JSONObject payload = new JSONObject();
        payload.put("type", T_GAME_OUTCOME);
        payload.put("winnerName", winnerName);
        payload.put("isDraw", game.isDraw);

        sendSafe(
            clients.socketByName(game.player1.name),
            payload.toString()
        );
        sendSafe(
            clients.socketByName(game.player2.name),
            payload.toString()
        );

        gm.stop();
        System.out.println("La partida ha sido detenida.");
    }

    public static void sendUpdateOrder(int id) {
        JSONObject objeto = new JSONObject();
        objeto.put("type", "drawOrder");
        GameMatch gameMatch = null;
        for (GameMatch gm : gameMatches) {
            if(gm.id_game == id) {
                gameMatch = gm;
            }
        }
        
        Game game = gameMatch.game;
        String player1 = game.player1.name;
        String player2 = game.player2.name;

        // Posicion de los jugadores
        objeto.put("pos_x_1", game.player1.x);
        objeto.put("pos_y_1", game.player1.y);
        objeto.put("pos_x_2", game.player2.x);
        objeto.put("pos_y_2", game.player2.y);

        // Board serializada
        int[][] grid = game.board.grid;
        JSONArray chipGridPositions = new JSONArray();
        // grid[2][3] = 1;
        // grid[1][1] = 2;
        for(int i = 0; i < grid.length; i++) {
            for(int j = 0; j < grid[0].length; j++) {
                if(grid[i][j] != 0) {
                    chipGridPositions.put(i + " " + j + " " + grid[i][j]);
                }
            }
        }
        objeto.put("grid", chipGridPositions);
        objeto.put("board_pos_x", game.board.x);
        objeto.put("board_pos_y", game.board.y);

        // Draggable chips
        objeto.put("red_chip_dragg_x", game.draggableChips_red_x);
        objeto.put("red_chip_dragg_y", game.draggableChips_red_y);
        objeto.put("yellow_chip_dragg_x", game.draggableChips_yellow_x);
        objeto.put("yellow_chip_dragg_y", game.draggableChips_yellow_y);

        // Current chip draggeada
        if(game.currentChip != null) {
            objeto.put("current_chip", game.currentChip.x + " " + game.currentChip.y + " " + game.currentChip.player);
            // Possible moves 
            objeto.put("possible_moves", game.possibleMoves);
        } else {
            objeto.put("current_chip", "none");
            objeto.put("possible_moves", "none");
        }

        // Falling chip animation status
        if(gameMatch.animChip != null) {
            objeto.put("animChip", gameMatch.animating + " " + gameMatch.animX + " " + gameMatch.animY + " " + gameMatch.animChip.player);
        } else {
            objeto.put("animChip", "none");
        }

        // Mandando str con el jugador actual
        if(game.currentPlayer == 1) {
            objeto.put("currentPlayerStr", game.players.get(0).name);
        } else {
            objeto.put("currentPlayerStr", game.players.get(1).name);
        }
        
        // Ganador y línea ganadora
        if(game.winner != null) {
            objeto.put("winner", game.winner.name);
            objeto.put("winner_line", gameMatch.winner_start_x + " " + gameMatch.winner_end_x + " " +
                                            gameMatch.winner_start_y + " " + gameMatch.winner_end_y);
        } else {
            objeto.put("winner", "none");
            objeto.put("winner_line", "none");
        }

        objeto.put("turn", game.currentPlayer);
        


        sendSafe(clients.socketByName(player1), objeto.toString());
        sendSafe(clients.socketByName(player2), objeto.toString());

    }

    // ----------------- WebSocketServer overrides -----------------

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        log("Nuevo cliente conectado. Debería entrar en case T_REGISTER");
    }

    /** Elimina el client del registre i notifica la llista actualitzada. */
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        log("Cliente desconectado -> " + clients.nameBySocket(conn));
        clients.remove(conn);
        sendClientsListToAll();
    }

    /***** Procesa el mensaje recibido y actúa según el tipo de mensaje. *****/
    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            // Obtener el JSON
            JSONObject json = new JSONObject(message);
            String type = json.getString("type");
            if (!type.equals(T_PLAYER_MOUSE_INFO)) {
                log("Mensaje recibido del cliente -> " + message);
            }

            switch (type) {
                // Si es un registro de cliente
                case T_REGISTER:
                    log("Entro en case T_REGISTER.");
                    String clientName = json.getString(K_CLIENT_NAME);

                    if (clients.nameExists(clientName)) {
                        JSONObject payloadClientNameNotAvalible = new JSONObject();
                        payloadClientNameNotAvalible.put("type", "clientNameNotAvalible");
                        sendSafe(conn, payloadClientNameNotAvalible.toString());

                        log("Sigo dentro de case T_REGISTER. Cliente NO registrado, nombre ya está ocupado. Mensaje rechazando registro enviado");
                    } else {
                        clients.add(conn, clientName);
                        clients.addClientToAvaliblePlayers(conn, clientName);

                        JSONObject payloadConfirmedRegister = new JSONObject();
                        payloadConfirmedRegister.put("type", "confirmedRegister");
                        sendSafe(conn, payloadConfirmedRegister.toString());

                        log("Sigo dentro de case T_REGISTER. Cliente registrado -> " + clientName + ", mensaje de confirmación enviado");

                        // Enviar nuevo JSON con los clientes actuales a todo el mundo
                        sendClientsListToAll();
                    }
                    
                    break;

                // Si un cliente notifica que pasa a estar disponible
                case T_AVALIBLE_PLAYER:
                    log("Entro en case T_AVALIBLE_PLAYER");
                    String newAvaliblePlayer = json.getString("clientName");
                    clients.addClientToAvaliblePlayers(
                        clients.socketByName(newAvaliblePlayer),
                        newAvaliblePlayer
                    );
                    sendClientsListToAll();
                    break;
                
                // Si es un reto de un cliente a otro
                case T_CHALLENGE:
                    String challenger = json.getString("clientName");
                    String challengedPlayer = json.getString("challengedClientName");
                    log(String.format("Entro en case T_CHALLENGE. Cliente '%s' ha retado a '%s", challenger, challengedPlayer));

                    // Saca a ambos jugadores de la lista de disponibles
                    removeTwoPlayersFromAvalible(challenger, challengedPlayer);
                    sendClientsListToAll();

                    // Preparar mensaje y enviar sólo a cliente retado
                    String payload = new JSONObject()
                        .put("type", "challenge")
                        .put("challenger", challenger)
                        .toString();
                    sendSafe(clients.socketByName(challengedPlayer), payload);
                    break;

                // Si un cliente acepta una partida
                case T_START_MATCH:
                    String player_1 = json.getString("player_1");
                    String player_2 = json.getString("player_2");
                    log(String.format("Entro en case T_START_MATCH. Jugarán %s VS %s", player_1, player_2));

                    gameMatches.removeIf(gm ->
                        gm.game.player1.name.equals(player_1) ||
                        gm.game.player2.name.equals(player_1) ||
                        gm.game.player1.name.equals(player_2) ||
                        gm.game.player2.name.equals(player_2)
                    );
                                       
                    // Mandar players a vista Countdown
                    int startSeconds = 3;

                    JSONObject payloadStartCountdown = new JSONObject();
                    payloadStartCountdown.put("type", T_START_COUNTDOWN);
                    payloadStartCountdown.put("player_1", player_1);
                    payloadStartCountdown.put("player_2", player_2);
                    payloadStartCountdown.put("value", startSeconds);
                    sendSafe(clients.socketByName(player_1), payloadStartCountdown.toString());
                    sendSafe(clients.socketByName(player_2), payloadStartCountdown.toString());

                    // Pongo en marcha el Countdown
                    Countdown countdown = new Countdown(startSeconds);
                    log("Sigo dentro de case T_START_MATCH. He creado el objeto Countdown");
                    
                    countdown.setOnTick((remaining) -> {
                        log("Sigo dentro de case T_START_MATCH. Entro en countdown.setOnTick con remaining=" + remaining);
                        JSONObject msg = new JSONObject()
                            .put("type", T_REMAINING_COUNTDOWN)
                            .put("value", remaining);
                        sendSafe(clients.socketByName(player_1), msg.toString());
                        sendSafe(clients.socketByName(player_2), msg.toString());
                    });

                    countdown.setOnFinished(() -> {
                        // Pongo en marcha la partida
                        
                        GameMatch gameMatch = new GameMatch(game_id, player_1, player_2);
                        log("Sigo dentro de case T_START_MATCH. He creado el GameMatch con game_id=" + game_id);
                        
                        gameMatches.add(gameMatch);
                        

                        // Avisar a los clientes de que empieza la partida
                        JSONObject payloadConfirmedGame = new JSONObject();
                        payloadConfirmedGame.put("type", "startGame");
                        payloadConfirmedGame.put("game_id", game_id);
                        payloadConfirmedGame.put("player_1", player_1);
                        payloadConfirmedGame.put("player_2", player_2);
                        sendSafe(clients.socketByName(player_1), payloadConfirmedGame.toString());
                        sendSafe(clients.socketByName(player_2), payloadConfirmedGame.toString());
                        game_id += 1;
                    });

                    countdown.startCountdown();

                    break;

                // Si un cliente rechaza una partida
                case T_REFUSED_MATCH:
                    log("Entro en case T_REFUSED_MATCH");
                    String refusedMatchChallenger = json.getString("challenger");

                    addTwoPlayersToAvalible(clients.nameBySocket(conn), refusedMatchChallenger);
                    sendClientsListToAll();

                    // Enviar payload
                    JSONObject payloadRefusedGame = new JSONObject();
                    payloadRefusedGame.put("type", T_REFUSED_MATCH);
                    sendSafe(clients.socketByName(refusedMatchChallenger), payloadRefusedGame.toString());
                    break;

                case T_CANCELLED_CHALLENGE:
                    log("Entro en case T_CANCELLED_CHALLENGE");
                    String challenged = json.getString("challenged");

                    addTwoPlayersToAvalible(clients.nameBySocket(conn), challenged);
                    sendClientsListToAll();

                    JSONObject payloadCancelledChallenge = new JSONObject();
                    payloadCancelledChallenge.put("type", T_CANCELLED_CHALLENGE);
                    sendSafe(clients.socketByName(challenged), payloadCancelledChallenge.toString());
                    break;
                
                case T_PLAYER_MOUSE_INFO:
                    int game_id = json.getInt("game_id");
                    // log("Entro en case T_PLAYER_MOUSE_INFO");

                    String player1 = json.getString("player");
                    double pos_x = json.getDouble("pos_x");
                    double pos_y = json.getDouble("pos_y");
                    boolean dragging = json.getBoolean("dragging");

                    for(GameMatch gm : gameMatches) {
                        if (gm.id_game == game_id) {
                            gm.updatePlayerMousePos(player1, pos_x, pos_y);
                            gm.updatePlayerMouseState(player1, dragging);
                        }

                    }
                     
                    break;
                
                case T_KOTLIN_ADD_CHIP:
                    
                    int col = json.getInt("message");
                    String videojugador = json.getString("clientName");
                    System.out.println("\n\n\n\n\n\n\n\n\n\n\n\nMensaje recibido de kotlin, añadir chip\n\n\n\n\n\n\n\n\n\n\n"+col);
                    for(GameMatch gm : gameMatches) {
                            if (gm.game.player1.name.equals(videojugador) ) {
                                gm.game.board.addChip(1, col);
                            } else if (gm.game.player2.name.equals(videojugador)) {
                                gm.game.board.addChip(2, col);
                            }
                        }

                    break;
                

                default:
                    log("Entro en default (tipo de mensaje no controlado). Mensaje=" + message);
            }

        } catch (Exception e) {
            conn.send(new JSONObject()
                .put("type", "error")
                .put("message", "Invalid JSON")
                .toString()
            );
            e.printStackTrace();
        }
    }

    /** Log d'error global o de socket concret. */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        conn.send("error");
    }

    

    /** Arrencada: log i configuració del timeout de connexió perduda. */
    @Override
    public void onStart() {
        log("Servidor WebSocket encendido en el puerto: " + getPort() + ". Pulsa Ctrl+C para pararlo.");
    }

    /**
     * Punt d'entrada: arrenca el servidor al port per defecte i espera senyals.
     *
     * @param args arguments de línia d'ordres (no utilitzats)
     */
    public static void main(String[] args) {
        Main server = new Main(new InetSocketAddress(DEFAULT_PORT));
        server.start();
    }

    public static void log(String message) {
        System.out.println(message);
    }

    private void removeTwoPlayersFromAvalible(String player1, String player2) {
        clients.removeClientFromAvaliblePlayers(clients.socketByName(player1));
        clients.removeClientFromAvaliblePlayers(clients.socketByName(player2));
    }

    private void addTwoPlayersToAvalible(String player1, String player2) {
        clients.addClientToAvaliblePlayers(clients.socketByName(player1), player1);
        clients.addClientToAvaliblePlayers(clients.socketByName(player2), player2);
    }
}
