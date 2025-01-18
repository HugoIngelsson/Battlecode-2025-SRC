package quartz;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.UnitType;

import java.util.Random;

public abstract class Robot {
    RobotController rc;
    MapLocation home;
    MapLocation lastPainTower = null;
    MapLocation semiLastPaintTower = null;
    Random rng;
    MapLocation lastEnemyTower = null;
    UnitType lastEnemyTowerType = null;


    boolean isLargeMap = false;

    public Robot(RobotController rc) throws GameActionException {
        this.rc = rc;
        rng = new Random(rc.getID());
        if (rc.getMapHeight() * rc.getMapWidth() >= 1000)
            isLargeMap = true;
    }

    abstract void turn1() throws GameActionException;

    abstract void play() throws GameActionException;

    void initTurn() throws GameActionException {
    }

    void endTurn() throws GameActionException {

    }
}
