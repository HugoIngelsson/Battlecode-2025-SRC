package robot5;

import battlecode.common.*;

public class Splasher extends Unit {
    boolean found_area = false;
    MapLocation target = null;
    Direction[] dirs = {Direction.NORTHEAST, Direction.SOUTHEAST, Direction.NORTHWEST, Direction.SOUTHWEST};
    public Splasher(RobotController rc) throws GameActionException {
        super(rc);
    }

    void play() throws GameActionException {
        if(target == null){
            int x = rng.nextInt(rc.getMapWidth());
            int y = rng.nextInt(rc.getMapHeight());
            target = new MapLocation(x, y);
        }
        Direction bestDirMove = considerMoves();

        if(rc.canMove(bestDirMove)) {
            rc.move(bestDirMove);
        }
        int maxP = 0;
        Direction bestDirSpalsh = dirs[0];
        for (Direction dir : dirs) {
            int mt = 0;
            for (MapInfo mi : rc.senseNearbyMapInfos(rc.getLocation().add(dir))) {
                if (mi.getPaint() == PaintType.EMPTY) {
                    mt++;
                }
            }
            if (mt > maxP) {
                maxP = mt;
                bestDirSpalsh = dir;
            }
        }
        if (maxP > 6 && rc.getPaint() > 49) {
            if(rc.canAttack(rc.getLocation().add(bestDirSpalsh))) {
                rc.attack(rc.getLocation().add(bestDirSpalsh));
            }
        }

    }

    @Override
    void initTurn() throws GameActionException {
        super.initTurn();
    }

    @Override
    void endTurn() throws GameActionException {
        super.endTurn();
    }

    Direction considerMoves() throws GameActionException {
        int maxVal = Integer.MIN_VALUE;
        Direction ret = Direction.CENTER;

        for (Direction d : RobotPlayer.directions) {
            int numEnemyTowers = 0;
            int numEnemyMoppers = 0;
            int numAllies = 0;
            MapLocation dest = rc.getLocation().add(d);
            if (rc.canMove(d)) {
                for (int i=nearRobots.length-1; i>=0; i--) {
                    if (nearRobots[i].team == rc.getTeam()) {
                        if (nearRobots[i].getLocation().distanceSquaredTo(dest) <= 10)
                            numAllies++;
                    } else if (nearRobots[i].getType() == UnitType.MOPPER) {
                        if (nearRobots[i].getLocation().distanceSquaredTo(dest) <= 4)
                            numEnemyMoppers++;
                    } else if (nearRobots[i].getType() != UnitType.SOLDIER &&
                            nearRobots[i].getType() != UnitType.SPLASHER) {
                        if (nearRobots[i].getLocation().distanceSquaredTo(dest) <= 3)
                            numEnemyTowers++;
                        else if (nearRobots[i].getType() == UnitType.LEVEL_ONE_DEFENSE_TOWER &&
                                nearRobots[i].getLocation().distanceSquaredTo(dest) <= 20) {
                            numEnemyTowers += 2;
                        }
                    }
                }

                MapInfo destInfo = rc.senseMapInfo(dest);
                int val = dest.distanceSquaredTo(target) * -3;
                if (!destInfo.getPaint().isAlly()) {
                    if (destInfo.getPaint() == PaintType.EMPTY) {
                        if (!rc.isActionReady())
                            val -= 5;
                    } else {
                        val -= 10;
                    }
                }

                if (d != Direction.CENTER) {
                    for (int i = 6; i >= 0; i--) {
                        if (rc.getLocation().equals(lastLocations[i])) {
                            val -= 20;
                            break;
                        }
                    }
                }

                val += numAllies - numEnemyMoppers - numEnemyTowers * 3;
                if (val > maxVal) {
                    maxVal = val;
                    ret = d;
                }
            }
        }

        return ret;
    }

}
