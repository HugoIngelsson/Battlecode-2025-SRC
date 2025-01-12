package juno;

import battlecode.common.*;

public class Mopper extends Unit {
    private static final int MAX_PAINT = 100;

    MapInfo[] mopLocations;
    public Mopper(RobotController rc) throws GameActionException {
        super(rc);
    }

    void play() throws GameActionException {
        closestRuin = getClosestRuin();

        mopLocations = rc.senseNearbyMapInfos(4);
        MapLocation enemyPaint;
        if (rc.getPaint() < Math.sqrt(rc.getLocation().distanceSquaredTo(lastPainTower))) {
            if (lastPainTower == null) {
                lastPainTower = home;
            }

            target = lastPainTower;
            targetIsRuin = false;

            if (rc.getLocation().distanceSquaredTo(target) < 2) {
                RobotInfo tower = rc.senseRobotAtLocation(target);
                if (tower != null && rc.isActionReady()) {
                    rc.transferPaint(target, -1 * Math.min(MAX_PAINT*2/3 - rc.getPaint(), tower.getPaintAmount()));
                } else {
                    /*lastPainTower = (target == lastPainTower) ? semiLastPaintTower : lastPainTower;
                    semiLastPaintTower = null;
                    target = lastPainTower;*/

                    target = home;
                }
            }
        }
        else if ((enemyPaint = closePaint()) != null) {
            target = enemyPaint;
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
        if (rc.isActionReady()) {
            MapLocation paintLoc = findBestPaintLoc();
            if (paintLoc != null) {
                if (rc.isActionReady()) {
                    rc.attack(paintLoc);
                }
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

                if (rc.senseRobotAtLocation(m.getMapLocation()) != null) {
                    val += 500;
                }

                if (val > bestVal) {
                    bestVal = val;
                    ret = loc;
                }
            }

            RobotInfo ri = rc.senseRobotAtLocation(m.getMapLocation());
            if (rc.getPaint() > 30 && ri != null &&
                    ri.getPaintAmount() == 0 && ri.getTeam() == rc.getTeam()) {
                rc.transferPaint(m.getMapLocation(), 5);
                return null;
            }
        }

        return ret;
    }

    MapLocation closePaint() {
        MapLocation closest = null;
        int dist = Integer.MAX_VALUE;

        for (MapInfo m : mopLocations) {
            if (m.getPaint() == PaintType.ENEMY_PRIMARY || m.getPaint() == PaintType.ENEMY_SECONDARY) {
                if (m.getMapLocation().distanceSquaredTo(rc.getLocation()) < dist) {
                    dist = m.getMapLocation().distanceSquaredTo(rc.getLocation());
                    closest = m.getMapLocation();
                }
            }
        }

        return closest;
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
                        if (!rc.onTheMap(dest.add(e))) continue;

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
                        val -= 10;
                    } else {
                        if (!rc.isActionReady())
                            val -= 20;
                    }
                } else if (rc.getPaint() < 20) {
                    val += 50;
                }
                if (canMop) val += 20;

                if (d != Direction.CENTER) {
                    for (int i = 6; i >= 0; i--) {
                        if (rc.getLocation().equals(lastLocations[i])) {
                            val -= 10;
                            break;
                        }
                    }
                }

                val += 5 * numAllies - numEnemyMoppers - numEnemyTowers * 3;
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
            if (ri != null && ri.getTeam() == rc.getTeam()) {
                UnitType tp = ri.getType();
                if (tp == UnitType.LEVEL_ONE_PAINT_TOWER ||
                        tp == UnitType.LEVEL_THREE_PAINT_TOWER ||
                        tp == UnitType.LEVEL_TWO_PAINT_TOWER) {
                    if (ri.getLocation() != this.lastPainTower) {
                        this.semiLastPaintTower = this.lastPainTower;
                        this.lastPainTower = ri.getLocation();
                    }
                }
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
        super.endTurn();
    }
}
