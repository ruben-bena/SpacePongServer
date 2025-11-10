package com.server;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

public class GameMatch implements Initializable{
    @FXML
    private Canvas canvas;
    public int WINDOW_WIDTH = 900;
    public int WINDOW_HEIGHT = 600;
    public int id_game = 0;
    private double mouse_x, mouse_y;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private GraphicsContext gc;
    public double canvas_width = WINDOW_WIDTH;
    public double canvas_height = WINDOW_HEIGHT;
    private double CELL_SIZE = 80;
    public Color boardColor = new Color(0,0,0.5,1);
    private Color redColor = new Color(0.5,0,0,1);
    private Color blueColor = new Color(0,0,0.5,1);
    private Color yellowColor = new Color(0.7,0.6,0.3,1);

    public Game game;
    private CanvasTimer timer;

    // Animación de caida
    public Chip animChip;
    public boolean animating = false;
    public int animCol = -1, animRow = -1;
    public double animX;
    public double animY;
    public double targetY;
    public double fallSpeed = 1300;
    public long lastRunNanos = 0;

    // Winner line
    public double winner_start_x, winner_start_y, winner_end_x, winner_end_y;

    
    @FXML
    private AnchorPane root;
    
    public void setMousePos(double x, double y) {
        mouse_x = x;
        mouse_y = y;
    }

    private void startGameLoop() {
        // Run at ~30 FPS => every 32 milliseconds
        scheduler.scheduleAtFixedRate(() -> {
            try {
                update();
            } catch (Exception e) {
                e.printStackTrace(); // Never let exceptions kill your game loop!
            }
        }, 0, 32, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    // Este es de servidor, hecho durante cambios
    public void updatePlayerMousePos(String player, double pos_x, double pos_y) {
        game.setPlayerPos(player, pos_x, pos_y);
    }

    public void updatePlayerMouseState(String player, boolean dragging) {
        if(game.player1.name.equals(player)) {
            game.player1.isDragging = dragging;
        } else {
            game.player2.isDragging = dragging;
        }
    }


    public void setNewWindowSize(int width, int height) {
        WINDOW_WIDTH = width;
        WINDOW_HEIGHT = height;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        gc = canvas.getGraphicsContext2D();
        canvas_width = canvas.getWidth();
        canvas_height = canvas.getHeight();

        timer.start();

        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());

        canvas.widthProperty().addListener(evt -> updateWindowSize());
        canvas.heightProperty().addListener(evt -> updateWindowSize());

        canvas.setOnMouseMoved(event -> {
            setMousePos(event.getSceneX(), event.getSceneY());

            update();
        });

        canvas.setOnMouseDragged(event -> {
            setMousePos(event.getSceneX(), event.getSceneY());
            game.setPlayersDragging(true);
        });

        canvas.setOnMouseReleased(event -> {
            game.setPlayersDragging(false);
        });


    }

    public GameMatch(int game_id, String player1, String player2) {
        id_game = game_id;
        game = new Game(player1, player2);
        startGameLoop();
    }

    public void updateWindowSize() {
        canvas_width = canvas.getWidth();
        canvas_height = canvas.getHeight();
    }

    

    public void update() {
        game.updateVisualLogics();
        Main.sendUpdateOrder(id_game);
        
        if(game.winner == null) {
            game.updateLogic();
        }
    }



    class Game {
        public int currentPlayer;
        public Board board;
        private final int rows = 6;
        private final int cols = 7;
        public Player player1, player2;
        public Player winner;
        public boolean isDraw = false;
        public ArrayList<Player> players = new ArrayList<Player>();
        public double draggableChips_red_x = 650;
        public double draggableChips_red_y = 100;
        public double draggableChips_yellow_x = 800;
        public double draggableChips_yellow_y = 100;
        private DraggableChip redDraggableChip;
        private DraggableChip yellowDraggableChip;
        private ArrayList<DraggableChip> draggableChips = new ArrayList<DraggableChip>();
        public Chip currentChip;
        public String possibleMoves;


        public Game(String player1, String player2) {
            currentPlayer = 1; // Red starts
            
            board = new Board(20, 85, rows, cols);
            this.player1 = new Player(player1,1, -50, -50);
            this.player2 = new Player(player2, 2, -150, -50);
            players.add(this.player1);
            players.add(this.player2);

            redDraggableChip = new DraggableChip(draggableChips_red_x, draggableChips_red_y, redColor, this.player1);
            yellowDraggableChip = new DraggableChip(draggableChips_yellow_x, draggableChips_yellow_y, yellowColor, this.player2);
            draggableChips.add(redDraggableChip);
            draggableChips.add(yellowDraggableChip);


        }

        public void setPlayerPos(String playerSending, double pos_x, double pos_y) {
            for (Player player : players) {
                if(player.name.equals(playerSending)) {
                    player.x = pos_x;
                    player.y = pos_y;

                    return;
                }
            }
        }

        public void switchPlayer() {
            if (currentPlayer == 1) {
                currentPlayer = 2;
            } else {
                currentPlayer = 1;
            }
        }

        public void setPlayersDragging(boolean isDragging) {
            player1.isDragging = isDragging;
            player2.isDragging = isDragging;
        }

        public void updatePlayerPositions() {
            player1.setPosition(mouse_x, mouse_y);
            player2.setPosition(mouse_x + 100, mouse_y);
        }

        public void updateVisualLogics()
        {
            long now = System.nanoTime();
            double dt;
            if (lastRunNanos == 0) {
            dt = 0; // primer frame
            } else {
            dt = (now - lastRunNanos) / 1_000_000_000.0;
            }
            lastRunNanos = now;

            if (animating && dt > 0) {
                animX = game.board.x + game.board.margin/2 + animCol * (CELL_SIZE + game.board.space_between);
            animY += fallSpeed * dt;
            if (animY >= targetY) {
                animY = targetY;
                board.addChip(animChip, animCol); // Se añade la ficha al terminar de caer
                animating = false;
                
            }

        }
        }

        

        public boolean isPlayerDraggingChip(Player player, DraggableChip draggableChip) {
            return draggableChip.isPlayerDraggingThisChip();
            
        }

        public void checkReleases() {
            if (game.currentChip != null) {
                ArrayList<Integer> possibleMoves = board.getNotFullColumns();
                int move = game.board.whatColIsChipDroppedIn(currentChip);
                if(players.get(game.currentChip.player - 1).isDragging) {
                    return;
                }
                for (Integer possibleMove : possibleMoves) {
                    if(possibleMove == move) {
                        board.doAddChipAnimation(currentChip, move); // La ficha se añade cuando se cambia animating = false en updateVisualLogics()
                        currentChip = null;
                        switchPlayer();
                       
                    }
                }
            }
        }

        public int checkWinner() {
            for(int row = 0; row < board.grid.length; row++) {
                for(int col = 0; col < board.grid[0].length; col++) {
                    // Check for the player assigned to the chip, then check for coincidences in a direction,
                    // if there are 4 straight, then that player wins.
                    int player = board.grid[row][col];
                    if(player != 0){
                        if(checkIsThereFourStraight(player, row, col)) {
                            System.out.println(player + " wins");
                            // Enviar la linea ganadora aquí
                            winner = players.get(player - 1);
                            return player;
                        }
                    }

                }
            }

            return -1;
        }

        private boolean checkDraw() {
            System.out.print("Entro en checkDraw()...");
            for (int i=0; i<rows; i++) {
                for (int j=0; j<cols; j++) {
                    if (game.board.grid[i][j] == 0) {
                        System.out.println("y retorno FALSE");
                        return false;
                    }
                }
            }
            System.out.println("y retorno TRUE");
            return true;
        }

        public boolean checkIsThereFourStraight(int player, int row, int col) {
            if (player == 0) return false;
            
            int coincidenceAmount = 0;
            // Checking to the right
            for (int i = 0; i < 4; i++) {
                
                //Checking if it's out of bounds
                if (col + i > game.board.grid[0].length - 1) break;
                if (game.board.grid[row][col + i] == player) coincidenceAmount++;
                if (coincidenceAmount >= 4) {
                    winner_start_x = board.getRowColPosition(row, col)[0]; 
                    winner_start_y = board.getRowColPosition(row, col)[1]; 
                    winner_end_x = board.getRowColPosition(row, col + i)[0];
                    winner_end_y = board.getRowColPosition(row, col)[1]; 
                    return true;
                }
            }

            coincidenceAmount = 0;
            // Checking down
            for (int i = 0; i < 4; i++) {
                if (i == 0) {
                    winner_start_x = board.getRowColPosition(row, col)[0]; 
                }
                //Checking if it's out of bounds
                if (row + i > game.board.grid.length - 1) break;
                if (game.board.grid[row + i][col] == player) coincidenceAmount++;
                if (coincidenceAmount >= 4) {
                    winner_start_x = board.getRowColPosition(row, col)[0]; 
                    winner_start_y = board.getRowColPosition(row, col)[1]; 
                    winner_end_x = board.getRowColPosition(row, col)[0];
                    winner_end_y = board.getRowColPosition(row + i, col)[1];
                    return true;
                }
            }

            // No need to check up or left

            coincidenceAmount = 0;
            // Checking down-right
            for (int i = 0; i < 4; i++) {
                if (i == 0) {
                    winner_start_x = board.getRowColPosition(row, col)[0]; 
                }
                //Checking if it's out of bounds
                if (row + i > game.board.grid.length - 1 || col + i > game.board.grid[0].length - 1) break;
                if (game.board.grid[row + i][col + i] == player) coincidenceAmount++;
                if (coincidenceAmount >= 4) {
                    winner_start_x = board.getRowColPosition(row, col)[0]; 
                    winner_start_y = board.getRowColPosition(row, col)[1]; 
                    winner_end_x = board.getRowColPosition(row + i, col + i)[0];
                    winner_end_y = board.getRowColPosition(row + i, col + i)[1];
                    return true;
                }
            }

            coincidenceAmount = 0;
            // Checking down-left
            for (int i = 0; i < 4; i++) {
                if (i == 0) {
                    winner_start_x = board.getRowColPosition(row, col)[0]; 
                }
                //Checking if it's out of bounds
                if (row + i > game.board.grid.length - 1 || col - i < 0) break;
                if (game.board.grid[row + i][col - i] == player) coincidenceAmount++;
                if (coincidenceAmount >= 4) {
                    winner_start_x = board.getRowColPosition(row, col)[0]; 
                    winner_start_y = board.getRowColPosition(row, col)[1]; 
                    winner_end_x = board.getRowColPosition(row + i, col - i)[0];
                    winner_end_y = board.getRowColPosition(row + i, col - i)[1]; 
                    return true;
                }
            }


            return false;
        }




        public void updateLogic() { // Yellow thing here
            if (isPlayerDraggingChip(player1, redDraggableChip) && currentPlayer == 1) {
                currentChip = player1.takeChip(redDraggableChip.offsetX, redDraggableChip.offsetY);
                redDraggableChip.setIsBeingDragged(true);
                possibleMoves = board.getPossibleMoves();
            } else {
                if(!player1.isDragging) {
                    checkReleases();
                }
                
                redDraggableChip.setIsBeingDragged(false);
                currentChip = null;
            }
            if (isPlayerDraggingChip(player2, yellowDraggableChip) && currentPlayer == 2 && !redDraggableChip.beingDragged) {
                currentChip = player2.takeChip(yellowDraggableChip.offsetX, yellowDraggableChip.offsetY);
                yellowDraggableChip.setIsBeingDragged(true);
                possibleMoves = board.getPossibleMoves();
                
            } else {
                if(!player2.isDragging) {
                    checkReleases();
                }
                if(!redDraggableChip.beingDragged) {
                    yellowDraggableChip.setIsBeingDragged(false);
                    currentChip = null;
                }
                
                // if(currentChip!=null){
                //     if (!player2.isDragging) {
                //         checkReleases();
                //     }
                    
                //     if(currentPlayer != 1) {
                //         yellowDraggableChip.setIsBeingDragged(false);
                //         currentChip = null;
                //     }
                // }
                
            }
            checkWinner();
            isDraw = checkDraw();
            if (winner != null || isDraw) {
                Main.sendGameOutcome(id_game);
            }
        }


        class DraggableChip {
            private double x, y;
            private double offsetX = 0, offsetY = 0;
            private double diameter = CELL_SIZE;
            private Color color;
            private boolean beingDragged = false;
            private Player assignedPlayer;

            public DraggableChip(double x, double y, Color color, Player player) {
                this.x = x;
                this.y = y;
                this.color = color;
                this.assignedPlayer = player;
            }

            public void setIsBeingDragged(boolean beingDragged) {
                this.beingDragged = beingDragged;
            }

            public boolean isPlayerDraggingThisChip() {
                if(!isPlayerDragging()) return false;

                if(beingDragged) return true;

                if (isPlayerOverChip() && isPlayerDragging()) {
                    setIsBeingDragged(true);
                    return true;
                }
                setIsBeingDragged(false);
                return false;
            }

            public boolean isPlayerDragging() {
                return assignedPlayer.isDragging;
            }

            public boolean isPlayerOverChip() {
                if (assignedPlayer.x >= x && assignedPlayer.x <= x + diameter &&
                    assignedPlayer.y >= y && assignedPlayer.y <= y + diameter) {
                        offsetX = assignedPlayer.x - x;
                        offsetY = assignedPlayer.y - y;
                        return true;
                    }
                return false;
            }

            
        }



    }

    class Player {
        private int playerNumber;
        public String name;
        public double x, y;
        private boolean isDragging = false;
        
        public Player(String playerName, int playerNumber, double x, double y) {
            this.name = playerName;
            this.playerNumber = playerNumber;
            this.x = x;
            this.y = y;
        }

        public void setPosition(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Chip takeChip(double offsetX, double offsetY) {
            return createChip(playerNumber, offsetX, offsetY);
        }
    }

    class Board{
        public int[][] grid; // 0 = empty, 1 = red, 2 = yellow
        public double x, y;

        private double margin = CELL_SIZE/10;
        private double space_between = margin/2;

        public Board(double x, double y, int rows, int cols) {
            grid = new int[rows][cols];
            this.x = x;
            this.y = y;

            // Testing zone
            // setGridToDemonstrateDraw();
            
        }

        private void setGridToDemonstrateDraw() {
            int[][] drawGrid = {
                {2, 2, 1, 0, 1, 2, 2},
                {1, 1, 1, 2, 1, 1, 1},
                {2, 2, 2, 1, 2, 2, 2},
                {2, 2, 2, 1, 2, 2, 2},
                {1, 1, 1, 2, 1, 1, 1},
                {1, 1, 1, 2, 1, 1, 1}
            };

            for (int i=0; i<drawGrid.length; i++) {
                for (int j=0; j<drawGrid[0].length; j++) {
                    grid[i][j] = drawGrid[i][j];
                }
            }
        }

        public double[] getRowColPosition(int row, int col) {
                double[] position = new double[2];
                double pos_x = 5000, pos_y = 5000;

                for(int i = 0; i < grid.length; i++) {
                                      // cols
                    for(int j = 0; j < grid[0].length; j++) {
                        if (i == row && j == col) {
                            pos_x = CELL_SIZE/2 + x + margin/2 + j * (CELL_SIZE + space_between);
                            pos_y = CELL_SIZE/2 + y + margin/2 + i * (CELL_SIZE + space_between);
                        }
                        
                    }
                }
                position[0] = pos_x;
                position[1] = pos_y;
                return position;
            }
        
        public void doAddChipAnimation(Chip chip, int col) {
                int row = findLowestEmptyRow(col);
                if (row < 0) return; // columna llena

                double cellCenterYTop = game.board.y + CELL_SIZE * 0.5;
                double startY = game.board.y - CELL_SIZE * 0.5; // ligeramente por encima
                double endY = cellCenterYTop + row * CELL_SIZE;

                // Se asigna currentChip a animChip ya que currentChip se vuelve null casi instantáneamente.
                animChip = game.currentChip;
                animCol = col;
                animRow = row;
                animY = startY;
                targetY = endY;
                animating = true;
                lastRunNanos = 0; // para que se calcule el primer dt
            }

        private int findLowestEmptyRow(int col) {
            for (int r = game.board.grid.length - 1; r >= 0; r--) {
                if (game.board.grid[r][col] == 0) return r;
            }
            return -1;
            }

        public boolean isChipIn(int row, int col, int player) {
                if (grid[row][col] == player) {
                    return true;
                }
                return false;
            }

        public String getPossibleMoves() {
            String result = "";
            for (int col = 0; col < grid[0].length; col++) {
                    if (grid[0][col] == 0) {
                        result += String.valueOf(col);
                    }
                }

            return result;

        }

        public ArrayList<Integer> getNotFullColumns() {
                ArrayList<Integer> notFullCols = new ArrayList<Integer>();
                for (int col = 0; col < grid[0].length; col++) {
                    if (grid[0][col] == 0) {
                        notFullCols.add(col);
                    }
                }

                return notFullCols;
            }

        public int whatColIsChipDroppedIn(Chip chip) {
                double colXStart = 5000, colXEnd = 5000, colYStart = 5000, colYEnd = 5000;
                colYStart = game.board.y - CELL_SIZE - game.board.space_between;
                colYEnd = game.board.y;

                for(int col = 0; col < grid[0].length; col++) {
                    colXStart = game.board.x + game.board.margin/2 + col * (CELL_SIZE + game.board.space_between);
                    colXEnd = game.board.x + CELL_SIZE + game.board.margin/2 + col * (CELL_SIZE + game.board.space_between);
                    if (game.players.get(chip.player - 1).x > colXStart && game.players.get(chip.player - 1).x < colXEnd 
                    && game.players.get(chip.player - 1).y > colYStart && game.players.get(chip.player - 1).y < colYEnd) {
                        return col;
                    }
                }
                return -1;
            }

        public void addChip(Chip chip, int col) {
            int row_to_add_in = 0;

            for(int i = grid.length - 1; i > 0; i--) {
                if(grid[i][col] == 0) {
                    row_to_add_in = i;
                    break;
                }
            }

            grid[row_to_add_in][col] = chip.player;
            animChip = null;
        }

        // This one is for the kotlin client, so I'll change the turn here
        public void addChip(int player, int col) {
            int row_to_add_in = 0;

            for(int i = grid.length - 1; i > 0; i--) {
                if(grid[i][col] == 0) {
                    row_to_add_in = i;
                    break;
                }
            }

            grid[row_to_add_in][col] = player;
            animChip = null;
            game.switchPlayer();
        }


    }

    class Chip {
        public int player; // 1 o 2
        public double x, y;

        public Chip(int player, double x, double y) {
            this.x = x;
            this.y = y;
            this.player = player;
        }

    }

    public Chip createChip(int player, double offsetX, double offsetY) {
        return new Chip(player, game.players.get(player - 1).x - offsetX, game.players.get(player - 1).y - offsetY);
    }

}
