/*
 * Copyright (c) 2020. Laurent Réveillère
 */

package fr.ubx.poo.engine;

import fr.ubx.poo.game.Direction;
import fr.ubx.poo.view.sprite.Sprite;
import fr.ubx.poo.view.sprite.SpriteBomb;
import fr.ubx.poo.view.sprite.SpriteFactory;
import fr.ubx.poo.game.Game;
import fr.ubx.poo.game.Position;
import fr.ubx.poo.game.World;
import fr.ubx.poo.model.decor.DoorNextClosed;
import fr.ubx.poo.model.decor.DoorNextOpened;
import fr.ubx.poo.model.go.Bomb;
import fr.ubx.poo.model.go.Monster;
import fr.ubx.poo.model.go.character.Player;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public final class GameEngine {

    private static AnimationTimer gameLoop;
    private final String windowTitle;
    private final Game game;
    private final Player player;
    private List<Monster> monsters = new ArrayList<>();
    private final List<Sprite> sprites = new ArrayList<>();
    private List<Bomb> bombs = new ArrayList<>();
    private StatusBar statusBar;
    private Pane layer;
    private Input input;
    private Stage stage;
    private Sprite spritePlayer;
    private final List<Sprite> spritesBomb = new ArrayList<>();
    private final List<Sprite> spritesMonster = new ArrayList<>();
    private List<World> memoryWorld = new ArrayList<>(); //Permet de mettre en mémoires les mondes au second plan
    

    public GameEngine(final String windowTitle, Game game, final Stage stage) {
        this.windowTitle = windowTitle;
        this.game = game;
        this.player = game.getPlayer();
        this.monsters = game.getMonsters();
        initialize(stage, game);
        buildAndSetGameLoop();
    }

    private void initialize(Stage stage, Game game) {
        this.stage = stage;
        Group root = new Group();
        layer = new Pane();

        int height = game.getWorld().dimension.height;
        int width = game.getWorld().dimension.width;
        int sceneWidth = width * Sprite.size;
        int sceneHeight = height * Sprite.size;
        Scene scene = new Scene(root, sceneWidth, sceneHeight + StatusBar.height);
        scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());

        stage.setTitle(windowTitle);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        input = new Input(scene);
        root.getChildren().add(layer);
        statusBar = new StatusBar(root, sceneWidth, sceneHeight, game);
        // Create decor sprites
        game.getWorld().forEach( (pos,d) -> sprites.add(SpriteFactory.createDecor(layer, pos, d)));
        spritePlayer = SpriteFactory.createPlayer(layer, player);    

        for (Monster m : monsters){
            spritesMonster.add(SpriteFactory.createMonster(layer, m));
        }
    }

    protected final void buildAndSetGameLoop() {
        gameLoop = new AnimationTimer() {
            public void handle(long now) {
                // Check keyboard actions
                processInput(now);

                // Do actions
                update(now);

                // Graphic update
                render();
                statusBar.update(game);
            }
        };
    }

    private void processInput(long now) {
        if (input.isExit()) {
            gameLoop.stop();
            Platform.exit();
            System.exit(0);
        }
        if (input.isMoveDown()) {
            player.requestMove(Direction.S);
        }
        if (input.isMoveLeft()) {
            player.requestMove(Direction.W);
        }
        if (input.isMoveRight()) {
            player.requestMove(Direction.E);
        }
        if (input.isMoveUp()) {
            player.requestMove(Direction.N);
        }
        if (input.isBomb()) {
            if (player.getnbAvailable() > 0){
                player.setnbAvailable(player.getnbAvailable() - 1);
                Bomb bomb = new Bomb(game, player.getPosition(), now) ;
                bombs.add(bomb);
                spritesBomb.add(SpriteFactory.createBomb(layer, bomb)); 
            }
        }
        if (input.isKey()){
            //Si la position de la case où regarde le joueur est la porte alors on l'ouvre
            World world = game.getWorld();
            Position doorPosition = player.getDirection().nextPosition(player.getPosition());
            if (world.get(doorPosition) instanceof DoorNextClosed){
                if (player.getKey() > 0){
                    world.set(doorPosition, new DoorNextOpened());
                    player.useKey();
                }   
            }
        }
        input.clear();
    }

    private void showMessage(String msg, Color color) {
        Text waitingForKey = new Text(msg);
        waitingForKey.setTextAlignment(TextAlignment.CENTER);
        waitingForKey.setFont(new Font(60));
        waitingForKey.setFill(color);
        StackPane root = new StackPane();
        root.getChildren().add(waitingForKey);
        Scene scene = new Scene(root, 400, 200, Color.WHITE);
        stage.setTitle(windowTitle);
        stage.setScene(scene);
        input = new Input(scene);
        stage.show();
        new AnimationTimer() {
            public void handle(long now) {
                processInput(now);
            }
        }.start();
    }


    private void update(long now) {
        player.update(now);
        
        for (Monster monster : monsters){
            monster.update(now);
        }

        for(Bomb bomb : bombs){
            bomb.update(now);
        }

        if(game.getWorld().hasChanged()){
            sprites.forEach(Sprite::remove); 
            sprites.clear();
            game.getWorld().setChanged(false);
            game.getWorld().forEach( (pos,d) -> sprites.add(SpriteFactory.createDecor(layer, pos, d)));
        }
        if (game.isChanged()) {
            //met le niveau actuel dans la liste de mémoire (à l'indice level - 1)
            if (memoryWorld.size() <= game.getLevel()) 
                memoryWorld.add(game.getLevel() - 1, game.getWorld());
            
            //actualise le numero de level (le niveau où on va)
            if (game.isBacking()) game.setLevel(game.getLevel() - 1);
            else game.setLevel(game.getLevel() + 1);

            //retrait monstres
            monsters.clear();
            spritesMonster.forEach(Sprite::remove); 

            //retrait fenêtre courante
            stage.close();

            //chargement du nouveau monde 
            //vérifie s'il est en mémoire & le charge 
            if (memoryWorld.size() >= game.getLevel()){
                game.setWorld(memoryWorld.get(game.getLevel() - 1));
            }

            //sinon on va chercher le nouveau
            else game.loadWorldFromFile();
            
            //met à jour les monstres & positions du joueur
            game.changeLevel();

            initialize(stage, game);
            game.setChanged(false);
        }

        if (player.isAlive() == false) {
            gameLoop.stop();
            showMessage("Perdu!", Color.RED);
        }
        if (player.isWinner()) {
            gameLoop.stop();
            showMessage("Gagné", Color.BLUE);
        }        
    }

    private void render() {
        spritesBomb.forEach(Sprite::render);
        sprites.forEach(Sprite::render);
        // last rendering to have player in the foreground
        spritesMonster.forEach(Sprite::render);
        spritePlayer.render();
    }

    public void start() {
        gameLoop.start();
    }
}
