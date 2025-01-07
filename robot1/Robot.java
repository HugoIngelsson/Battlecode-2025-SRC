package robot1;

import battlecode.common.*;

public abstract class Robot {
    RobotController rc;
    MapLocation home;

    public Robot(RobotController rc) throws GameActionException {
        this.rc = rc;
    }

    abstract void turn1() throws GameActionException;

    abstract void play() throws GameActionException;

    void initTurn() throws GameActionException {
    }

    void endTurn() throws GameActionException {

    }
}
