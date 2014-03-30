package org.linesOfAction.client;

import org.game_api.GameApi;
import org.game_api.GameApi.UpdateUI;
import org.game_api.GameApi.Container;
import org.game_api.GameApi.Operation;
import org.game_api.GameApi.EndGame;
import org.game_api.GameApi.Set;
import org.game_api.GameApi.SetTurn;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

public class GamePresenter {
	
	public interface View {
		void setPresenter(GamePresenter gamepresenter);
		
		// present the animation of last move to the player 
		void setState(String moveFrom, String moveTo, int color);
		
		// present the initial state of the board to the player, and set the player's color
		void setInitialState(String color);
		
		/** ask the player to select a position on the board
		 *  if origin and destination are both empty, the position player selects will be an origin
		 *  if origin is not empty, player can either cancel the origin(by selecting the same position) or
		 *  select a destination(by selecting a different position)
		 */
		void choosePosition(String origin, String destination, ArrayList<String> possiblePosition);
		
		// alert the winner information (winner is true: "You win!"; winner is false: "You lose!")
		void declareWinner(boolean winner);
	}
	
	private final View view;
	private final Container container;
	
	private int[][] board = new int[8][8];
	private String yourPlayerId;
	private String opponentPlayerId;
	private String turnId;
	private String yourColor;
	private String opponentColor;
	
	
	private String origin;
	private String destination;
	
	private boolean initialFlag; 
	
	public GamePresenter(View view, Container container) {
		this.view = view;
		this.container = container;
		view.setPresenter(this);
	}

	
	public void updateUI(UpdateUI updateUI) {
		origin = "";
		destination = "";
		
		Map<String,Object> newState = updateUI.getState();
		List<Operation> lastMove = updateUI.getLastMove();
		yourPlayerId = updateUI.getYourPlayerId();
		String lastMovePlayerId = updateUI.getLastMovePlayerId();
		int lastMovePlayerIndex = updateUI.getPlayerIndex(lastMovePlayerId);
		int color = lastMovePlayerIndex == 0 ? 2 : 1;
		int yourPlayerIndex = updateUI.getPlayerIndex(yourPlayerId);
		int opponentPlayerIndex = 1 - yourPlayerIndex;
		yourColor = yourPlayerIndex == 0 ? "W"
		        : yourPlayerIndex == 1 ? "B" : "";
		opponentColor = yourColor.equals("W") ? "B"
				: yourColor.equals("B") ? "W" : "";
		
		if (newState.size() == 0) 
			initialFlag = true;
		else 
			initialFlag = false;
		
		if (-1 != yourPlayerIndex){
			opponentPlayerId = updateUI.getPlayerIds().get(opponentPlayerIndex);
			if (initialFlag) turnId = yourColor.equals("W") ? yourPlayerId : opponentPlayerId;
			else turnId = newState.get("turn").equals(yourColor) ? yourPlayerId : opponentPlayerId;
		}
		
		if (initialFlag){ //initial state
			view.setInitialState(yourColor);
			board = new int[][]{
		    		  { 0, 1, 1, 1, 1, 1, 1, 0 },
		    		  { 2, 0, 0, 0, 0, 0, 0, 2 },
		    		  { 2, 0, 0, 0, 0, 0, 0, 2 },
		    		  { 2, 0, 0, 0, 0, 0, 0, 2 },
		    		  { 2, 0, 0, 0, 0, 0, 0, 2 },
		    		  { 2, 0, 0, 0, 0, 0, 0, 2 },
		    		  { 2, 0, 0, 0, 0, 0, 0, 2 },
		    		  { 0, 1, 1, 1, 1, 1, 1, 0 }
		    		};
		}
		else{
			for (int i=0;i<8;i++) {
				for (int j=0;j<8;j++){
					switch ((String)newState.get(Character.toString((char)('1'+i)) + Character.toString((char)('A'+j)))) {
					case "O":	board[i][j] = 0;
								break;
					case "B":	board[i][j] = 1;
								break;
					case "W":	board[i][j] = 2;
								break;
					default:	board[i][j] = -1;
								break;
					}
					//System.out.print(board[i][j]);
				}
				//System.out.println("");
			}
			
			String moveFrom = ((Set)updateUI.getLastMove().get(0)).getKey();
			String moveTo = ((Set)updateUI.getLastMove().get(1)).getKey();
	
			if (updateUI.isViewer()) {
			      view.setState(moveFrom,moveTo,color);
			      return;
			    }
			if (updateUI.isAiPlayer()) {
				// TODO: implement AI in a later HW!
				//container.sendMakeMove(..);
				return;
			}
			
			view.setState(moveFrom,moveTo,color);
			
			if (lastMove.size() == 5){
				if (((EndGame)lastMove.get(4)).getPlayerIdToScore().get(yourPlayerId) != null)
					view.declareWinner(true);
				else 
					view.declareWinner(false);
				return;
			}
		}
		
		if (isMyTurn()){ 
			if (yourColor.equals("W")) choosePosition(1,2);
			if (yourColor.equals("B")) choosePosition(1,1);
		}
	}
	 
	private void choosePosition(int flag, int color){
		if (1 == flag) view.choosePosition(origin,destination,getPossibleOrigins(color));
		else{
			view.choosePosition(origin,destination,getPossibleDestinations(origin));
		}
	}
	
	// The view can only call this method if the presenter called {@link View#choosePosition}
	public void positionSelected(String position){
		check(isMyTurn());
		int color;
		if (yourColor.equals("W")) color=1; else color=2;
		if ("".equals(origin)) {origin = position;choosePosition(2,0);} // origin selected, continue;
		else{
			if (position.equals(origin)) {origin="";choosePosition(1,color);} // origin canceled, continue;
			else {
				destination = position;
				String winnerId = checkWin(board,origin,destination);
				//System.out.print("winneris: ");
				//System.out.println(winnerId);
				if (Integer.parseInt(winnerId)>=0) makeMoveWin(winnerId); // move made and someone wins the game
				else makeMoveContinue(); // move made and the game continue
			}
		}
	}
	
	private void makeMoveWin(String winnerId){
		List<Operation> theMove = new ArrayList<Operation>();
		theMove.add(new Set(origin,"0"));
		theMove.add(new Set(destination,yourColor));
		theMove.add(new Set("turn",opponentColor));
		theMove.add(new SetTurn(opponentPlayerId));
		theMove.add(new EndGame(winnerId));
		container.sendMakeMove(theMove);
	}
	
	private void makeMoveContinue(){
		List<Operation> theMove = new ArrayList<Operation>();
		theMove.add(new Set(origin,"0"));
		theMove.add(new Set(destination,yourColor));
		if (initialFlag){
			String[][] initialBoard = new String[][]{
		    		  { "0", "B", "B", "B", "B", "B", "B", "0" },
		    		  { "W", "0", "0", "0", "0", "0", "0", "W" },
		    		  { "W", "0", "0", "0", "0", "0", "0", "W" },
		    		  { "W", "0", "0", "0", "0", "0", "0", "W" },
		    		  { "W", "0", "0", "0", "0", "0", "0", "W" },
		    		  { "W", "0", "0", "0", "0", "0", "0", "W" },
		    		  { "W", "0", "0", "0", "0", "0", "0", "W" },
		    		  { "0", "B", "B", "B", "B", "B", "B", "0" },
		    		};
			for (int i=0;i<8;i++)
				for (int j=0;j<8;j++){
					if ((String.valueOf(i+1)+Character.toString((char)('A'+j))).equals(origin) || (String.valueOf(i+1)+Character.toString((char)('A'+j))).equals(destination))
						continue;
					theMove.add(new Set(String.valueOf(i+1)+Character.toString((char)('A'+j)),initialBoard[i][j]));
				}
		}
		theMove.add(new Set("turn",opponentColor));
		theMove.add(new SetTurn(opponentPlayerId));
		container.sendMakeMove(theMove);
	}
	
	// helper function, return the ID of the winner, -100 if no winner
	String checkWin(int[][] board,String origin, String destination){
		boolean youWin = connect(board,yourColor.equals("W")?2:1);
		boolean otherWin = connect(board,yourColor.equals("W")?1:2);
		if (!(youWin || otherWin)) return "-100";
		else{
			if (youWin) return yourPlayerId;
			else return opponentPlayerId;
		}
	}
	
	// helper function, check whether one's pieces are all connected
	boolean connect(int[][] board, int side){
		int[][] temp_board = new int[8][8];
		for (int i=0;i<8;i++)
			for (int j=0;j<8;j++)
				temp_board[i][j] = board[i][j];
		temp_board[origin.charAt(0)-'1'][origin.charAt(1)-'A'] = 0;
		temp_board[destination.charAt(0)-'1'][destination.charAt(1)-'A'] = "B".equals(yourColor) ? 1 : 2;
		int row=-1;
		int col=-1;
		for (int i=0;i<8;i++)
			for (int j=0;j<8;j++)
				if (side == temp_board[i][j]) {row=i;col=j;break;}
		if (col == -1) return false;
		else traverse(temp_board,side,row,col);
		col = -1; 
		row = -1;
		for (int i=0;i<8;i++)
			for (int j=0;j<8;j++)
				if (side == temp_board[i][j]) {row=i;col=j;break;}
		if (col == -1) return true;
		else return false;
	}
	
	// helper function, DFS
	void traverse(int[][] a,int side,int row,int col){
		if (row<0 || row>7 || col<0 || col>7) return;
		else if (a[row][col] != side) return;
		else {
			a[row][col] = -1;
			traverse(a,side,row+1,col-1);
			traverse(a,side,row+1,col);
			traverse(a,side,row+1,col+1);
			traverse(a,side,row,col-1);
			traverse(a,side,row,col+1);
			traverse(a,side,row-1,col-1);
			traverse(a,side,row-1,col);
			traverse(a,side,row-1,col+1);
		}
	}
	
	boolean isMyTurn(){
		if (yourPlayerId.equals(GameApi.VIEWER_ID) || !yourPlayerId.equals(turnId)) return false;
		else return true;
	}
	
	private void check(boolean val) {
		if (!val) {
	    	throw new IllegalArgumentException();
	    }
	}
	
	private ArrayList<String> getPossibleOrigins(int color){
		ArrayList<String> list = new ArrayList<String>();
		for (int i=0;i<8;i++)
			for (int j=0;j<8;j++){
				if (board[i][j] == color) {
					StringBuilder str = new StringBuilder(Character.toString((char)('1'+i)));
					str.append((char)('A'+j));
					list.add(str.toString());
				}
			}
		return list;
	}
	
	private ArrayList<String> getPossibleDestinations(String position){
		int row = position.charAt(0)-'1';
		int col = position.charAt(1)-'A'; 
		int color = board[row][col];
		ArrayList<String> list = new ArrayList<String>();
		
		// self
		
		// horizental
		int hori_count=0;
		for (int i=0;i<8;i++){
			if (board[row][i] == 1 || board[row][i] == 2) hori_count++;
		}
		for (int i=col-1;i>=0;i--){
			if (col - i <= hori_count && board[row][i] != color){
				StringBuilder str = new StringBuilder(Character.toString((char)('1'+row)));
				str.append((char)('A'+i));
				list.add(str.toString());
			}
			if (board[row][i] == 3-color) break;
		}
		for (int i=col+1;i<8;i++){
			if (i - col <= hori_count && board[row][i] != color){
				StringBuilder str = new StringBuilder(Character.toString((char)('1'+row)));
				str.append((char)('A'+i));
				list.add(str.toString());
			}
			if (board[row][i] == 3-color) break;
		}
		
		// vertical
		int verti_count=0;
		for (int i=0;i<8;i++){
			if (board[i][col] == 1 || board[i][col] == 2) verti_count++;
		}
		for (int i=row-1;i>=0;i--){
			if (row - i <= verti_count && board[i][col] != color){
				StringBuilder str = new StringBuilder(Character.toString((char)('1'+i)));
				str.append((char)('A'+col));
				list.add(str.toString());
			}
			if (board[i][col] == 3-color) break;
		}
		for (int i=row+1;i<8;i++){
			if (i - row <= verti_count && board[i][col] != color){
				StringBuilder str = new StringBuilder(Character.toString((char)('1'+i)));
				str.append((char)('A'+col));
				list.add(str.toString());
			}
			if (board[i][col] == 3-color) break;
		}
		
		// diagonal /
		int diag_count=0;
		for (int i=0;col+i<8 && row+i<8;i++){
			if (board[row+i][col+i] == 1 || board[row+i][col+i] == 2) diag_count++;
		}
		for (int i=1;col-i>=0 && row-i>=0;i++){
			if (board[row-i][col-i] == 1 || board[row-i][col-i] == 2) diag_count++;
		}
		for (int i=1;col+i<8 && row+i<8;i++){
			if (i<=diag_count && board[row+i][col+i] != color){
				StringBuilder str = new StringBuilder(Character.toString((char)('1'+row+i)));
				str.append((char)('A'+col+i));
				list.add(str.toString());
			}
			if (board[row+i][col+i] == 3 - color) break;
		}
		for (int i=1;col-i>=0 && row-i>=0;i++){
			if (i<=diag_count && board[row-i][col-i] != color){
				StringBuilder str = new StringBuilder(Character.toString((char)('1'+row-i)));
				str.append((char)('A'+col-i));
				list.add(str.toString());
			}
			if (board[row-i][col-i] == 3 - color) break;
		}
		
		// diagonal \
		diag_count=0;
		for (int i=0;col-i>=0 && row+i<8;i++){
			if (board[row+i][col-i] == 1 || board[row+i][col-i] == 2) diag_count++;
		}
		for (int i=1;col+i<8 && row-i>=0;i++){
			if (board[row-i][col+i] == 1 || board[row-i][col+i] == 2) diag_count++;
		}
		for (int i=1;col-i>=0 && row+i<8;i++){
			if (i<=diag_count && board[row+i][col-i] != color){
				StringBuilder str = new StringBuilder(Character.toString((char)('1'+row+i)));
				str.append((char)('A'+col-i));
				list.add(str.toString());
			}
			if (board[row+i][col-i] == 3 - color) break;
		}
		for (int i=1;col+i<8 && row-i>=0;i++){
			if (i<=diag_count && board[row-i][col+i] != color){
				StringBuilder str = new StringBuilder(Character.toString((char)('1'+row-i)));
				str.append((char)('A'+col+i));
				list.add(str.toString());
			}
			if (board[row-i][col+i] == 3 - color) break;
		}
		return list;
	}

}
