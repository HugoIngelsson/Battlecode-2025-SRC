package robot1;

import battlecode.common.*;

public abstract class Unit extends Robot {

    public Unit(RobotController rc) throws GameActionException {
        super(rc);
    }

    abstract void play() throws GameActionException;
}
