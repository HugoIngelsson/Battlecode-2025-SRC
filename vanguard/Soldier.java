package vanguard;

import battlecode.common.*;

public class Soldier extends Unit {
    private static final int MAX_PAINT = 200;

    MapInfo[] attackPos;
    int srpToFix = Integer.MAX_VALUE;
    boolean formingSRP = false;
    int SRPSearch = -1;

    int lastOccupiedRuinTurn = -11;
    MapLocation lastOccupiedRuin = null;
    public Soldier(RobotController rc) throws GameActionException {
        super(rc);
    }

    void play() throws GameActionException {
        if (formingSRP)
            rc.setIndicatorString("SRPing");

        closestRuin = getClosestRuin();
        if (closestRuin != null) {
            lastRuinSeen = closestRuin;
            ruinTurn = rc.getRoundNum();

            if (formingSRP || rc.getRoundNum() - building < 50){
                closestRuin = null;
            }
        }

        rc.setIndicatorDot(lastPainTower, 0, 0, 255);
        if (rc.getPaint() < Math.max(Math.sqrt(rc.getLocation().distanceSquaredTo(lastPainTower)), 15)) {
            target = lastPainTower;
            targetIsRuin = false;
            formingSRP = false;

            if (!retreating) {
                storeMessage();

                if (formingSRP) {
                    saveSRPLoc = rc.getLocation();
                }
                if (closestRuin != null) {
                    saveRuinLoc = rc.getLocation();
                }
            }

            if (rc.getLocation().distanceSquaredTo(target) <= 2) {
                RobotInfo tower = rc.senseRobotAtLocation(target);
                if (tower != null) {
                    if (tower.getPaintAmount() < 20 && rc.senseMapInfo(rc.getLocation()).getPaint().isAlly()){
                        target = rc.getLocation();
                    } else if (rc.isActionReady() && tower.getPaintAmount() > 0) {
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
        else if (closestRuin != null && SRPSearch < 0) {
            target = closestRuin;
            targetIsRuin = true;

            ruinPattern = decodeMarkings(target);
            if (ruinPattern == -1) {
                hasRuinPattern = false;
                ruinPattern = GameConstants.MONEY_TOWER_PATTERN;
            } else {
                hasRuinPattern = true;
            }
            retreating = false;
        }
        else if (attackTarget != null && rc.getHealth() > 20) {
            target = attackTarget;
            formingSRP = false;
            retreating = false;
        }
        else if ((srpToFix = neededForSRP()) <= rc.getPaint() * 10 &&
                saveSRPLoc == null ||
                (rc.getLocation() == saveSRPLoc && srpToFix < Integer.MAX_VALUE)) {
            target = rc.getLocation();
            formingSRP = true;

            if (rc.senseMapInfo(target).getMark() == PaintType.EMPTY)
                rc.mark(rc.getLocation(), false);
            retreating = false;

        }
        else if (saveSRPLoc != null) { // go back to finish what you started
            target = saveSRPLoc;
            targetIsRuin = false;
        }
        else if (saveRuinLoc != null) {
            target = saveRuinLoc;
            targetIsRuin = false;
        }
        else if (directive != null) {
            target = directive;
        }
        else if (!blockNewTarget) { // roam randomly
            int x = rng.nextInt(rc.getMapWidth());
            int y = rng.nextInt(rc.getMapHeight());
            target = new MapLocation(x, y);
            blockNewTarget = true;
            targetIsRuin = false;
            retreating = false;
        } else {
            retreating = false;
            targetIsRuin = false;
        }

        if (srpToFix == Integer.MAX_VALUE)
            formingSRP = false;

        if (targetIsRuin && rc.isMovementReady() &&
                rc.getLocation().distanceSquaredTo(target) == 1) {
            if (rc.senseMapInfo(target.add(Direction.NORTH)).getMark() == PaintType.EMPTY) {

                if (rng.nextInt(100) < 60 || rc.getNumberTowers() == 2) {
                    markRuin(target, UnitType.LEVEL_ONE_MONEY_TOWER);
                    System.out.println("Marked a ruin for a money tower");
                } else {
                    markRuin(target, UnitType.LEVEL_ONE_PAINT_TOWER);
                    System.out.println("Marked a ruin for a paint tower");
                }
            }
        }

        if (SRPSearch-- < 0 && blockNewTarget && rc.getLocation().distanceSquaredTo(target) < 15) {
            blockNewTarget = false;
        }

        if (directive != null && rc.getLocation().distanceSquaredTo(directive) <= 20) {
            directive = null;
            blockNewTarget = false;
        }


        if (saveSRPLoc != null && rc.onTheMap(saveSRPLoc))
            rc.setIndicatorDot(target, 255, 255, 0);

        if (saveSRPLoc != null && rc.getLocation().equals(saveSRPLoc) && srpToFix == Integer.MAX_VALUE &&
                rc.getPaint() > Math.max(Math.sqrt(rc.getLocation().distanceSquaredTo(lastPainTower)), 15)) {
            saveSRPLoc = null;
        }

        if (saveRuinLoc != null && rc.getLocation().equals(saveRuinLoc) &&
                rc.getPaint() > Math.max(Math.sqrt(rc.getLocation().distanceSquaredTo(lastPainTower)), 15)) {
            saveRuinLoc = null;
        }

        if (rc.onTheMap(target))
            rc.setIndicatorDot(target, 255, 0, 0);

        if (rc.isActionReady()) {
            greedyAttack();
        }

        if (rc.isMovementReady()) {
            if (stuck) {
                stuck = PathFinder.move(currentLocation, target);
            }

            if (!stuck && rc.isMovementReady()) {
                Direction bestDir = considerMoves();
                if (bestDir != Direction.CENTER && !formingSRP) {
                    rc.move(bestDir);
                }
            }
        }

        attackPos = rc.senseNearbyMapInfos(9);
        if (rc.isActionReady() &&
                rc.getPaint() >= Math.max(Math.sqrt(rc.getLocation().distanceSquaredTo(lastPainTower)), 15)) {
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
                    int x = paintLoc.x % 4, y = paintLoc.y % 4;
                    secondary = (GameConstants.RESOURCE_PATTERN & (1 << (x + 5*y))) > 0;
                }

                rc.attack(paintLoc, secondary);
                if (formingSRP) {
                    try {
                        rc.completeResourcePattern(rc.getLocation());
                        rc.mark(rc.getLocation(), true);

                        SRPSearch = 10;
                        blockNewTarget = true;
                        target = new MapLocation(target.x+4, target.y);
                        saveSRPLoc = null;
                        formingSRP = false;
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
                    remarkAsDefense(target);
                    break;
                case GameConstants.DEFENSE_TOWER_PATTERN:
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, target);
                    remarkAsDefense(target);
                    break;
                case GameConstants.MONEY_TOWER_PATTERN:
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, target);
                    remarkAsDefense(target);
                    break;
            }
        } catch (GameActionException e) {
            if (rc.getNumberTowers() >= GameConstants.MAX_NUMBER_OF_TOWERS) { // apparently a thing
                building = rc.getRoundNum();
            }
        }

        if (!formingSRP)
            unstuckify();
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
                        int delta = (int)(Math.random() * 100);
                        if (2000 + delta > bestVal) {
                            bestVal = 2000 + delta;
                            ret = loc;
                        }
                    }
                    else if (m.getPaint() == PaintType.ALLY_SECONDARY ^ secondary) {
                        int delta = (int)(Math.random() * 100);
                        if (1000 + delta > bestVal) {
                            bestVal = 1000 + delta;
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
            int numSuperCloseAllies = 0;
            MapLocation dest = rc.getLocation().add(d);
            if (rc.canMove(d)) {
                for (int i=nearRobots.length-1; i>=0; i--) {
                    if (nearRobots[i].team == rc.getTeam()) {
                        if (nearRobots[i].getLocation().distanceSquaredTo(dest) <= 10) {
                            if (nearRobots[i].type == UnitType.SOLDIER)
                                numAllies++;

                            if (!rc.senseMapInfo(nearRobots[i].location).hasRuin() &&
                                    nearRobots[i].getLocation().isWithinDistanceSquared(dest, 2))
                                numSuperCloseAllies++;
                        }
                    } else if (nearRobots[i].getType() == UnitType.MOPPER) {
                        if (nearRobots[i].getLocation().distanceSquaredTo(dest) <= 4)
                            numEnemyMoppers++;
                    } else if (nearRobots[i].getType() != UnitType.SOLDIER &&
                            nearRobots[i].getType() != UnitType.SPLASHER) {
                        if (nearRobots[i].getLocation().distanceSquaredTo(dest) <= 9)
                            numEnemyTowers++;
                        else if (nearRobots[i].getType().getBaseType() == UnitType.LEVEL_ONE_DEFENSE_TOWER &&
                                nearRobots[i].getLocation().distanceSquaredTo(dest) <= 16) {
                            numEnemyTowers += 2;
                        }
                    }
                }

                MapInfo destInfo = rc.senseMapInfo(dest);
                int val = dest.distanceSquaredTo(target) * -2;
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
                    val -= numEnemyTowers * 100;
                else if (rc.getHealth() > 20)
                    val += numEnemyTowers * 50;

                val -= numSuperCloseAllies * 30;

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
        attackTarget = null;

        for (MapLocation m : nearRuins) {
            RobotInfo ri = rc.senseRobotAtLocation(m);
            if (ri == null && minDist > m.distanceSquaredTo(rc.getLocation()) &&
                    (m != lastOccupiedRuin || lastOccupiedRuinTurn + 10 > rc.getRoundNum())) {
                if (rc.getLocation().distanceSquaredTo(m) > 2 && rc.getRoundNum() > 30 && (
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
            else if (ri != null) {
                if (rc.canSendMessage(ri.getLocation())) {
                    rc.sendMessage(ri.getLocation(), constructMessage(rc.getRoundNum() % 2 == 1));
                }

                // मैं दुनिया पर राज करता था। जब मैं वचन देता तो आंखें उठ जातीं!!

                UnitType tp = ri.getType();
                if (tp == UnitType.LEVEL_ONE_PAINT_TOWER ||
                        tp == UnitType.LEVEL_THREE_PAINT_TOWER ||
                        tp == UnitType.LEVEL_TWO_PAINT_TOWER) {
                    if (ri.getLocation() != this.lastPainTower && ri.getTeam() == rc.getTeam()) {
                        this.semiLastPaintTower = this.lastPainTower;
                        this.lastPainTower = ri.getLocation();
                    }
                }

                if (ri.getTeam() != rc.getTeam()) {
                    lastEnemyTower = ri.getLocation();
                    lastEnemyTowerType = tp;
                    enemyTowerTurn = rc.getRoundNum();

                    if (lastEnemyTowerType.getBaseType() != UnitType.LEVEL_ONE_DEFENSE_TOWER) {
                        attackTarget = lastEnemyTower;
                    } else if (rc.getHealth() > 100 && attackTarget == null) {
                        attackTarget = lastEnemyTower;
                    }
                } else if (m != lastPainTower && m != home){
                    lastFriendlyTower = m;
                    friendlyTowerTurn = rc.getRoundNum();
                }
            }
        }

        return ret;
    }

    // returns the number of squares needed to change for SRP
    // will return INTEGER.MAX_VALUE if this would ruin another SRP
    int neededForSRP() throws GameActionException {
        if (rc.senseMapInfo(rc.getLocation()).isResourcePatternCenter())
            return Integer.MAX_VALUE;

        int x = rc.getLocation().x;
        int y = rc.getLocation().y;
        if (x <= 1 || y <= 1 || y >= rc.getMapHeight()-2 || x >= rc.getMapWidth()-2)
            return Integer.MAX_VALUE;

        int cnt = 2;

        for (int i=nearLocations.length-1; i>=0; i--) {
            MapInfo mi = nearLocations[i];
            MapLocation loc = mi.getMapLocation();
            int relX = loc.x - x;
            int relY = loc.y - y;

            if (relX == 0 && relY == 0) {
                if (rc.senseMapInfo(rc.getLocation()).getMark() == PaintType.ALLY_PRIMARY)
                    cnt -= 2;
            } else if (Math.abs(relX) <= 2 && Math.abs(relY) <= 2) {
                if (!rc.onTheMap(loc))
                    return Integer.MAX_VALUE;

                if (mi.getPaint().isEnemy() || !mi.isPassable() ||
                        mi.getMark() == PaintType.ALLY_PRIMARY && rc.canSenseRobotAtLocation(loc) ||
                        mi.isResourcePatternCenter())
                    return Integer.MAX_VALUE;

                boolean secondary = (GameConstants.RESOURCE_PATTERN & (1 << (relX + 5*relY))) > 0;
                if (!mi.getPaint().isAlly() ||
                        (mi.getPaint() == PaintType.ALLY_SECONDARY ^ secondary))
                    cnt++;
            } else {
                if (!rc.onTheMap(loc))
                    continue;

                if (mi.isResourcePatternCenter() ||
                        mi.getMark() == PaintType.ALLY_PRIMARY && rc.canSenseLocation(loc))
                    return Integer.MAX_VALUE;
            }
        }

        return cnt;
    }

    void greedyAttack() throws GameActionException {
        for (MapLocation m : nearRuins) {
            if (rc.canSenseRobotAtLocation(m) &&
                    rc.senseRobotAtLocation(m).getTeam() != rc.getTeam() &&
                    rc.canAttack(m)) {
                rc.attack(m);
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
}
