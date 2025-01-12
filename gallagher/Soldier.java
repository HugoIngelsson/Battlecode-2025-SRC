package gallagher;

import battlecode.common.*;

public class Soldier extends Unit {
    private static final int MAX_PAINT = 200;

    MapInfo[] attackPos;
    int building = -51;
    public Soldier(RobotController rc) throws GameActionException {
        super(rc);
    }

    void play() throws GameActionException {
        closestRuin = getClosestRuin();

        if (rc.getRoundNum() - building < 50){
            closestRuin = null;
        }

        rc.setIndicatorDot(lastPainTower, 0, 0, 255);
        if (rc.getPaint() < Math.sqrt(rc.getLocation().distanceSquaredTo(lastPainTower)) / 2) {
            target = lastPainTower;
            targetIsRuin = false;

            if (rc.getLocation().distanceSquaredTo(target) < 2) {
                RobotInfo tower = rc.senseRobotAtLocation(target);
                if (tower != null) {
                    if (tower.getPaintAmount() < 20 && rc.senseMapInfo(rc.getLocation()).getPaint().isAlly()){
                        target = rc.getLocation();
                    } else if (rc.isActionReady()) {
                        rc.transferPaint(target, -1 * Math.min(MAX_PAINT*2/3 - rc.getPaint(), tower.getPaintAmount()));
                    }
                } else {
                    if (semiLastPaintTower != null) {
                        lastPainTower = semiLastPaintTower;
                        semiLastPaintTower = null;
                    } else lastPainTower = home;

                    target = lastPainTower;
                }
            }
        }
        else if (closestRuin != null) {
            target = closestRuin;
            targetIsRuin = true;

            ruinPattern = decodeMarkings(target);
            if (ruinPattern == -1) {
                hasRuinPattern = false;
                ruinPattern = GameConstants.PAINT_TOWER_PATTERN;
            } else {
                hasRuinPattern = true;
            }
        }
        else if (!blockNewTarget) { // roam randomly
            int x = rng.nextInt(rc.getMapWidth());
            int y = rng.nextInt(rc.getMapHeight());
            target = new MapLocation(x, y);
            blockNewTarget = true;
            targetIsRuin = false;
        }

        if (targetIsRuin && rc.isMovementReady() &&
                rc.getLocation().distanceSquaredTo(target) == 1) {
            if (rc.senseMapInfo(target.add(Direction.NORTH)).getMark() == PaintType.EMPTY) {

                if (rng.nextInt(100) < 75) {
                    markRuin(target, UnitType.LEVEL_ONE_MONEY_TOWER);
                    System.out.println("Marked a ruin for a money tower");
                } else {
                    markRuin(target, UnitType.LEVEL_ONE_PAINT_TOWER);
                    System.out.println("Marked a ruin for a paint tower");
                }
            }
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

        attackPos = rc.senseNearbyMapInfos(9);
        if (rc.isActionReady() &&
                rc.getPaint() >= Math.max(Math.sqrt(rc.getLocation().distanceSquaredTo(lastPainTower)) / 2, 10)) {
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
            }
        }

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
        } catch (GameActionException e) {
            if(e.getMessage().contains("limit number of towers")){
                building = rc.getRoundNum();
            }
        }
    }

    MapLocation findBestPaintLoc() throws GameActionException {
        MapLocation ret = null;
        int bestVal = Integer.MIN_VALUE;

        for (MapInfo m : attackPos) {
            if (!m.isPassable()) {
                RobotInfo robotAt = rc.senseRobotAtLocation(m.getMapLocation());
                if (m.hasRuin() && robotAt != null && robotAt.getTeam() != rc.getTeam()) {
                    return m.getMapLocation();
                } else continue;
            }

            rc.setIndicatorDot(m.getMapLocation(), 0, 255, 0);
            MapLocation loc = m.getMapLocation();
            if (m.getPaint() == PaintType.EMPTY) {
                int val = -5 * loc.distanceSquaredTo(target);
                val -= loc.distanceSquaredTo(home);
                if (Math.abs(loc.x - target.x) <= 3 &&
                        Math.abs(loc.y - target.y) <= 3) {
                    val += 200;
                }
                if (loc.equals(rc.getLocation()))
                    val += 135;

                if (val > bestVal) {
                    bestVal = val;
                    ret = loc;
                }
            } else if (m.getPaint() == PaintType.ALLY_PRIMARY || m.getPaint() == PaintType.ALLY_SECONDARY) {
                if (closestRuin != null && hasRuinPattern &&
                        Math.abs(loc.x - closestRuin.x) <= 2 &&
                        Math.abs(loc.y - closestRuin.y) <= 2) {

                    int relX = loc.x - closestRuin.x + 2;
                    int relY = loc.y - closestRuin.y + 2;

                    boolean secondary = (ruinPattern & (1 << (relX + 5*relY))) > 0;

                    if ((m.getPaint() == PaintType.ALLY_SECONDARY) ^ secondary) {
                        if (200 > bestVal) {
                            bestVal = 200;
                            ret = loc;
                        }
                    }
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
                } else if (rc.getPaint() < 20) {
                    val += 50;
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

    MapLocation getClosestRuin() throws GameActionException {
        int minDist = Integer.MAX_VALUE;
        MapLocation ret = null;

        for (MapLocation m : nearRuins) {
            RobotInfo ri = rc.senseRobotAtLocation(m);
            if (ri == null && minDist > m.distanceSquaredTo(rc.getLocation())) {
                minDist = m.distanceSquaredTo(rc.getLocation());
                ret = m;
            }
            else if (ri != null && ri.getTeam() == rc.getTeam()) {
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
