package zenith;

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
        indicateKnown();

        rc.attack(null);
        target = bestAttackTarget();
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
            MapLocation spawnLoc = nextSpawnLocation(UnitType.MOPPER);
            if (spawnLoc != null)
                rc.buildRobot(UnitType.MOPPER, spawnLoc);

            nextSpawn = chooseNextSpawntype();
        }
        if (nextSpawn == 1 && rc.getPaint() >= 200 &&
                (rc.getMoney() >= 1030 && rc.getRoundNum() > 30 || rc.getRoundNum() <= 2 && rc.getMoney() >= 250)) {
            MapLocation spawnLoc = nextSpawnLocation(UnitType.SOLDIER);
            if (spawnLoc != null)
                rc.buildRobot(UnitType.SOLDIER, spawnLoc);

            nextSpawn = chooseNextSpawntype();
        }
        else if (nextSpawn == 3 && rc.getPaint() >= 300 && rc.getMoney() >= 1030) {
            MapLocation spawnLoc = nextSpawnLocation(UnitType.SPLASHER);
            if (spawnLoc != null)
                rc.buildRobot(UnitType.SPLASHER, spawnLoc);

            nextSpawn = chooseNextSpawntype();
        }
    }

    @Override
    void endTurn() throws GameActionException {
        super.endTurn();
    }
}
