package avenger;

import battlecode.common.*;

public class Mopper extends Unit {
    MapInfo[] mopLocations;
    public Mopper(RobotController rc) throws GameActionException {
        super(rc);
    }

    void play() throws GameActionException {
        closestRuin = getClosestRuin();

        if (rc.getPaint() < 20) {
            target = home; // return home
            targetIsRuin = false;
        }
        else if (!blockNewTarget) { // roam randomly
            int x = rng.nextInt(rc.getMapWidth());
            int y = rng.nextInt(rc.getMapHeight());
            target = new MapLocation(x, y);
            blockNewTarget = true;
            targetIsRuin = false;
        }

        if (blockNewTarget && rc.getLocation().distanceSquaredTo(target) < 15) {
            blockNewTarget = false;
        }

        if (rc.onTheMap(target))
            rc.setIndicatorDot(target, 255, 0, 0);

        if (rc.isMovementReady()) {
            Direction bestDir = considerMoves();
            rc.setIndicatorDot(rc.getLocation().add(bestDir), 255, 0, 0);

            if (bestDir != Direction.CENTER)
                rc.move(bestDir);
        }

        // where can we mop?
        mopLocations = rc.senseNearbyMapInfos(2);
        if (rc.isActionReady() && rc.getPaint() >= 10) {
            MapLocation paintLoc = findBestPaintLoc();

            if (paintLoc != null) {
                boolean secondary;
                if (closestRuin != null &&
                        Math.abs(paintLoc.x - closestRuin.x) <= 2 &&
                        Math.abs(paintLoc.y - closestRuin.y) <= 2) {
                    int relX = paintLoc.x - closestRuin.x + 2;
                    int relY = paintLoc.y - closestRuin.y + 2;

                    secondary = (ruinPattern & (1 << (relX + 5*relY))) > 0;
                } else {
                    secondary = (paintLoc.x + paintLoc.y) % 2 == 0;
                }

                rc.attack(paintLoc, secondary);
                try {
                    // sensing whether it's possible is just as expensive
                    // so no reason not to try
                    switch (ruinPattern) {
                        case GameConstants.PAINT_TOWER_PATTERN:
                            rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, target);
                            break;
                        case GameConstants.DEFENSE_TOWER_PATTERN:
                            rc.completeTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, target);
                            break;
                        case GameConstants.MONEY_TOWER_PATTERN:
                            rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, target);
                            break;
                    }
                } catch (GameActionException e) { ; }
            }
        }
    }

    MapLocation findBestPaintLoc() throws GameActionException {
        MapLocation ret = null;
        int bestVal = Integer.MIN_VALUE;

        for (MapInfo m : mopLocations) {
            if (!m.isPassable())
                continue;

            MapLocation loc = m.getMapLocation();
            if (m.getPaint() == PaintType.ENEMY_SECONDARY || m.getPaint() == PaintType.ENEMY_PRIMARY) {
                int val = -3 * loc.distanceSquaredTo(home);
                if (closestRuin != null && loc.distanceSquaredTo(closestRuin) <= 2) {
                    val += 200;
                }
                if (loc.equals(rc.getLocation()))
                    val += 135;

                if (val > bestVal) {
                    bestVal = val;
                    ret = loc;
                }
            }
        }

        return ret;
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
                        else if (nearRobots[i].getType() == UnitType.LEVEL_ONE_DEFENSE_TOWER && // FIX!! ONLY LEVEL 1 FOR NOW
                                nearRobots[i].getLocation().distanceSquaredTo(dest) <= 20) {
                            numEnemyTowers += 2;
                        }
                    }
                }

                boolean canMop = false;
                if (rc.isActionReady()) {
                    for (Direction e : RobotPlayer.directions) {
                        PaintType pt = rc.senseMapInfo(dest.add(e)).getPaint();
                        if (pt == PaintType.ENEMY_PRIMARY || pt == PaintType.ENEMY_SECONDARY) {
                            canMop = true;
                            break;
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
                        val -= 20;
                    }
                }
                if (canMop) val += 10;

                val += numAllies - numEnemyMoppers - numEnemyTowers * 3;
                if (val > maxVal) {
                    maxVal = val;
                    ret = d;
                }
            }
        }

        return ret;
    }

    MapLocation getClosestRuin() throws GameActionException {
        int minDist = Integer.MAX_VALUE;
        MapLocation ret = null;

        for (MapLocation m : nearRuins) {
            RobotInfo ri = rc.senseRobotAtLocation(m);
            if (ri == null && minDist > m.distanceSquaredTo(rc.getLocation())) {
                minDist = m.distanceSquaredTo(rc.getLocation());
                ret = m;
            }
        }

        return ret;
    }

    @Override
    void initTurn() throws GameActionException {
        super.initTurn();
    }

    @Override
    void endTurn() throws GameActionException {

    }
}
