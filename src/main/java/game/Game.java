package game;

import java.io.PrintWriter;
import java.util.Objects;

public class Game {
    private String board;
    private PrintWriter player1;
    private String player1username;
    private PrintWriter player2;
    private String player2username;
    private int turn;
    private int player1score = 0;
    private int player2score = 0;
    private boolean finished = false;

    public Game(String board, PrintWriter player1, String player1username, PrintWriter player2, String player2username, int turn) {
        super();
        this.board = board;
        this.player1 = player1;
        this.player1username = player1username;
        this.player2 = player2;
        this.player2username = player2username;
        this.turn = turn;
    }

    public PrintWriter getPlayer1() {return player1;}
    public void setPlayer1(PrintWriter player1) {
        this.player1 = player1;
    }

    public String getPlayer1username() {return player1username;}
    public void setPlayer1username(String player1username) {
        this.player1username = player1username;
    }

    public PrintWriter getPlayer2() {return player2;}
    public void setPlayer2(PrintWriter player2) {
        this.player2 = player2;
    }

    public String getPlayer2username() {return player2username;}
    public void setPlayer2username(String player2username) {
        this.player2username = player2username;
    }

    public int getTurn() {return turn;}
    public void setTurn(int turn) {
        this.turn = turn;
    }

    public void changeTurn() {
        if (turn == 0) {
            player2.println("YOUR_TURN");
            turn = 1;
        } else {
            player1.println("YOUR_TURN");
            turn = 0;
        }

    }

    public void boardUpdate(String username, String board) {
        if (username.equalsIgnoreCase(player1username)) {
            player2.println("BOARD " + board);
        } else {
            player1.println("BOARD " + board);
        }
    }

    public void setScore(String username, int score) {
        if (player1username.equalsIgnoreCase(username)) {
            player1score = score;
            player2.println("SCORE_CHANGE " + score);
        } else {
            player2score = score;
            player1.println("SCORE_CHANGE " + score);
        }
    }

    public void gameOver() {
        if (player1score > player2score) {
            player1.println("GAME_OVER " + player1username);
            player2.println("GAME_OVER " + player1username);
        } else if (player2score > player1score) {
            player1.println("GAME_OVER " + player2username);
            player2.println("GAME_OVER " + player2username);
        } else {
            player1.println("GAME_OVER X");
            player2.println("GAME_OVER X");
        }
        this.finished = true;
    }

    public void gameOverDisc() {
        if (this.finished) return;
        player1.println("GAME_OVER D");
        player2.println("GAME_OVER D");
    }

    @Override
    public int hashCode() {
        return Objects.hash(player1username, player2username);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Game other = (Game) obj;
        return Objects.equals(player1username, other.player1username)
                && Objects.equals(player2username, other.player2username);
    }

}

