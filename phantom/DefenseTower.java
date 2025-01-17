package phantom;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.UnitType;

public class DefenseTower extends Tower {
    int nextSpawn = 1;

    public DefenseTower(RobotController rc) throws GameActionException {
        super(rc);
        this.AOE_DMG = rc.getType().aoeAttackStrength;
        this.TARGET_DMG = rc.getType().attackStrength;
        this.ATTACK_RANGE_SQ = rc.getType().actionRadiusSquared;
    }

    void play() throws GameActionException {
        rc.attack(null);
        MapLocation target = bestAttackTarget();
        if (target != null) {
            rc.attack(target);
        }

        if (rc.getType() == UnitType.LEVEL_ONE_DEFENSE_TOWER && rc.getMoney() >= 5500) {
            rc.upgradeTower(rc.getLocation());
            AOE_DMG += 5;
            TARGET_DMG += 10;
        }
        else if (rc.getType() == UnitType.LEVEL_TWO_DEFENSE_TOWER && rc.getMoney() >= 8000) {
            rc.upgradeTower(rc.getLocation());
            AOE_DMG += 10;
            TARGET_DMG += 10;
        }
        if (nextSpawn == 0 && rc.getPaint() >= 100 &&
                (rc.getMoney() >= 1030 && rc.getRoundNum() > 30  || rc.getRoundNum() == 1 && rc.getMoney() >= 300)) {
            for (int i=11; i>=0; i--) {
                if (rc.canBuildRobot(UnitType.MOPPER, spawnPlaces[i])) {
                    rc.buildRobot(UnitType.MOPPER, spawnPlaces[i]);
                    break;
                }
            }
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
