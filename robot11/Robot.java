package robot11;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Random;

public abstract class Robot {
    RobotController rc;
    MapLocation home;
    MapLocation lastPainTower = null;
    MapLocation semiLastPaintTower = null;
    Random rng;

    public Robot(RobotController rc) throws GameActionException {
        this.rc = rc;
        rng = new Random(rc.getID());
    }

    abstract void turn1() throws GameActionException;

    abstract void play() throws GameActionException;

    void initTurn() throws GameActionException {
    }

    void endTurn() throws GameActionException {

    }
}
