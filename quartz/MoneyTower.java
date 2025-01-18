package quartz;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.UnitType;

public class MoneyTower extends Tower {
    int nextSpawn = 1;
    public MoneyTower(RobotController rc) throws GameActionException {
        super(rc);
        this.AOE_DMG = 10;
        this.TARGET_DMG = 20;
        this.ATTACK_RANGE_SQ = 9;
    }

    void play() throws GameActionException {
        rc.attack(null);
        MapLocation target = bestAttackTarget();
        if (target != null) {
            rc.attack(target);
        }

        if (nextSpawn == 0 && rc.getPaint() >= 100 &&
                (rc.getMoney() >= 1030 && rc.getRoundNum() > 30  || rc.getRoundNum() == 1 && rc.getMoney() >= 300)) {
            for (int i=11; i>=0; i--) {
                if (rc.canBuildRobot(UnitType.MOPPER, spawnPlaces[i])) {
                    rc.buildRobot(UnitType.MOPPER, spawnPlaces[i]);
                    break;
                }
            }
            if (rc.getRoundNum() > 5)
                nextSpawn++;
        }
        if (nextSpawn == 1 && rc.getPaint() >= 200 &&
                (rc.getMoney() >= 1030 && rc.getRoundNum() > 30 || rc.getRoundNum() <= 2 && rc.getMoney() >= 250)) {
            for (int i=11; i>=0; i--) {
                if (rc.canBuildRobot(UnitType.SOLDIER, spawnPlaces[i])) {
                    rc.buildRobot(UnitType.SOLDIER, spawnPlaces[i]);
                    break;
                }
            }
            if (rc.getRoundNum() <= 2 && isLargeMap) {
                nextSpawn = 1;
            }
            else if (rc.getRoundNum() <= 2 ||
                    Math.random() > 0.6 && rc.getRoundNum() > 30 && rc.getPaint() >= 300){
                nextSpawn = 3;
            } else {
                nextSpawn = 0;
            }
        }
        else if (nextSpawn == 3 && rc.getPaint() >= 300 && rc.getMoney() >= 1030) {
            for (int i=11; i>=0; i--) {
                if (rc.canBuildRobot(UnitType.SPLASHER, spawnPlaces[i])) {
                    rc.buildRobot(UnitType.SPLASHER, spawnPlaces[i]);
                    break;
                }
            }
            nextSpawn = 0;
        }
    }

    @Override
    void endTurn() throws GameActionException {

    }
}
