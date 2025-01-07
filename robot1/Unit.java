package robot1;

import battlecode.common.*;

public abstract class Unit extends Robot {
    MapLocation target;
    MapLocation[] nearRuins;
    MapInfo[] nearLocations;
    RobotInfo[] nearRobots;
    MapLocation closestRuin;

    public Unit(RobotController rc) throws GameActionException {
        super(rc);
    }

    void turn1() throws GameActionException {
        // find spawn tower
        MapLocation[] ruins = rc.senseNearbyRuins(-1);
        int minDist = 10000;
        for (MapLocation m : ruins) {
            if (m.distanceSquaredTo(rc.getLocation()) < minDist) {
                minDist = m.distanceSquaredTo(rc.getLocation());
                home = m;
            }
        }
    }

    void initTurn() throws GameActionException {
        nearRuins = rc.senseNearbyRuins(-1);
        nearLocations = rc.senseNearbyMapInfos();
        nearRobots = rc.senseNearbyRobots();
    }

    abstract void play() throws GameActionException;
}
