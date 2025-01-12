package juno;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.UnitType;

public class PaintTower extends Tower {
    int nextSpawn = 1;
    public PaintTower(RobotController rc) throws GameActionException {
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

        if (rc.getType() == UnitType.LEVEL_ONE_PAINT_TOWER && rc.getMoney() >= 5000) {
            rc.upgradeTower(rc.getLocation());
        }
        else if (rc.getType() == UnitType.LEVEL_TWO_PAINT_TOWER && rc.getMoney() >= 7500) {
            rc.upgradeTower(rc.getLocation());
        }
        else if (nextSpawn == 0 && rc.getPaint() >= 100 &&
                (rc.getMoney() >= 1030 || (rc.getRoundNum() < 5 && rc.getMoney() >= 300))) {
            for (int i=11; i>=0; i--) {
                if (rc.canBuildRobot(UnitType.MOPPER, spawnPlaces[i])) {
                    rc.buildRobot(UnitType.MOPPER, spawnPlaces[i]);
                    break;
                }
            }
            nextSpawn++;
        }
        else if (nextSpawn == 1 && rc.getPaint() >= 200 &&
                (rc.getMoney() >= 1030 || (rc.getRoundNum() < 5 && rc.getMoney() >= 250))) {
            for (int i=11; i>=0; i--) {
                if (rc.canBuildRobot(UnitType.SOLDIER, spawnPlaces[i])) {
                    rc.buildRobot(UnitType.SOLDIER, spawnPlaces[i]);
                    break;
                }
            }
            if(Math.random() > 0.7 && rc.getRoundNum() > 100 * rc.getMapWidth() / 20) {
                nextSpawn = 3;
            } else {
                nextSpawn = 0;
            }
        }
        else if (nextSpawn == 3  && rc.getPaint() >= 300 && rc.getMoney() >= 250) {
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
