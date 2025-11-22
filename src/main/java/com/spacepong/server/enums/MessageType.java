package com.spacepong.server.enums;

/* 
 * All these keys, type definitions and fields, are documented in the document "API protocols"
 * 
 * https://github.com/Sabrina-rgb9/ScapePongAppRasberry/wiki/API-protocols
 * 
*/
public interface MessageType {
    // KEYS
    String K_TYPE = "type";
    String X = "x";
    String Y = "y";
    String P1 = "p1";
    String P2 = "p2";

    // FIELDS
    String F_CLIENT_NAME = "clientName";
    String F_DIRECTION = "direction";
    String F_UP = "up";
    String F_DOWN = "down";
    String F_REASON = "reason";
    String F_PLAYER_1 = "player1";
    String F_PLAYER_2 = "player2";
    String F_REMAINING_COUNTDOWN = "remainingCountdown";
    String F_WINNER = "winner";
    String F_LOSER = "LOSER";
    String F_CONFIG_MESSAGE = "configMessage";
    String F_BALL = "ball";
    String F_PADDLES = "paddles";
    String F_SCORE = "score";
    String F_TIMESTAMP = "timestamp";


    // SERVER INPUT
    String T_REGISTER = "register";
    String T_MOVE = "move";
    String T_EXIT = "exit";
    String T_REQUEST_CONFIGURATION = "requestConfiguration";

    // SERVER OUTPUT
    String T_ACCEPT_REGISTER = "acceptRegister";
    String T_DENY_REGISTER = "denyRegister";
    String T_START_GAME = "startGame";
    String T_START_COUNTDOWN = "startCountdown";
    String T_REMAINING_COUNTDOWN = "remainingCountdown";
    String T_END_COUNTDOWN = "endCountdown";
    String T_GAME_STATE = "gameState";
    String T_GAME_OUTCOME = "gameOutcome";
    String T_CONFIGURATION = "configuration";
}