/*
 * Copyright (c) 2020. Laurent Réveillère
 */

/*
package fr.ubx.poo.model.go;

import fr.ubx.poo.game.Direction;
import fr.ubx.poo.game.Position;
import fr.ubx.poo.model.Movable;
import fr.ubx.poo.model.go.GameObject;
import fr.ubx.poo.game.Game;

public class Monster extends GameObject implements Movable {

    private final boolean alive = true;
    Direction direction;

    public Monster(Game game, Position position) {
        super(game, position);
        this.direction = Direction.S;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public boolean canMove(Direction direction) {
        Position nextPos = direction.nextPosition(getPosition());
        //collision avec les bords
        if (nextPos.inside(game.getWorld().dimension)){
            return true;
        }
        return false;
    }

    @Override
    public void doMove(Direction direction) {
        Position nextPos = direction.nextPosition(getPosition());
        setPosition(nextPos);
    }

    public boolean isAlive() {
        return alive;
    }

}
*/

package fr.ubx.poo.model.decor;

import fr.ubx.poo.model.Movable;
import fr.ubx.poo.game.Direction;

public class Monster extends Decor implements Movable{
    @Override
    public String toString() {
        return "Monster";
    }
    @Override
    public boolean canMove(Direction direction){
        return true;
    }

    @Override
    public void doMove(Direction direction){

    }
}