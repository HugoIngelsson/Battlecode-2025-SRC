package yorktown;

import battlecode.common.*;

public class Mopper extends Unit {
    private static final int MAX_PAINT = 100;

    MapInfo[] mopLocations;
    MapLocation avoidCrowdingRuin = null;
    int timeoutTurns = 0;
    public Mopper(RobotController rc) throws GameActionException {
        super(rc);
    }

    void play() throws GameActionException {
        closestRuin = getClosestRuin();
        timeoutTurns--;

        if (closestRuin != null){
            lastRuinSeen = closestRuin;
            ruinTurn = rc.getRoundNum();

            if (nothingLeftToRemoveRuin(closestRuin)) {
                if (!closestRuin.equals(avoidCrowdingRuin)) {
                    avoidCrowdingRuin = closestRuin;
                    timeoutTurns = 5;
                }
            }

        }

        mopLocations = rc.senseNearbyMapInfos(4);
        MapLocation enemyPaint, chaseTarget;
        if (rc.getPaint() < Math.max(Math.sqrt(rc.getLocation().distanceSquaredTo(lastPainTower)), 5)) {
            if (lastPainTower == null) {
                lastPainTower = home;
            }
            if (!retreating) {
                storeMessage();
                retreating = true;
            }

            target = lastPainTower;
            targetIsRuin = false;

            if (rc.getLocation().distanceSquaredTo(target) < 2) {
                RobotInfo tower = rc.senseRobotAtLocation(target);
                if (tower != null && rc.isActionReady() && tower.getPaintAmount() > 0) {
                    rc.transferPaint(target, -1 * Math.min(MAX_PAINT*2/3 - rc.getPaint(), tower.getPaintAmount()));
                } else {
                    target = home;
                }
            }
        }
        else if ((chaseTarget = otherTarget()) != null) {
            target = chaseTarget;
            targetIsRuin = false;
            retreating = false;
        }
        else if ((enemyPaint = closePaint()) != null) {
            target = enemyPaint;
            targetIsRuin = false;
            retreating = false;
        }
        else if (closestRuin != null && (!closestRuin.equals(avoidCrowdingRuin) || timeoutTurns > 0)) {
            target = closestRuin;
            retreating = false;
        }
        else if (directive != null) {
            target = directive;
            retreating = false;
        }
        else if (!blockNewTarget) { // roam randomly
            int x = rng.nextInt(7, rc.getMapWidth()-7);
            int y = rng.nextInt(7, rc.getMapHeight()-7);
            target = new MapLocation(x, y);
            blockNewTarget = true;
            targetIsRuin = false;
            retreating = false;
        } else {
            retreating = false;
        }

        if (blockNewTarget && rc.getLocation().distanceSquaredTo(target) < 15) {
            blockNewTarget = false;
        }

        if (directive != null && rc.getLocation().distanceSquaredTo(directive) <= 20) {
            directive = null;
            blockNewTarget = false;
        }

        if (rc.onTheMap(target))
            rc.setIndicatorDot(target, 255, 0, 0);

        if (rc.isMovementReady()) {
            if (stuck) {
                stuck = PathFinder.move(currentLocation, target);
            }

            if (!stuck && rc.isMovementReady()) {
                Direction bestDirMove = considerMoves();
                if (bestDirMove != Direction.CENTER) {
                    rc.move(bestDirMove);
                }
            }
        }

        // where can we mop?
        mopLocations = rc.senseNearbyMapInfos(2);
        if (rc.isActionReady()) {
            MapLocation paintLoc = findBestPaintLoc();
            if (paintLoc != null) {
                if (rc.isActionReady()) {
                    if (rc.getLocation().distanceSquaredTo(paintLoc) > 2) {
                        rc.mopSwing(rc.getLocation().directionTo(paintLoc));
                    }
                    else if (rc.canSenseRobotAtLocation(paintLoc) &&
                            rc.senseRobotAtLocation(paintLoc).getTeam() == rc.getTeam()) {
                        rc.transferPaint(paintLoc, 10);
                    }
                    else {
                        rc.attack(paintLoc);
                    }
                }
            }
        }

        if (target == closestRuin && target != null) {
            try {
                rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, target);
                remarkAsDefense(target);
            } catch (GameActionException e) {
                try {
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, target);
                    remarkAsDefense(target);
                } catch (GameActionException f) {
                    try {
                        rc.completeTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, target);
                    } catch (GameActionException g) { ; }
                }
            }
        }

        unstuckify();
    }

    int[][][] delta_swings = {
            {{-1, 1}, { 0, 1}, { 1, 1}, {-1, 2}, { 0, 2}, { 1, 2}},
            {{ 1,-1}, { 1, 0}, { 1, 1}, { 2,-1}, { 2, 0}, { 2, 1}},
            {{-1,-1}, { 0,-1}, { 1,-1}, {-1,-2}, { 0,-2}, { 1,-2}},
            {{-1,-1}, {-1, 0}, {-1, 1}, {-2,-1}, {-2, 0}, {-2, 1}}
    };

    MapLocation findBestPaintLoc() throws GameActionException {
        MapLocation ret = null;
        int bestVal = Integer.MIN_VALUE;

        for (MapInfo m : mopLocations) {
            if (!m.isPassable())
                continue;

            MapLocation loc = m.getMapLocation();
            if (rc.canSenseRobotAtLocation(loc) &&
                    rc.senseRobotAtLocation(loc).team == rc.getTeam()) {
                if (rc.getPaint() > 50 && rc.senseRobotAtLocation(loc).getPaintAmount() < 10 &&
                        100 - rc.senseRobotAtLocation(loc).getPaintAmount() + rc.getPaint() * 2 > bestVal) {
                    bestVal = 100 - rc.senseRobotAtLocation(loc).getPaintAmount() + rc.getPaint() * 2;
                    ret = loc;
                }
                continue;
            }

            if (m.getPaint() == PaintType.ENEMY_SECONDARY || m.getPaint() == PaintType.ENEMY_PRIMARY) {
                int val = -3 * loc.distanceSquaredTo(home);
                if (closestRuin != null && loc.distanceSquaredTo(closestRuin) <= 5) {
                    val += 200;
                }
                if (loc.equals(rc.getLocation()))
                    val += 135;

                if (val > bestVal) {
                    bestVal = val;
                    ret = loc;
                }
            }

            if (rc.canSenseRobotAtLocation(m.getMapLocation()) &&
                    rc.senseRobotAtLocation(m.getMapLocation()).team != rc.getTeam()) {
                if (500 > bestVal) {
                    bestVal = 500;
                    ret = loc;
                }
            }
        }

        for (int dir=3; dir>=0; dir--) {
            int numHits = 0;
            for (int swing=5; swing>=3; swing--) {
                MapLocation hit = rc.getLocation().translate(delta_swings[dir][swing][0], delta_swings[dir][swing][1]);
                if (rc.canSenseRobotAtLocation(hit) && rc.senseRobotAtLocation(hit).team != rc.getTeam())
                    numHits+=2;
            }
            for (int swing=2; swing>=0; swing--) {
                MapLocation hit = rc.getLocation().translate(delta_swings[dir][swing][0], delta_swings[dir][swing][1]);
                if (rc.canSenseRobotAtLocation(hit) && rc.senseRobotAtLocation(hit).team != rc.getTeam())
                    numHits++;
            }

            if (numHits >= 2 && 1000 + numHits > bestVal) {
                bestVal = 1000 + numHits;
                ret = rc.getLocation().translate(delta_swings[dir][4][0], delta_swings[dir][4][1]);
            }
        }

        return ret;
    }

    MapLocation closePaint() {
        MapLocation closest = null;
        int dist = Integer.MAX_VALUE;

        if (closestRuin != null) {
            for (MapInfo m : mopLocations) {
                if (m.getPaint().isEnemy()) {
                    int dx = m.getMapLocation().x - closestRuin.x;
                    int dy = m.getMapLocation().y - closestRuin.y;
                    if (Math.abs(dx) <= 2 && Math.abs(dy) <= 2 &&
                            m.getMapLocation().distanceSquaredTo(rc.getLocation()) < dist) {
                        dist = m.getMapLocation().distanceSquaredTo(rc.getLocation());
                        closest = m.getMapLocation();
                    }
                }
            }
        } else {
            for (MapInfo m : mopLocations) {
                if (m.getPaint().isEnemy()) {
                    if (m.getMapLocation().distanceSquaredTo(rc.getLocation()) < dist) {
                        dist = m.getMapLocation().distanceSquaredTo(rc.getLocation());
                        closest = m.getMapLocation();
                    }
                }
            }
        }

        if (closest != null) return closest;

        // do a more thorough search
        if (closestRuin != null) {
            for (MapInfo m : nearLocations) {
                if (m.getPaint().isEnemy()) {
                    int dx = m.getMapLocation().x - closestRuin.x;
                    int dy = m.getMapLocation().y - closestRuin.y;
                    if (Math.abs(dx) <= 2 && Math.abs(dy) <= 2 &&
                            m.getMapLocation().distanceSquaredTo(rc.getLocation()) < dist) {
                        dist = m.getMapLocation().distanceSquaredTo(rc.getLocation());
                        closest = m.getMapLocation();
                    }
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
            int numSuperCloseAllies = 0;
            MapLocation dest = rc.getLocation().add(d);
            if (rc.canMove(d)) {
                for (int i=nearRobots.length-1; i>=0; i--) {
                    if (nearRobots[i].team == rc.getTeam()) {
                        if (nearRobots[i].getLocation().distanceSquaredTo(dest) <= 4){
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

                        if (nearRobots[i].getType().getBaseType() == UnitType.LEVEL_ONE_DEFENSE_TOWER &&
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
                int val = dest.distanceSquaredTo(target) * -2;
                if (!destInfo.getPaint().isAlly()) {
                    if (destInfo.getPaint() == PaintType.EMPTY) {
                        val -= 40;
                    } else {
                        val -= 70;
                    }
                } else if (rc.getPaint() < 20) {
                    val += 70;
                }

                if (canMop) val += 100;

                if (d != Direction.CENTER) {
                    for (int i = 6; i >= 0; i--) {
                        if (dest.equals(lastLocations[i])) {
                            val -= 20;
                            break;
                        }
                    }
                }

                val += 30 * numAllies - numEnemyMoppers - numEnemyTowers * 200 - numSuperCloseAllies * 70;

                if (lastDefenseTowerSeen != null && rc.getLocation().isWithinDistanceSquared(lastDefenseTowerSeen, 16))
                    val -= 200;

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

            if (ri != null) {

                if (rc.canSendMessage(ri.getLocation())) {
                    rc.sendMessage(ri.getLocation(), constructMessage(true));
                }
                UnitType tp = ri.getType();

                // मैं दुनिया पर राज करता था। जब मैं वचन देता तो आंखें उठ जातीं!!
                if (tp == UnitType.LEVEL_ONE_PAINT_TOWER ||
                        tp == UnitType.LEVEL_THREE_PAINT_TOWER ||
                        tp == UnitType.LEVEL_TWO_PAINT_TOWER) {
                    if (ri.getLocation() != this.lastPainTower && ri.getTeam() == rc.getTeam()) {
                        this.semiLastPaintTower = this.lastPainTower;
                        this.lastPainTower = ri.getLocation();
                    }
                }

                if (ri.getTeam() != rc.getTeam()){
                    lastEnemyTower = m;
                    lastEnemyTowerType = tp;
                    enemyTowerTurn = rc.getRoundNum();

                    if (lastEnemyTowerType.getBaseType() == UnitType.LEVEL_ONE_DEFENSE_TOWER) {
                        lastDefenseTowerSeen = ri.getLocation();
                    }
                } else if (m != lastPainTower && m != home){
                    lastFriendlyTower = m;
                    friendlyTowerTurn = rc.getRoundNum();
                }
            }
        }

        return ret;
    }

    boolean nothingLeftToRemoveRuin(MapLocation loc) throws GameActionException {
        for (int i=-2; i<=2; i++) {
            for (int j=-2; j<=2; j++) {
                if (rc.canSenseLocation(loc.translate(i, j)) && rc.senseMapInfo(loc.translate(i, j)).getPaint().isEnemy()) {
                    return false;
                }
            }
        }

        return true;
    }

    MapLocation otherTarget() {
        MapLocation bestTarget = null;
        int bestDist = Integer.MAX_VALUE;
        for (RobotInfo ri : nearRobots) {
            if (!ri.getType().isRobotType() || ri.getTeam().isPlayer())
                continue;

            if (ri.getHealth() > ri.getType().health / 3)
                continue;

            if (bestDist > ri.getLocation().distanceSquaredTo(rc.getLocation())) {
                bestTarget = ri.getLocation();
                bestDist = ri.getLocation().distanceSquaredTo(rc.getLocation());
            }
        }

        return bestTarget;
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
