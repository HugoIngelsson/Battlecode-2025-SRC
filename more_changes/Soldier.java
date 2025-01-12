package more_changes;

import battlecode.common.*;

public class Soldier extends Unit {
    private static final int MAX_PAINT = 200;

    MapInfo[] attackPos;
    int srpToFix = Integer.MAX_VALUE;
    boolean formingSRP = false;

    int lastOccupiedRuinTurn = -11;
    MapLocation lastOccupiedRuin = null;
    public Soldier(RobotController rc) throws GameActionException {
        super(rc);
    }

    void play() throws GameActionException {
        closestRuin = getClosestRuin();
        formingSRP = false;

        if (rc.getRoundNum() - building < 50){
            closestRuin = null;
        }

        rc.setIndicatorDot(lastPainTower, 0, 0, 255);
        if (rc.getPaint() < Math.max(Math.sqrt(rc.getLocation().distanceSquaredTo(lastPainTower)) / 2, 15)) {
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
        else if (((srpToFix = neededForSRP()) <= 8 ||
                rc.getRoundNum() < 50 && building > -51 && srpToFix <= 25) &&
                srpToFix > 0) {
            target = rc.getLocation();
            formingSRP = true;

            if (rc.senseMapInfo(target).getMark() == PaintType.EMPTY)
                rc.mark(rc.getLocation(), false);
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

                if (rng.nextInt(100) < 75 || rc.getRoundNum() < 30) {
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

        MapLocation greedyAttackLoc;
        if (rc.isActionReady() && (greedyAttackLoc = greedyAttack()) != null) {
            rc.attack(greedyAttackLoc);
        }

        if (rc.isMovementReady()) {
            Direction bestDir = considerMoves();
            rc.setIndicatorDot(rc.getLocation().add(bestDir), 255, 0, 0);

            if (bestDir != Direction.CENTER && !formingSRP)
                rc.move(bestDir);
        }

        attackPos = rc.senseNearbyMapInfos(9);
        if (rc.isActionReady() &&
                rc.getPaint() >= Math.max(Math.sqrt(rc.getLocation().distanceSquaredTo(lastPainTower)) / 2, 15)) {
            MapLocation paintLoc = findBestPaintLoc();

            if (paintLoc != null) {
                boolean secondary;
                if (closestRuin != null &&
                        Math.abs(paintLoc.x - closestRuin.x) <= 2 &&
                        Math.abs(paintLoc.y - closestRuin.y) <= 2) {
                    int relX = paintLoc.x - closestRuin.x + 2;
                    int relY = paintLoc.y - closestRuin.y + 2;

                    secondary = (ruinPattern & (1 << (relX + 5*relY))) > 0;
                } else if (formingSRP &&
                        Math.abs(paintLoc.x - target.x) <= 2 &&
                        Math.abs(paintLoc.y - target.y) <= 2) {
                    int relX = paintLoc.x - target.x + 2;
                    int relY = paintLoc.y - target.y + 2;

                    secondary = (GameConstants.RESOURCE_PATTERN & (1 << (relX + 5*relY))) > 0;
                } else {
                    secondary = (paintLoc.x + paintLoc.y) % 2 == 0;
                }

                rc.attack(paintLoc, secondary);
                if (formingSRP) {
                    try {
                        rc.completeResourcePattern(rc.getLocation());
                    }
                    catch (GameActionException e) { ; }
                }
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
                if (closestRuin != null && hasRuinPattern &&
                        (Math.abs(loc.x - closestRuin.x) > 2 ||
                        Math.abs(loc.y - closestRuin.y) > 2))
                    continue;

                int val = -5 * loc.distanceSquaredTo(target);
                val -= loc.distanceSquaredTo(home);
                if (Math.abs(loc.x - target.x) <= 2 &&
                        Math.abs(loc.y - target.y) <= 2) {
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
                } else if (formingSRP && Math.abs(loc.x - target.x) <= 2 &&
                        Math.abs(loc.y - target.y) <= 2) {
                    int relX = loc.x - target.x + 2;
                    int relY = loc.y - target.y + 2;

                    boolean secondary = (GameConstants.RESOURCE_PATTERN & (1 << (relX + 5*relY))) > 0;

                    if (!m.getPaint().isAlly()) {
                        if (2000 > bestVal) {
                            bestVal = 2000;
                            ret = loc;
                        }
                    }
                    else if (m.getPaint() == PaintType.ALLY_SECONDARY ^ secondary) {
                        if (1000 > bestVal) {
                            bestVal = 1000;
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

                for (int i = 6; i >= 0; i--) {
                    if (dest.equals(lastLocations[i])) {
                        val -= 20;
                        break;
                    }
                }

                val += numAllies - numEnemyMoppers;
                if (!rc.isActionReady())
                    val -= numEnemyTowers * 10;

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
            if (ri == null && minDist > m.distanceSquaredTo(rc.getLocation()) &&
                    (m != lastOccupiedRuin || lastOccupiedRuinTurn + 10 > rc.getRoundNum())) {
                if (rc.getLocation().distanceSquaredTo(m) > 2 && (
                        rc.canSenseRobotAtLocation(m.add(Direction.NORTH)) && rc.senseRobotAtLocation(m.add(Direction.NORTH)).getTeam() == rc.getTeam() ||
                        rc.canSenseRobotAtLocation(m.add(Direction.SOUTH)) && rc.senseRobotAtLocation(m.add(Direction.SOUTH)).getTeam() == rc.getTeam() ||
                        rc.canSenseRobotAtLocation(m.add(Direction.WEST)) && rc.senseRobotAtLocation(m.add(Direction.WEST)).getTeam() == rc.getTeam() ||
                        rc.canSenseRobotAtLocation(m.add(Direction.EAST)) && rc.senseRobotAtLocation(m.add(Direction.EAST)).getTeam() == rc.getTeam())
                ) {
                    lastOccupiedRuinTurn = rc.getRoundNum();
                    lastOccupiedRuin = m;
                } else {
                    minDist = m.distanceSquaredTo(rc.getLocation());
                    ret = m;
                }
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

    // returns the number of squares needed to change for SRP
    // will return INTEGER.MAX_VALUE if this would ruin another SRP (eventually)
    int neededForSRP() throws GameActionException {
        int x = rc.getLocation().x;
        int y = rc.getLocation().y;
        int cnt = 0;
        for (int i=-2; i<=2; i++) {
            for (int j=-2; j<=2; j++) {
                MapLocation loc = new MapLocation(x+i, y+j);
                if (!rc.onTheMap(loc)) return Integer.MAX_VALUE;

                MapInfo mi = rc.senseMapInfo(loc);
                if (mi.getPaint().isEnemy() || !mi.isPassable() ||
                    mi.getMark() != PaintType.EMPTY && (i != 0 && j != 0)) return Integer.MAX_VALUE;

                int relX = i + 2;
                int relY = j + 2;

                boolean secondary = (GameConstants.RESOURCE_PATTERN & (1 << (relX + 5*relY))) > 0;
                if (!mi.getPaint().isAlly() ||
                        mi.getPaint() == PaintType.ALLY_SECONDARY ^ secondary)
                    cnt++;
            }
        }

        return cnt;
    }

    MapLocation greedyAttack() {
        for (MapLocation m : nearRuins) {
            if (rc.canSenseRobotAtLocation(m) && rc.canAttack(m))
                return m;
        }

        return null;
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
