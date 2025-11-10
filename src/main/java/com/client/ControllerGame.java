package com.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.json.JSONArray;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ControllerGame implements Initializable {

    // -----------------------------
    // Game state
    // -----------------------------
    private static String name = Main.clientName;
    private static int playerNum;
    private static boolean dragging;

    public int game_id;

    public int[][] grid;
    public static ArrayList<Integer> possibleMovesList = new ArrayList<>();

    private static String winner = "none";
    private static double winnerStartX, winnerStartY, winnerEndX, winnerEndY;

    // -----------------------------
    // Board & chip data
    // -----------------------------
    public double CELL_SIZE = 80;
    public double boardX, boardY;

    public double redChipDragX, redChipDragY;
    public double yellowChipDragX, yellowChipDragY;

    public double currentChipX, currentChipY;
    public int currentChipPlayer;

    private double animChipX, animChipY;
    private boolean isAnimChip;
    private int animChipPlayer;

    public String currentPlayerName = "";

    // -----------------------------
    // Colors·
    // -----------------------------
    public final Color BOARD_COLOR = Color.rgb(20, 20, 63, 1);
    
    private final Color RED_COLOR = Color.rgb(157, 6, 6, 1);
    private final Color RED_COLOR_TRANSPARENT = Color.rgb(157, 6, 6, 0.5);
    private final Color BACKGROUND_COLOR = Color.rgb(162, 166, 185, 1);
    private final Color YELLOW_COLOR = Color.rgb(157, 146, 6, 1);
    private final Color YELLOW_COLOR_TRANSPARENT = Color.rgb(157, 146, 6, 0.5);


    // -----------------------------
    // JavaFX
    // -----------------------------
    @FXML
    private Canvas canvas;
    private GraphicsContext gc;

    // -----------------------------
    // Artists
    // -----------------------------
    private BoardArtist boardArtist;

    // =====================================================
    //                 Static Update Methods
    // =====================================================
    public void resetGameState() {
    // Limpia todas las estructuras de datos visuales
    grid = new int[6][7];
    
    
    // Limpia el canvas
    GraphicsContext gc = canvas.getGraphicsContext2D();
    gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
}
    public void setCurrentGameId(int gameId) {
        game_id = gameId;
    }
    public void updateWinnerLine(String line) {
        if (line.equals("none")) return;
        String[] parts = line.split(" ");
        winnerStartX = Double.parseDouble(parts[0]);
        winnerEndX = Double.parseDouble(parts[1]);
        winnerStartY = Double.parseDouble(parts[2]);
        winnerEndY = Double.parseDouble(parts[3]);
    }

    public void updateWinner(String winn) {
        winner = winn;
    }

    public void updateCurrentPlayer(String name) {
        currentPlayerName = name;
    }

    public void updateAnimChip(String animChipStatus) {
        if (animChipStatus.equals("none")) {
            isAnimChip = false;
            return;
        }

        String[] parts = animChipStatus.split(" ");
        isAnimChip = true;
        animChipX = Double.parseDouble(parts[1]);
        animChipY = Double.parseDouble(parts[2]);
        animChipPlayer = Integer.parseInt(parts[3]);
    }

    public void updateBoardPos(double x, double y) {
        boardX = x;
        boardY = y;
    }

    public void updateCurrentChip(String chip) {
        if (chip.equals("none")) {
            currentChipPlayer = 0;
            return;
        }

        String[] parts = chip.split(" ");
        currentChipX = Double.parseDouble(parts[0]);
        currentChipY = Double.parseDouble(parts[1]);
        currentChipPlayer = Integer.parseInt(parts[2]);
    }

    public void updateDragChipsPos(double redX, double redY, double yellowX, double yellowY) {
        redChipDragX = redX;
        redChipDragY = redY;
        yellowChipDragX = yellowX;
        yellowChipDragY = yellowY;
    }

    public void setPlayerInfo(String nombre, int numero, boolean draggea) {
        name = Main.clientName;
        playerNum = numero;
        dragging = draggea;
    }

    public void updatePossibleMoves(String possibleMoves) {
        possibleMovesList.clear();
        if (possibleMoves.equals("none")) return;

        for (int i = 0; i < possibleMoves.length(); i++) {
            possibleMovesList.add(Character.getNumericValue(possibleMoves.charAt(i)));
        }
    }

    public void updateGrid(JSONArray arr) {
        for (int i = 0; i < arr.length(); i++) {
            String[] parts = arr.get(i).toString().split(" ");
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);
            int value = Integer.parseInt(parts[2]);
            grid[row][col] = value;
        }
    }

    // =====================================================
    //                      Drawing
    // =====================================================

    public void draw(double posX1, double posY1, double posX2, double posY2) {
        gc.clearRect(0, 0, 10000, 10000);
        drawBackground();
        // Board
        boardArtist.draw();

        // Draggable chips
        drawChip(RED_COLOR, redChipDragX, redChipDragY, true);
        drawChip(YELLOW_COLOR, yellowChipDragX, yellowChipDragY, true);

        // Possible moves
        if (currentChipPlayer != 0) {
            boardArtist.drawPossibleMoves();
        }

        // Current chip
        if (currentChipPlayer != 0) {
            drawChip(currentChipPlayer == 1 ? RED_COLOR : YELLOW_COLOR, currentChipX,  currentChipY, true);
        }
        
        // Current Player str
        gc.setFill(BOARD_COLOR);
        
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 28d));
        gc.fillText(currentPlayerName + "'s turn", 625, 400, 300);

        // Animated chip
        if (isAnimChip) {
            drawChip(animChipPlayer == 1 ? RED_COLOR : YELLOW_COLOR, animChipX,  animChipY, true);
        }

        // Players
        gc.setFill(Color.rgb(255, 0, 0, 0.5));
        gc.fillOval(posX1 - 25, posY1 - 25, 50, 50);
        gc.setFill(Color.rgb(0, 255, 255, 0.5));
        gc.fillOval(posX2 - 25, posY2 - 25, 50, 50);

        // Winner line
        if (!winner.equals("none")) {
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(20);
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.strokeLine(winnerStartX, winnerStartY, winnerEndX, winnerEndY);
        }
    }
   
    // =====================================================
    //                   JavaFX Initialize
    // =====================================================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        gc = canvas.getGraphicsContext2D();
        boardArtist = new BoardArtist(6, 7);
        grid = new int[6][7];

        canvas.setOnMouseMoved(event -> 
            Main.sendPlayerMousePosInfo(Main.clientName, event.getSceneX(), event.getSceneY(), dragging)
        );

        canvas.setOnMouseDragged(event -> {
            dragging = true;
            Main.sendPlayerMousePosInfo(Main.clientName, event.getSceneX(), event.getSceneY(), dragging);
        });

        canvas.setOnMouseReleased(event -> {
            dragging = false;
            Main.sendPlayerMousePosInfo(Main.clientName, event.getSceneX(), event.getSceneY(), dragging);
        });
    }

    // =====================================================
    //                       Artists
    // =====================================================

    public interface Drawable {
        void draw();
    }

    public void drawChip(Color color, double x, double y, Boolean details) {
        // Drawing the chip itself
        gc.setFill(color);
        gc.fillOval(x, y, CELL_SIZE, CELL_SIZE);

        if(details){
            // Drawing lighter part
            gc.setFill(new Color(1,1,1,0.1));
            gc.fillOval(x + 5, y + 5, CELL_SIZE - 5*2, CELL_SIZE - 5*2);

        }
    }

    public void drawBackground() {
        double blockSize = 70;
        Color alternColor = Color.rgb(138,143,167, 0.5);
        Color curColor = alternColor;

        for(int i = 0; i < 50; i++) {
            double offset = 0;
            if (i%2 == 0) {
                offset = blockSize;
            }
            for (int j = 0; j < 50; j++) {
                gc.setFill(curColor);
                gc.fillRect(j * blockSize + offset, i * blockSize, blockSize, blockSize);

                if(curColor == BACKGROUND_COLOR) {
                    curColor = alternColor;
                } else {
                    curColor = BACKGROUND_COLOR;
                }
            }

            
        }

        

    }

    // -----------------------------
    // Board Artist
    // -----------------------------
    class BoardArtist implements Drawable {
        private final double width;
        private final double height;
        private final double margin = CELL_SIZE / 10;
        private final double spaceBetween = margin / 2;

        public BoardArtist(int rows, int cols) {
            width = cols * (CELL_SIZE + spaceBetween);
            height = rows * (CELL_SIZE + spaceBetween);
        }

        @Override
        public void draw() {
            gc.setFill(BOARD_COLOR);
            gc.fillRect(boardX, boardY, width + margin, height + spaceBetween);

            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[0].length; j++) {
                    double posX = boardX + margin / 2 + j * (CELL_SIZE + spaceBetween);
                    double posY = boardY + margin / 2 + i * (CELL_SIZE + spaceBetween);

                    // Filling background spots
                    drawChip(BACKGROUND_COLOR, posX, posY, false);

                    if (grid[i][j] == 1) {
                        drawChip(RED_COLOR, posX, posY, true);
                    } else if (grid[i][j] == 2) {
                        drawChip(YELLOW_COLOR, posX, posY, true);
                    }
                }
            }
        }

        public void drawPossibleMoves() {
            gc.setFill(currentChipPlayer == 1 ? RED_COLOR_TRANSPARENT : YELLOW_COLOR_TRANSPARENT);
            for (int col : getNotFullColumns()) {
                double posX = boardX + margin / 2 + col * (CELL_SIZE + spaceBetween);
                double posY = (boardY + margin / 2) - CELL_SIZE - spaceBetween;
                gc.fillOval(posX, posY, CELL_SIZE, CELL_SIZE);
            }
        }

        private ArrayList<Integer> getNotFullColumns() {
            ArrayList<Integer> notFullCols = new ArrayList<>();
            for (int col = 0; col < grid[0].length; col++) {
                if (grid[0][col] == 0) notFullCols.add(col);
            }
            return notFullCols;
        }
    }



}
