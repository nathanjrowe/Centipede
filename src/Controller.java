/*
 * Author: Nathan J. Rowe
 * Controller Class
 * Sets up and controls the game
 */
//For Animation Timer
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import javafx.animation.AnimationTimer;
//For GUI elements
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
//For Game Elements
import java.util.ArrayList;
import javafx.scene.input.KeyCode;

public class Controller {
    //Game Display
    private final GamePanel game;
    private final Menu menu;
    //AnimationTimer centipedeSpawn;
    private boolean paused = false;
    private boolean death = false;
    //Animation timer
    AnimationTimer update;
    AnimationTimer respawn;
    //User Input & Sprite
    private KeyCode input = null;
    private String sInput = null;
    private final Ship ship;
    //Game Over Display
    Image gameOver = new Image("resources/images/gameover.png");
    Image restartButton = new Image("resources/images/restart.png");
    ImageView gameOverView = new ImageView(gameOver);
    ImageView restart = new ImageView(restartButton);
    VBox gameOverBox = new VBox(gameOverView, restart);
    ImageView controls;

/*
* ---------------------------------
*          Constructor
* ---------------------------------                 
*/
    public Controller(BorderPane root) {
        /*    Game Setup    */
        this.game = new GamePanel(80); //Make a new game
        root.setCenter(game); //Add game to root
        this.menu = new Menu(game);
        controls = menu.controls();
        root.setTop(menu);
        root.setBottom(controls);
        root.setAlignment(controls, Pos.CENTER);
        /*   Initialize User */
        this.ship = this.game.getShip();
        shipControls();  
        /*   Game Over Display Setup */
        gameOverBox.setAlignment(Pos.CENTER);
        restart.getStyleClass().add("restart");
        restart.setOnMouseEntered(e -> {
            restart.setOpacity(0.5);
            restart.setOnMouseExited(e2 -> {
                restart.setOpacity(1);
            });
        });
        restart.setOnMouseClicked(e -> {
            new Controller(root);
        });
        //Start game
        start(root);  
    }

/*
 * ---------------------------------
 *         User Controls
 * ---------------------------------
 */
    private void shipControls() {
        this.ship.setFocusTraversable(true);
        this.ship.setOnKeyPressed(e -> {
            input = e.getCode();
            if (input == KeyCode.LEFT || input == KeyCode.A) {
                sInput = "left";
            }
            if (input == KeyCode.RIGHT || input == KeyCode.D) {
                sInput = "right";
            }
            if(input == KeyCode.UP || input == KeyCode.W) {
                sInput = "up";
            }
            if(input == KeyCode.DOWN || input == KeyCode.S) {
                sInput = "down";
            }
            if(input == KeyCode.SPACE) {
                sInput = "space";
            }
            //Pause game
            if(input == KeyCode.ESCAPE && !paused) {
                this.paused = true;
                this.ship.setOnKeyPressed(null);
                pause();
                unpause();
                this.menu.paused(paused);
            }
            //Move ship on input
            this.ship.move(sInput);
            sInput = null;
        });
    }

    //Pause controls;
    private void pause() {
        ArrayList<Centipede> centipedes = game.getCentipedes();
        if(paused == true) {
            for(Centipede centipede : centipedes) {
                centipede.move(false);
            }
            respawn.stop();
            update.stop();
        }
        else {
            for(Centipede centipede : centipedes) {
                centipede.move(true);
            }
            respawn.start();
            update.start();
        }
        centipedes = null;
    }

    //Add controls to unpause the game
    private void unpause() {
        this.ship.setOnKeyPressed(e -> {
            input = e.getCode();
            if (input == KeyCode.ESCAPE) {
                paused = false;
                game.setOnKeyPressed(null);
                pause();
                shipControls();
                menu.paused(paused);
            }
        });
    }

    //Check if ship is out of lives
    private void checkDeath(BorderPane root) {
        if(this.ship.getLives() < 0) {
            death = true;
            update.stop();
            game.clear();
            root.setCenter(gameOverBox);
        }
    }

/*
 * ---------------------------------
 *          Game Start
 * ---------------------------------
 */
    //Method to start the game
    public void start(BorderPane root) {
        //Animation timer to update game
        //Checks for changes in game state
        update = new AnimationTimer() {
            //Variables to check for changes in game state
            private int currLives = ship.getLives();
            private int counter = 0;
            private long prev = 0;
            private long scoreCounter = 0;
            private Duration lastUpdate = Duration.of(0, ChronoUnit.NANOS);
            @Override
            public void handle(long now) {
                Duration nowDur = Duration.of(now, ChronoUnit.NANOS);
                if (nowDur.minus(lastUpdate).toMillis() > 25) {
                    lastUpdate = nowDur; 
                    //Check if score has changed
                    scoreCounter = game.getScore() - prev;
                    //Check if no lives
                    //Else, update lives
                    checkDeath(root);
                    if(death) {
                        this.stop();
                    }
                    else {
                        if(ship.getLives() < currLives) {
                            currLives = ship.getLives();
                            respawn.start();
                        }
                        //Add life
                        if(scoreCounter >= 12000) {
                                prev = game.getScore();
                                currLives = ship.getLives() + 1;
                                ship.setLives(currLives);
                        }
                        //Run continuous cleanup on board
                        //Update menu display
                        game.cleanUp();
                        menu.update();
                        //If no centipedes/ spawn a new centipede
                        if(game.getCentipedes().isEmpty()) {
                            game.spawnCentipede(0, 0, 12);
                        }
                        //Spawn Flea every 1200 frames
                        if(counter >= 1200) {
                            game.spawnFlea();
                            counter = 0;
                        }
                        //Increment counter
                        counter++;
                    }
                }            
            }
        };

        //Animation timer to respawn ship
        //Used when ship is hit
        respawn = new AnimationTimer() {
            private Duration lastUpdate = Duration.of(0, ChronoUnit.NANOS);
            private int count = 0;
            @Override
            public void handle(long now) {
                Duration nowDur = Duration.of(now, ChronoUnit.NANOS);
                if (nowDur.minus(lastUpdate).toMillis() > 1) {
                    //Respawn ship after 300 frames
                    if(count >= 300) {
                        game.phase(false);
                        count = 0;
                        this.stop();
                    }
                    count++;
                }
            }
        };
        //Start update timer
        update.start();
    }
}
