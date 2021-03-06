
import java.io.InputStream;import java.io.OutputStream;import java.util.ArrayList;import java.util.HashMap;import java.util.LinkedList;import java.util.List;import java.util.Map;import java.util.PriorityQueue;
import edu.cwru.sepia.action.Action;import edu.cwru.sepia.agent.Agent;import edu.cwru.sepia.environment.model.history.History.HistoryView;import edu.cwru.sepia.environment.model.state.ResourceNode;import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;import edu.cwru.sepia.environment.model.state.State.StateView;import edu.cwru.sepia.environment.model.state.Unit;import edu.cwru.sepia.environment.model.state.Unit.UnitView;import edu.cwru.sepia.util.Direction;import edu.cwru.sepia.util.DistanceMetrics;public class ProbAgent extends Agent {	private BoardNode[][] board;	private final byte towerRange = 4;	private final byte peasantRange = 2;	private final byte towerDamage = 13;	private Map<Integer,Integer> oldHp = new HashMap<Integer,Integer>();	private Map<Integer,Coordinate> oldSpot = new HashMap<Integer,Coordinate>();	private List<Coordinate> badMoves = new LinkedList<Coordinate>();	private List <Coordinate> goldMines = new LinkedList<Coordinate>();	private List <Coordinate> towers = new LinkedList<Coordinate>();	private List<Integer> townhalls = new LinkedList<Integer>();
 	private int range = 5;
 	private int safePath=0;
	
	private Coordinate goalC = new Coordinate(0,0);	public ProbAgent(int playernum){ 		super(playernum);	}
		private void getUnits(List<Integer> peasants, StateView state){ 		for (Integer id : state.getAllUnitIds()){ 			UnitView u = state.getUnit(id);			if (u.getTemplateView().getName().equalsIgnoreCase("peasant"))			{				peasants.add(id);			}		}	}	private void getTownhalls(StateView state){		for(Integer id: state.getAllUnitIds()){			UnitView u = state.getUnit(id);			if(u.getTemplateView().getName().equalsIgnoreCase("townhall")){				townhalls.add(id);				board[u.getXPosition()][u.getYPosition()].setOpen(false);			}		}	}	@Override	public Map<Integer, Action> initialStep(StateView state, HistoryView view){		board = new BoardNode[state.getXExtent()][state.getYExtent()];		for(int i = 0; i< state.getXExtent(); i++){			for(int j = 0; j < state.getYExtent(); j++){				board[i][j] = new BoardNode();			}		}
		
		for (Integer id : state.getAllUnitIds()){ 
			UnitView u = state.getUnit(id);
			if (u.getTemplateView().getName().equalsIgnoreCase("peasant"))
			{
				oldSpot.put(id, new Coordinate(u.getXPosition(), u.getYPosition()));
				oldHp.put(id, u.getHP());
			}
		}
		goalC = new Coordinate(state.getXExtent()-1,0);		return null;	}	public boolean badMove(int x, int y ){		for(Coordinate c: badMoves){			if(c.getX() == x && c.getY() == y)				return true;		}	return false;	}	@Override	public void loadPlayerData(InputStream arg) {		// TODO Auto-generated method stu	}
	public void foundGoldMine(int x, int y){
		for (Coordinate coor:goldMines){
			if (coor.getX() == x && coor.getY() == y){
				return;
			}
		}
		goldMines.add(new Coordinate(x,y));
	}
	
	public void setAroundTree(int x, int y, StateView state){
		for(int i = x-2; i< x+2;i++){
			for(int j = y-2; j< y+2;j++){
				if(state.inBounds(i, j) && DistanceMetrics.euclideanDistance(i, j, x, y)  <=1 && board[i][j].getProbability() <= .75 &&board[i][j].getHit()!=(byte)2)
					board[i][j].setProbability(.75);
				else if(state.inBounds(i, j) && DistanceMetrics.euclideanDistance(i, j, x, y) <=2 && board[i][j].getProbability() < .5 &&board[i][j].getHit()!=(byte)2)
					board[i][j].setProbability(.5);
				//if (board[i][j].isSteppedOn() && board[i][j].getHit()==(byte)0)
					//board[i][j].setProbability(0);
			}
		}
	
	}	public void checkSpaces(Unit.UnitView unit,State.StateView state){		int startX = unit.getXPosition()-2;		int startY = unit.getYPosition()-2;		int endX = unit.getXPosition()+2;		int endY = unit.getYPosition()+2;
		int oldHealth=50;
		
		if (oldHp.containsKey(unit.getID())){
			oldHealth = oldHp.get(unit.getID());
		}
		if (!oldSpot.containsKey(unit.getID()))
				oldSpot.put(unit.getID(), new Coordinate(unit.getXPosition(),unit.getYPosition()));		int difference = oldHealth - unit.getHP();
		if (difference >0){
			board[oldSpot.get(unit.getID()).getX()][oldSpot.get(unit.getID()).getY()].setHit((byte)1);
		}
		else{
			board[oldSpot.get(unit.getID()).getX()][oldSpot.get(unit.getID()).getY()].setProbability(0);
			board[oldSpot.get(unit.getID()).getX()][oldSpot.get(unit.getID()).getY()].setHit((byte)2);

		}
		oldHp.put(unit.getID(), unit.getHP());		double prob = board[oldSpot.get(unit.getID()).getX()][oldSpot.get(unit.getID()).getY()].getProbability();		if(difference > 13)			prob= .75*.25 + .75*.25 + .75*.75;		else if(difference >0)			prob= .75;		else{
			if(board[oldSpot.get(unit.getID()).getX()][oldSpot.get(unit.getID()).getY()].getHit() ==0)
			prob/=2;
		}		board[oldSpot.get(unit.getID()).getX()][oldSpot.get(unit.getID()).getY()].setProbability(prob);		for(int i = startX; i<= endX ; i++){			for(int j = startY; j<= endY; j++){				if(state.inBounds(i,j)){					BoardNode space = board[i][j];					space.setSeen(true);					if(i == unit.getXPosition() && j == unit.getYPosition()){						space.setSteppedOn(true);
						space.setSeen(true);
					}					else if(state.isResourceAt(i,j)){						ResourceNode.ResourceView resource = state.getResourceNode(state.resourceAt(i,j));						if(resource.getType() == ResourceNode.Type.GOLD_MINE){							space.setGoldMine(true);
							foundGoldMine(i,j);
						}						else{							space.setTree(true);
							setAroundTree(i,j,state);
						}						space.setOpen(false);					}					else if(state.isUnitAt(i,j)){						Unit.UnitView unitAt = state.getUnit(state.unitAt(i,j));						if(unitAt.getTemplateView().getName().equalsIgnoreCase("ScoutTower")){							space.setOpen(false);							space.setTower(true);							towerHere(unitAt, state);						}					}				space.setSeen(true);				}							}		}	}
	
	public boolean spaceInTowerRange(int x1, int y1, int x2, int y2){
		return (DistanceMetrics.euclideanDistance(x1, y1, x2, y2) <=4);
	}		public void towerHere(Unit.UnitView unit, State.StateView state){
			int range = unit.getTemplateView().getRange();
		int startX = unit.getXPosition()-range;
		int startY = unit.getYPosition()-range;
		int endX = unit.getXPosition()+range;
		int endY = unit.getYPosition()+range;		for(int i = startX; i<= endX ; i++){		for(int j = startY; j<= endY; j++){
				if(state.inBounds(i, j) && spaceInTowerRange(i,j,unit.getXPosition(),unit.getYPosition())){
				board[i][j].setProbability(.75);
				board[i][j].setHit((byte) 1);
				}		}	}		}
		
		private void checkNoTower(int i, int j,StateView state){
			for(int x = i-4; x<=i+4; x++){
				for(int y = j-4; y<=j+4;y++){
					if(x==i && y==j || !spaceInTowerRange(i,j,x,y)){
						
					}
					else{
						if(state.inBounds(x, y) && (board[x][y].isTower() || !board[x][y].isSeen()))
						return;
					}
				}
			}
			board[i][j].setHit((byte)2);
			board[i][j].setProbability(0);
		}
		private void checkSafe(State.StateView state){
			for(int i = 0; i <state.getXExtent();i++){
				for(int j = 0; j < state.getYExtent(); j++){
					checkNoTower(i,j,state);
				}
			}
		}	/**		 * This converts the coordinates of the position of the footman and the		 * position it wants to move to, to a Direction please look at the Direction		 * class for more information		 * 	 * @param n	 	 *            the Node to move to	 	 * @return the direction the Node is reletive to the footman	 	 */	 	private Direction convertToDirection(Node n,Unit.UnitView unit) {	 		int x = n.getX();	 		int y = n.getY();	 		int x2 =  unit.getXPosition();	 		int y2 = unit.getYPosition();	 		int xDiff = x - x2;	 		int yDiff = y - y2;	 		switch (xDiff) {	 		case 1:	 			switch (yDiff) {	 			case 0:	 				return Direction.EAST;	 			case 1:	 				return Direction.SOUTHEAST;	 			case -1:	 				return Direction.NORTHEAST;	 			}	 		case -1:	 			switch (yDiff) {	 			case 0:	 				return Direction.WEST;	 			case -1:	 				return Direction.NORTHWEST;	 			case 1:	 				return Direction.SOUTHWEST;	 			}	 		case 0:	 			switch (yDiff) {	 			case 1:	 				return Direction.SOUTH;	 			case -1:	 				return Direction.NORTH;	 			}	 		}	 		return null;	 			}			/**	 * Takes a node and returns the path to get to that node by going to the	 * parent of each node until the parent becomes null. It returns a list of	 * coordinates to move to, to get to the Node.	 * 	 * @param n	 *            the node to trace the route to	 * @return A linked list of coordinates from a starting state to Node n	 */		 	public LinkedList<Node> backTrace(Node n) {	 		LinkedList<Node> backtrace = new LinkedList<Node>();		 		while (n.getParent() != null) {			 			backtrace.addFirst(n);				 			n = n.getParent();		}		 		return backtrace;	}	/**	 	 * Checks to see if the node can move to the east, it does this by checking	 	 * to see if the coordinates to move are on the map, if there are no units	 	 * on that space, and that there are also no resources on that space.	 	 * 	 	 * @param node	 	 *            The node to check if it can move to the east	 	 * @param state	 	 *            the current state of the map	 	 * @return true if the unit can move east or false if it cannot	 	 */	 	private boolean canMoveEast(Node node, StateView state) {	 		int newX = node.getX() +1;	 		int newY = node.getY();	 		return (state.inBounds(newX,newY) && !badMove(newX,newY) && board[newX][newY].isOpen());	 	}	/**	 	 * Checks to see if the node can move to the north, it does this by checking	 	 * to see if the coordinates to move are on the map, if there are no units	 	 * on that space, and that there are also no resources on that space.	 	 * 	 	 * @param node	 	 *            The node to check if it can move to the north	 	 * @param state	 	 *            the current state of the map	 	 * @return true if the unit can move north or false if it cannot	 	 */	 	private boolean canMoveNorth(Node node, StateView state) {	 		int newX = node.getX();	 		int newY = node.getY() -1;	 		return (state.inBounds(newX, newY) && !badMove(newX,newY) && board[newX][newY].isOpen());	 	}	/**	 	 * Checks to see if the node can move to the northeast, it does this by	 	 * checking to see if the coordinates to move are on the map, if there are	 	 * no units on that space, and that there are also no resources on that	 	 * space.	 	 * 	 	 * @param node	 	 *            The node to check if it can move to the northeast	 	 * @param state	 	 *            the current state of the map	 	 * @return true if the unit can move northeast or false if it cannot	 	 */	 	private boolean canMoveNortheast(Node node, StateView state) {	 			int newX = node.getX() +1;	 			int newY = node.getY() -1;	 		return (state.inBounds(newX, newY) && !badMove(newX,newY) && board[newX][newY].isOpen());	 	}	/**	 	 * Checks to see if the node can move to the northwest, it does this by	 	 * checking to see if the coordinates to move are on the map, if there are	 	 * no units on that space, and that there are also no resources on that	 	 * space.	 	 * 	 	 * @param node	 	 *            The node to check if it can move to the northwest	 	 * @param state	 	 *            the current state of the map	 	 * @return true if the unit can move northwest or false if it cannot	 	 */	 	private boolean canMoveNorthwest(Node node, StateView state) {	 		int newX = node.getX() -1;	 		int newY = node.getY() -1;	 		return (state.inBounds(newX, newY) && !badMove(newX,newY) && board[newX][newY].isOpen());	 	}	/**	 	 * Checks to see if the node can move to the south, it does this by checking	 	 * to see if the coordinates to move are on the map, if there are no units	 	 * on that space, and that there are also no resources on that space.	 	 * 	 	 * @param node	 	 *            The node to check if it can move to the south	 	 * @param state	 	 *            the current state of the map	 	 * @return true if the unit can move south or false if it cannot	 	 */	 	private boolean canMoveSouth(Node node, StateView state) {	 		int newX = node.getX();	 		int newY = node.getY() +1;	 		return (state.inBounds(newX, newY) && !badMove(newX,newY) && board[newX][newY].isOpen());	 	}	/**	 	 * Checks to see if the node can move to the southeast, it does this by	 	 * checking to see if the coordinates to move are on the map, if there are	 	 * no units on that space, and that there are also no resources on that	 	 * space.	 	 * 	 	 * @param node	 	 *            The node to check if it can move to the southeast	 	 * @param state	 	 *            the current state of the map	 	 * @return true if the unit can move southeast or false if it cannot	 	 */	 	private boolean canMoveSoutheast(Node node, StateView state) {	 		int newX = node.getX() +1;	 		int newY = node.getY() +1;	 		return (state.inBounds(newX, newY) && !badMove(newX,newY) && board[newX][newY].isOpen());	 	}	/**	 	 * Checks to see if the node can move to the southwest, it does this by	 	 * checking to see if the coordinates to move are on the map, if there are	 	 * no units on that space, and that there are also no resources on that	 	 * space.	 	 * 	 	 * @param node	 	 *            The node to check if it can move to the southwest	 	 * @param state	 	 *            the current state of the map	 	 * @return true if the unit can move southwest or false if it cannot	 	 */	 	private boolean canMoveSouthwest(Node node, StateView state) {	 		int newX = node.getX() -1;	 		int newY = node.getY() +1;	 		return (state.inBounds(newX, newY) && !badMove(newX,newY) && board[newX][newY].isOpen());	 	}	/**	 	 * Checks to see if the node can move to the west, it does this by checking	 	 * to see if the coordinates to move are on the map, if there are no units	 	 * on that space, and that there are also no resources on that space.	 	 * 	 	 * @param node	 	 *            The node to check if it can move to the west	 	 * @param state	 	 *            the current state of the map	 	 * @return true if the unit can move west or false if it cannot	 	 */	 	private boolean canMoveWest(Node node, StateView state) {	 		int newX = node.getX() -1;	 		int newY = node.getY();	 		return (state.inBounds(newX, newY) && !badMove(newX,newY) && board[newX][newY].isOpen());	 	}	 	private LinkedList<Node> path(StateView state, Unit.UnitView start, int  goalX, int goalY) {	 		NodeComparator compare = new NodeComparator();	 		PriorityQueue<Node> openList = new PriorityQueue<Node>(10, compare);	 		List<Node> closedList = new LinkedList<Node>();	 		Node startNode = new Node(start.getXPosition(),start.getYPosition(),0,0,null);
	 		startNode.setHP(start.getHP());	 		openList.add(startNode);	 		while (!openList.isEmpty()) {	 			Node temp = openList.poll();	 			setNeighbors(temp, state,goalX,goalY);	 			for (Node neighbor : temp.getNeighbors()) {	 				boolean skip = false;	 				if (goal(neighbor,goalX,goalY) ) {
	 					System.out.println(temp.getfCost());	 					return backTrace(neighbor);	 				}	 				skip = checkClosedList(neighbor, closedList)	 						|| checkOpenList(neighbor, openList);	 				if (!skip)	 					openList.add(neighbor);	 			}	 			closedList.add(temp);	 		}		return null;	}		/**	 	 * This function checks to see what neighbors the current node can have and	 	 * creates those neighbors, adding them to that nodes list of neighbors	 	 * 	 	 * @param node	 	 *            the node to check neighbors for	 	 * @param state	 	 *            the current state of the map	 	 */	 	private void setNeighbors(Node node, StateView state,int x, int y) {	 		List<Node> neighbors = new LinkedList<Node>();	 		if (canMoveNorth(node, state)) {
	 			if ((safePath==1 && board[node.getX()][node.getY()-1].getHit()==(byte)2)|| safePath!=1){	 				Node north = moveNorth(node,x,y);	 				neighbors.add(north);
	 			}	 		}	 		if (canMoveNortheast(node, state)) {
	 			if ((safePath==1 && board[node.getX()+1][node.getY()-1].getHit()==(byte)2)|| safePath!=1){	 				Node northeast = moveNortheast(node,x,y);	 				neighbors.add(northeast);
	 			}	 		}	 		if (canMoveEast(node, state)) {
	 			if ((safePath==1 && board[node.getX()+1][node.getY()].getHit()==(byte)2)|| safePath!=1){	 			Node east = moveEast(node,x,y);	 			neighbors.add(east);
	 			}	 		}	 		if (canMoveSoutheast(node, state)) {
	 			if ((safePath==1 && board[node.getX()+1][node.getY()+1].getHit()==(byte)2)|| safePath!=1){	 			Node southeast = moveSoutheast(node,x,y);	 			neighbors.add(southeast);
	 			}	 		}	 		if (canMoveSouth(node, state)) {
	 			if ((safePath==1 && board[node.getX()][node.getY()+1].getHit()==(byte)2)|| safePath!=1){	 			Node south = moveSouth(node,x,y);	 			neighbors.add(south);
	 			}	 		}	 		if (canMoveSouthwest(node, state)) {
	 			if ((safePath==1 && board[node.getX()-1][node.getY()+1].getHit()==(byte)2) || safePath!=1){	 			Node southwest = moveSouthwest(node,x,y);	 			neighbors.add(southwest);
	 			}	 		}	 		if (canMoveWest(node, state)) {
	 			if ((safePath==1 && board[node.getX()-1][node.getY()].getHit()==(byte)2)|| safePath!=1){	 			Node west = moveWest(node,x,y);	 			neighbors.add(west);
	 			}	 		}	 		if (canMoveNorthwest(node, state)) {
	 			if ((safePath==1 && board[node.getX()-1][node.getY()-1].getHit()==(byte)2)|| safePath!=1){	 			Node northwest = moveNorthwest(node,x,y);	 			neighbors.add(northwest);
	 			}	 		}	 		node.setNeighbors(neighbors);	 	}	 	/**	 	 * This checks a node with a priortyqueue to see that when there are two	 	 * nodes that are in the same position that the f cost of the node is less	 	 * than the cost of the node in the open list. if that cost is less than we	 	 * want to add that node to the open list because it might be a more optimal	 	 * path	 	 * 	 	 * @param neighbor	 	 *            the node to check with every node in the open list	 	 * @param openList	 	 *            the current list of open nodes	 	 * @return true if the node is more optimal or false if it is not	 	 */	 	private boolean checkOpenList(Node neighbor, PriorityQueue<Node> openList) {	 		for (Node check : openList) {	 			if (check.getX() == neighbor.getX()	 					&& check.getY() == neighbor.getY()	 					&& neighbor.getfCost() > check.getfCost())	 				return true;	 		}	 		return false;	 	}	/**	 	 * This creates a node in the direction, it uses the total cost of the	 	 * current node to make that the new nodes g cost and then uses the	 	 * chebyshev distance for the new nodes h cost.	 	 * 	 	 * @param node	 	 *            the parent of the new node	 	 * @return the new node created	 	 */	 	private Node moveEast(Node node,int x, int y) {
	 		int newX = node.getX() +1;
	 		int newY = node.getY();
	 		double h = DistanceMetrics.chebyshevDistance(newX, newY, x, y)*1000;
	 		if(board[newX][newY].getHit() != 2)
	 			h = DistanceMetrics.chebyshevDistance(newX, newY, x, y) * 1000 + board[newX][newY].getCost();
	 		return new Node(newX,newY, node.getfCost(),h, node);	}	/**	 	 * This creates a node in the direction, it uses the total cost of the	 	 * current node to make that the new nodes g cost and then uses the	 	 * chebyshev distance for the new nodes h cost.	 	 * 	 	 * @param node	 	 *            the parent of the new node	 	 * @return the new node created	 	 */	 	private Node moveNorth(Node node,int x, int y) {
	 		int newX = node.getX();
	 		int newY = node.getY() -1;
	 		double h = DistanceMetrics.chebyshevDistance(newX, newY, x, y)*1000;
	 		if(board[newX][newY].getHit() != 2)
	 			h = DistanceMetrics.chebyshevDistance(newX, newY, x, y) * 1000 + board[newX][newY].getCost();
	 		return new Node(newX,newY, node.getfCost(),h, node);	}	/**	 	 * This creates a node in the direction, it uses the total cost of the	 	 * current node to make that the new nodes g cost and then uses the	 	 * chebyshev distance for the new nodes h cost.	 	 * 	 	 * @param node	 	 *            the parent of the new node	 	 * @return the new node created	 	 */	 	private Node moveNortheast(Node node,int x, int y) {
	 		int newX = node.getX() +1;
	 		int newY = node.getY() -1;
	 		double h = DistanceMetrics.chebyshevDistance(newX, newY, x, y)*1000;
	 		if(board[newX][newY].getHit() != 2)
	 			h = DistanceMetrics.chebyshevDistance(newX, newY, x, y) * 1000 + board[newX][newY].getCost();
	 		return new Node(newX,newY, node.getfCost(),h, node);	}	/**	 	 * This creates a node in the direction, it uses the total cost of the	 	 * current node to make that the new nodes g cost and then uses the	 	 * chebyshev distance for the new nodes h cost.	 	 * 	 	 * @param node	 	 *            the parent of the new node	 	 * @return the new node created	 	 */	 	private Node moveNorthwest(Node node,int x, int y) {
	 		int newX = node.getX() -1;
	 		int newY = node.getY() -1;
	 		double h = DistanceMetrics.chebyshevDistance(newX, newY, x, y)*1000;
	 		if(board[newX][newY].getHit() != 2)
	 			h = DistanceMetrics.chebyshevDistance(newX, newY, x, y) * 1000 + board[newX][newY].getCost();
	 		return new Node(newX,newY, node.getfCost(),h, node);}	/**	 	 * This creates a node in the direction, it uses the total cost of the	 	 * current node to make that the new nodes g cost and then uses the	 	 * chebyshev distance for the new nodes h cost.	 	 * 	 	 * @param node	 	 *            the parent of the new node	 	 * @return the new node created	 	 */	 	private Node moveSouth(Node node,int x, int y) {
	 		int newX = node.getX();
	 		int newY = node.getY() +1;
	 		double h = DistanceMetrics.chebyshevDistance(newX, newY, x, y)*1000;
	 		if(board[newX][newY].getHit() != 2)
	 			h = DistanceMetrics.chebyshevDistance(newX, newY, x, y) * 1000 + board[newX][newY].getCost();
	 		return new Node(newX,newY, node.getfCost(),h, node);}	/**	 	 * This creates a node in the direction, it uses the total cost of the	 	 * current node to make that the new nodes g cost and then uses the	 	 * chebyshev distance for the new nodes h cost.	 	 * 	 	 * @param node	 	 *            the parent of the new node	 	 * @return the new node created	 	 */	 	private Node moveSoutheast(Node node,int x, int y) {
	 		int newX = node.getX() +1;
	 		int newY = node.getY()+1;
	 		double h = DistanceMetrics.chebyshevDistance(newX, newY, x, y)*1000;
	 		if(board[newX][newY].getHit() != 2)
	 			h = DistanceMetrics.chebyshevDistance(newX, newY, x, y) * 1000 + board[newX][newY].getCost();
	 		return new Node(newX,newY, node.getfCost(),h, node);	}	/**	 	 * This creates a node in the direction, it uses the total cost of the	 	 * current node to make that the new nodes g cost and then uses the	 	 * chebyshev distance for the new nodes h cost.	 	 * 	 	 * @param node	 	 *            the parent of the new node	 	 * @return the new node created	 	 */	 	private Node moveSouthwest(Node node,int x, int y) {
	 		int newX = node.getX() -1;
	 		int newY = node.getY()+1;
	 		double h = DistanceMetrics.chebyshevDistance(newX, newY, x, y)*1000;
	 		if(board[newX][newY].getHit() != 2)
	 			h = DistanceMetrics.chebyshevDistance(newX, newY, x, y) * 1000 + board[newX][newY].getCost();
	 		return new Node(newX,newY, node.getfCost(),h, node);	 	}	/**	 	 * This creates a node in the direction, it uses the total cost of the	 	 * current node to make that the new nodes g cost and then uses the	 	 * chebyshev distance for the new nodes h cost.	 	 * 	 	 * @param node	 	 *            the parent of the new node	 	 * @return the new node created	 	 */	 	private Node moveWest(Node node,int x, int y) {
	 		int newX = node.getX() -1;
	 		int newY = node.getY();
	 		double h = DistanceMetrics.chebyshevDistance(newX, newY, x, y)*1000;
	 		if(board[newX][newY].getHit() != 2)
	 			h = DistanceMetrics.chebyshevDistance(newX, newY, x, y) * 1000 + board[newX][newY].getCost();	 		return new Node(newX,newY, node.getfCost(),h, node);	}	 	private boolean goal(Node node, int goalX, int goalY) {	 		int xDiff = Math.abs(goalX - node.getX());	 		int yDiff = Math.abs(goalY- node.getY());	 		if (xDiff == 0) {	 			return yDiff == 1;	 		}	 		if (yDiff == 0)	 			return xDiff == 1;	 		return xDiff == 1 && yDiff == 1;	 	}	/**	 	 * This checks a node with a list to see that when there are two nodes that	 	 * are in the same position that the f cost of the node is less than the	 	 * cost of the node in the closed list. if that cost is less than we want to	 	 * add that node to the open list because it might be a more optimal path	 	 * 	 	 * @param neighbor	 	 *            the node to check with every node in the closed list	 	 * @param closedList	 	 *            the current list of closed nodes	 	 * @return true if the node is more optimal or false if it is not	 	 */	 	private boolean checkClosedList(Node neighbor, List<Node> closedList) {	 		for (Node check : closedList) {	 			if (check.getX() == neighbor.getX()	 					&& check.getY() == neighbor.getY()	 					&& neighbor.getfCost() > check.getfCost())	 				return true;	 		}	 		return false;	 	}	 	public Coordinate createGoal(Unit.UnitView unit,StateView state){	 	if (goldMines.isEmpty()){	 			//explore	 		if(board[goalC.getX()][goalC.getY()].isSeen()){
	 			goalC = new Coordinate(goalC.getX() -1,0);
	 		}
	 		return goalC;	 		//if (oldHp.get(unit.getID())>unit.getHP()){	 			//look for tower	 		//}	 		//else{return new);}	 	}	 	else if(unit.getCargoAmount()==0){	 		return new Coordinate(goldMines.get(0).getX(),goldMines.get(0).getY());	 	}	 	else{	 		UnitView townhall = state.getUnit(townhalls.get(0));	 		return new Coordinate(townhall.getXPosition(),townhall.getYPosition());	 	}	 	//return null;	 }
	 public Direction directionFromUnit(Unit.UnitView unit,int x,int y)
	 {
		 Node n = new Node(x,y,0,0,null);
		 return convertToDirection(n,unit);
	 }
	 public void setHeuristic(StateView state){
		 for (int x=0;x<state.getXExtent();x++){
			 for (int y=0;y<state.getYExtent();y++){
				 board[x][y].heuristic();
			 }
		 }
	 }
	 public Map<Integer,Action> buildPeasant(Map<Integer,Action> builder, StateView state){
		 if (state.getResourceAmount(0,ResourceType.GOLD)>=400){
			 
				builder.put(townhalls.get(0), Action.createCompoundProduction(townhalls.get(0), state.getTemplate(playernum, "Peasant").getID()));
		 }
		 return builder;
	 } 	 public Map<Integer, Action> middleStep(StateView state, HistoryView view) {	 	List<Integer> peasants = new ArrayList<Integer>();	 	Map<Integer,Action> builder = new HashMap<Integer,Action>();	 	getUnits(peasants,state);	 	for(Integer peasant: peasants){	 		Unit.UnitView unit = state.getUnit(peasant);	 		checkSpaces(unit,state);
	 		this.oldSpot.put(peasant, new Coordinate(unit.getXPosition(),unit.getYPosition()));	 	}
		for(int i = 0; i< state.getYExtent(); i++){
			for(int j = 0; j < state.getXExtent() ; j++){
				System.out.print(board[j][i].getHit());
				System.out.print(' ');
			}
			System.out.println();
		}
		
		checkSafe(state);
	 	setHeuristic(state);	 	badMoves.clear();
	 	//if (peasants.size()<=1)
	 		//builder=buildPeasant(builder,state);
	 	for(int i = 0; i < peasants.size();i++){
	 		if(i!=0){
	 			badMoves.add(new Coordinate(state.getUnit(peasants.get(i)).getXPosition(),state.getUnit(peasants.get(i)).getYPosition()));
	 		}
	 	}	 	for(Integer peasant:peasants){	 		Unit.UnitView unit = state.getUnit(peasant);	 		getTownhalls(state);	 		Unit.UnitView townhall = state.getUnit(townhalls.get(0));	 		Coordinate goalP = createGoal(unit,state);	 		if(Math.abs(unit.getXPosition()-goalP.getX())<=1 && Math.abs(unit.getYPosition()-goalP.getY())<=1 && goalP.getX()==townhall.getXPosition() && goalP.getY()==townhall.getYPosition() && unit.getCargoAmount()>0){	 			builder.put(peasant, Action.createCompoundDeposit(peasant, townhalls.get(0)));
	 			if (peasants.size()<2)
	 				safePath=1;
	 			range = 10;	 		}	 		else if(Math.abs(unit.getXPosition()-goalP.getX())<=1 && Math.abs(unit.getYPosition()-goalP.getY())<=1 && board[goalP.getX()][goalP.getY()].isGoldMine() && unit.getCargoAmount()==0){	 			builder.put(peasant, Action.createPrimitiveGather(peasant, directionFromUnit(unit,goldMines.get(0).getX(),goldMines.get(0).getY())));
	 			range = 6;
	 			if (safePath==1&&peasants.size()<2)
	 				range=10;	 		}	 		else{
	 			Coordinate nextm = nextBestMove(unit,state,goalP.getX(),goalP.getY(),range);
	 		//if (safePath == 1 && unit.getCargoAmount()==0){
	 			//nextm = new Coordinate(goldMines.get(0).getX(),goldMines.get(0).getY());
	 		//}
	 		//else if (safePath ==1 && unit.getCargoAmount()>0){
	 		//	nextm = new Coordinate(townhall.getXPosition(),townhall.getYPosition());
	 		//}
	 		//else { nextm = nextBestMove(unit,state,goalP.getX(),goalP.getY(),range);}	 		LinkedList<Node> unitPath = path(state,unit,nextm.getX(),nextm.getY());
	 		Direction nextMove = Direction.NORTH;
	 		if (unitPath==null){
	 			//nextMove = convertToDirection(new Node(unit.getXPosition(),unit.getYPosition()-1,0,0,null),unit);
	 		}
	 		else{	 		Node next = unitPath.poll();	 		badMoves.add(new Coordinate(next.getX(),next.getY()));	 		nextMove = convertToDirection(next,unit);
	 		}	 		builder.put(peasant,Action.createPrimitiveMove(peasant,nextMove));	 		}	 	}	 	return builder;	 }	 public Coordinate nextBestMove(UnitView unit,StateView state,int goalX, int goalY,int range){	double cheby = Double.MAX_VALUE;	int bestX = 0;	int bestY = 0;		for(int x = unit.getXPosition() -(2 + range) ;x<=unit.getXPosition() + (2+ range);x++){			for(int y = unit.getYPosition() -(2 + range);y<=unit.getYPosition() + (2 + range);y++){				if(!state.inBounds(x, y) || (x == unit.getXPosition() && y == unit.getYPosition()) ){									}				else{
				BoardNode n = board[x][y];
										double temp = DistanceMetrics.chebyshevDistance(x, y, goalX, goalY)* 50000 + n.getCost();					if(temp < cheby ){
						if (safePath==1 && n.getHit()==(byte)2 || safePath!=1){						cheby = temp;						bestX = x;						bestY = y;
						}
					}				}			}		}		return new Coordinate(bestX,bestY);	 }		@Override	public void savePlayerData(OutputStream arg0) {		// TODO Auto-generated method stub			}	@Override	public void terminalStep(StateView arg0, HistoryView arg1) {		// TODO Auto-generated method stub			}}

