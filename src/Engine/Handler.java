package Engine;

import GUI.ChessCanvas;
import Players.Castling;
import Players.Move;
import Players.PawnPromotion;
import pieces.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.awt.*;
import java.util.*;

/**
 * Handles all the game objects
 */
public class Handler implements Cloneable {
    //needs to be volatile, because both the game loop and window updates can access at the same time.
    //This array contains all the pieces. It's ordering is from 1 to 8 on the x axis and 1 to 8 on the y axis, viewed
    //from white's perspective. (1,1) would be the white rook in the starting position
    volatile private Piece[][] pieces =  new Piece[Engine.CELL_AMOUNT+1][Engine.CELL_AMOUNT+1];
    private boolean whiteTurn = true;

    int amountOfReversableMoves = 0; //to keep track of the fifty move rule

    //Whether or not castlings are possible (only based on movement of king and rook)
    private boolean[] castlingsPossible = {true, true, true, true};

    private ChessCanvas canvas;
    private Engine e;
    private Move lastMove; //used for en-passent

    public Handler(Engine e) {
        this.e = e;
        canvas = e.getCanvas();
        initialize();
    }

    /**
     * A copy constructor.
     */
    public Handler(Handler h) {
        this.e = h.e;
        this.canvas = h.canvas;
        this.pieces = new Piece[Engine.CELL_AMOUNT+1][Engine.CELL_AMOUNT+1];
        for (int x = 1; x < this.pieces.length; x++) {
            for (int y = 1; y < this.pieces.length; y++) {
                if (h.pieces[x][y] != null) {
                    this.pieces[x][y] = h.pieces[x][y].clone(this);
                }
            }
        }
        this.whiteTurn = h.whiteTurn;
        this.castlingsPossible = h.castlingsPossible.clone();
        this.lastMove = h.lastMove; //not clone, so that all handlers share the same set of moves that are done
        this.amountOfReversableMoves = h.amountOfReversableMoves;
    }

    /**
     * Initializes the game. To be called on every game start.
     */
    public synchronized void initialize() {
        pieces = new Piece[Engine.CELL_AMOUNT+1][Engine.CELL_AMOUNT+1];
        addPiece(new Rook(ChessColor.White, e,this), new ChessPosition(1,1, canvas));
        addPiece(new Rook( ChessColor.White, e,this), new ChessPosition(8, 1, canvas));
        addPiece(new Knight( ChessColor.White, e,this), new ChessPosition(2, 1, canvas));
        addPiece(new Knight( ChessColor.White, e,this), new ChessPosition(7, 1, canvas));
        addPiece(new Bishop( ChessColor.White, e,this), new ChessPosition(3, 1, canvas));
        addPiece(new Bishop( ChessColor.White, e,this), new ChessPosition(6, 1, canvas));
        addPiece(new King( ChessColor.White, e,this), new ChessPosition(5, 1, canvas));
        addPiece(new Queen( ChessColor.White, e,this), new ChessPosition(4, 1, canvas));
        for (int i = 1; i <= 8; i++) {
            addPiece(new Pawn( ChessColor.White, e,this), new ChessPosition(i, 2, canvas));
        }
        addPiece(new Rook(ChessColor.Black, e,this), new ChessPosition(1,8, canvas));
        addPiece(new Rook( ChessColor.Black, e, this), new ChessPosition(8, 8, canvas));
        addPiece(new Knight( ChessColor.Black, e,this), new ChessPosition(2, 8, canvas));
        addPiece(new Knight( ChessColor.Black, e,this), new ChessPosition(7, 8, canvas));
        addPiece(new Bishop( ChessColor.Black, e,this), new ChessPosition(3, 8, canvas));
        addPiece(new Bishop( ChessColor.Black, e,this), new ChessPosition(6, 8, canvas));
        addPiece(new King( ChessColor.Black, e,this), new ChessPosition(5, 8, canvas));
        addPiece(new Queen( ChessColor.Black, e,this), new ChessPosition(4, 8, canvas));
        for (int i = 1; i <= 8; i++) {
            addPiece(new Pawn( ChessColor.Black, e,this), new ChessPosition(i, 7, canvas));
        }
        lastMove = null;
        castlingsPossible = new boolean[]{true, true, true, true};
        whiteTurn = true;
        amountOfReversableMoves = 0;
    }

    public synchronized void addPiece(Piece p, ChessPosition l) {
        pieces[l.x][l.y] = p;
    }

    public synchronized void removePiece(ChessPosition l) {
        pieces[l.x][l.y] = null;
    }

    public synchronized Piece getPiece(ChessPosition position) {
        return getPiece(position.x, position.y);
    }

    public Piece getPiece(int x, int y) {
        if (x > Engine.CELL_AMOUNT || y > Engine.CELL_AMOUNT || x < 1 || y < 1) {
            return null; //there are no pieces out of bounds.
        }
        return pieces[x][y];
    }

    /**
     * Get all pieces.
     */
    public synchronized Piece[][] getPieces() {
        return pieces.clone();
    }

    /**
     * Get all pieces of a specific color
     */
    public synchronized Set<Piece> getPieces(ChessColor c) {
        Set<Piece> returnSet = new HashSet<>();
        for (Piece[] row : pieces) {
            for (Piece p : row) {
                if (p.getColor() == c) {
                    returnSet.add(p);
                }
            }
        }
        return returnSet;
    }

    public Set<Piece> getWhitePieces() {
        return getPieces(ChessColor.White);
    }

    public Set<Piece> getBlackPieces() {
        return getPieces(ChessColor.Black);
    }

    public synchronized Set<Piece> getOppositeColorPieces(ChessColor c) {
        if (c == ChessColor.White) {
            return getBlackPieces();
        } else {
            return getWhitePieces();
        }
    }

    public synchronized King getKing(ChessColor c) {
        for (Piece[] row : pieces) {
            for (Piece p : row) {
                if (p instanceof King && p.getColor() == c) {
                    return (King)p;
                }
            }
        }
        throw new IllegalStateException("There is no " + c + " king");
    }

    public synchronized King getWhiteKing() {
        return getKing(ChessColor.White);
    }

    public synchronized King getBlackKing() {
        return getKing(ChessColor.Black);
    }

    public synchronized ChessPosition getKingPosition(ChessColor c) {
        for (int x = 1; x < pieces.length; x++) {
            for (int y = 1; y < pieces.length; y++) {
                Piece king = pieces[x][y];
                if (king instanceof King && king.getColor() == c) {
                    return new ChessPosition(x, y, canvas);
                }
            }
        }
        throw new IllegalStateException("There are no kings to be found.");
    }

    public synchronized ChessPosition getBlackKingPosition() {
        return getKingPosition(ChessColor.Black);
    }

    public synchronized ChessPosition getWhiteKingPosition() {
        return getKingPosition(ChessColor.White);
    }

    public synchronized boolean whiteKingChecked() {
        return getWhiteKing().isChecked(getWhiteKingPosition());
    }

    public synchronized boolean blackKingChecked() {
        return getBlackKing().isChecked(getBlackKingPosition());
    }

    /**
     * Changes the turn. If it was white's Turn, it is now black's turn and the other way arround.
     */
    public void changeTurn() {
        whiteTurn = !whiteTurn;
    }

    public synchronized boolean isWhiteToMove() {
        return whiteTurn;
    }

    public boolean blackMated() {
        return getBlackKing().isMated(getBlackKingPosition());
    }

    public boolean whiteMated() {
        return getWhiteKing().isMated(getWhiteKingPosition());
    }

    public boolean whiteStaleMated() {
        return getWhiteKing().isStaleMated(getWhiteKingPosition());
    }

    public boolean blackStaleMated() {
        return getBlackKing().isStaleMated(getBlackKingPosition());
    }

    public boolean isLastMove() {
        return lastMove != null;
    }

    public void undoLastMove() {
        if (lastMove != null) {
            this.undo(lastMove);
        }
    }

    public void setLastMove(Move m) {
        this.lastMove = m;
    }

    public Move getLastMove() {
        return this.lastMove;
    }

    public synchronized void execute(Move m, boolean boardRedrawRequired) {
        if (m.isExecuted()) throw new IllegalArgumentException();
        m.setExecuted(true);
        ChessPosition start = m.getStartPosition();
        ChessPosition end = m.getEndPosition();

        m.setCastlings(castlingsPossible); //set the state of the castlings before the move was executed.
        checkAndSetCastlings(m);
        //remove the captured piece
        ChessPosition capturedPiecePosition = m.getCapturedPiecePosition();
        if (capturedPiecePosition != null) { //in case of a castling
            pieces[capturedPiecePosition.x][capturedPiecePosition.y] = null;
        }
        m.setAmountOfReversableMovesBeforeThisMove(this.amountOfReversableMoves);
        if (!(m.getCapturedPiece() != null || pieces[start.x][start.y] instanceof Pawn)) {
            //a move is reversable iff it is a move by a pieces (except pawns) to an empty target square.
            //i.e. the complement of all capture moves and all pawn moves.
            this.amountOfReversableMoves++;
        } else {
            this.amountOfReversableMoves = 0; //reset the counter if it was not a reversable move.
        }


        //move piece to new position
        pieces[end.x][end.y] = pieces[start.x][start.y];
        //set old position to null
        pieces[start.x][start.y] = null;
        if (m instanceof Castling) { //also move the rook
            ChessPosition rookStart = ((Castling) m).getRookStartPosition();
            ChessPosition rookEnd = ((Castling) m).getRookEndPosition();
            pieces[rookEnd.x][rookEnd.y] = pieces[rookStart.x][rookStart.y];
            pieces[rookStart.x][rookStart.y] = null;
        } else if (m instanceof PawnPromotion) { //promote the piece
            pieces[end.x][end.y] = ((PawnPromotion) m).getPromotionPiece();
        }
        this.setLastMove(m);
        this.changeTurn();
        if (boardRedrawRequired) {  //board redraw is done here instead of after an execute in run() of the engine to avoid
                                    //threading issues where the other player is already trying things out and moving pieces
                                    //before the redraw has happened. This will result in a redraw with pieces in undefined places.
                                    //This issue won't arise here, because this method is synchronised, so the other player
                                    //will have to wait for the board to finish drawing.
            e.getCanvas().requestBoardRepaint();
            e.getCanvas().repaint();
        }
    }

    public synchronized void undo(Move m) {
        if (!m.isExecuted()) throw new IllegalArgumentException();
        m.setExecuted(false);
        ChessPosition start = m.getStartPosition();
        ChessPosition end = m.getEndPosition();
        //move piece to old position
        pieces[start.x][start.y] = pieces[end.x][end.y];
        //remove piece from old position
        pieces[end.x][end.y] = null;
        //put back the captured piece
        ChessPosition capturedPiecePosition = m.getCapturedPiecePosition();
        if (capturedPiecePosition != null) { //in case of castling
            pieces[capturedPiecePosition.x][capturedPiecePosition.y] = m.getCapturedPiece();
        }
        if (m instanceof Castling) { //also move the rook back
            ChessPosition rookStart = ((Castling) m).getRookStartPosition();
            ChessPosition rookEnd = ((Castling) m).getRookEndPosition();
            pieces[rookStart.x][rookStart.y] = pieces[rookEnd.x][rookEnd.y]; //move the rook back
            pieces[rookEnd.x][rookEnd.y] = null; //delete the old rook
        } else if (m instanceof PawnPromotion) { //place the pawn back
            pieces[start.x][start.y] = ((PawnPromotion) m).getPawn();
        }

        castlingsPossible = m.getCastlingsPossible(); //this assumes that the values have been correctly set before the move was executed.
        amountOfReversableMoves = m.getAmountOfReversableMovesBeforeThisMove(); // """
        this.setLastMove(m.getPreviousLastMove());
        this.changeTurn();
    }

    public void checkAndSetCastlings(Move m) {
        ChessPosition start = m.getStartPosition();
        ChessPosition end = m.getEndPosition();
        Piece startPiece = getPiece(start);
        if (startPiece instanceof King) { //king is moving
            if (startPiece.getColor() == ChessColor.White) { //white king is moving
                castlingsPossible[0] = false; //no white castlings possible anymore
                castlingsPossible[1] = false;
            } else { //black king is moving
                castlingsPossible[2] = false; //no black castlings possible anymore
                castlingsPossible[3] = false;
            }
        } else if (startPiece instanceof Rook) {
            if (startPiece.getColor() == ChessColor.White) { //A white rook is moving
                if (start.equals(new ChessPosition(1, 1, canvas))) { //left white rook is moving
                    castlingsPossible[1] = false; //no white long castling possible anymore
                } else if (start.equals(new ChessPosition(8, 1, canvas))) {
                    castlingsPossible[0] = false; //no white short castling possible anymore
                }
            } else { //black rook is moving
                if (start.equals(new ChessPosition(1, 8, canvas))) { //left black rook is moving
                    castlingsPossible[3] = false; //no black long castling possible anymore
                } else if (start.equals(new ChessPosition(8, 8, canvas))) { //right black rook is moving
                    castlingsPossible[2] = false; //no black short castling possible anymore.
                }
            }
        }
    }

    /**
     * Return whether or not a castling is possible. It only checks if rook/king have moved yet.
     */
    public boolean whiteShortCastlingPossible() {
        return castlingsPossible[0];
    }

    /**
     * Return whether or not a castling is possible. It only checks if rook/king have moved yet.
     */
    public boolean whiteLongCastlingPossible() {
        return castlingsPossible[1];
    }

    /**
     * Return whether or not a castling is possible. It only checks if rook/king have moved yet.
     */
    public boolean blackShortCastlingPossible() {
        return castlingsPossible[3];
    }

    /**
     * Return whether or not a castling is possible. It only checks if rook/king have moved yet.
     */
    public boolean blackLongCastlingPossible() {
        return castlingsPossible[2];
    }

    public synchronized void drawPieces(Graphics g) {
        for (int x = 1; x < pieces.length; x++) {
            for (int y = 1; y < pieces.length; y++) {
                Piece p = pieces[x][y];
                if (p != null) {
                    pieces[x][y].draw(g, new ChessPosition(x, y, canvas));
                }
            }
        }
    }

    public synchronized Set<Move> getOppositeColorMoves(ChessColor c) {
        Set<Move> moves = new HashSet<>();
        for (int x = 1; x < pieces.length; x++) {
            for (int y = 1; y < pieces.length; y++) {
                Piece p = pieces[x][y];
                if (p != null) {
                    if (p.getColor() != c) {
                        moves.addAll(p.getMoves(new ChessPosition(x, y, canvas)));
                    }
                }
            }
        }
        return moves;
    }

    public synchronized Set<Move> getMovesWithCheck(ChessColor c) {
        Set<Move> moves = new HashSet<>();
        for (int x = 1; x < pieces.length; x++) {
            for (int y = 1; y < pieces.length; y++) {
                Piece p = pieces[x][y];
                if (p != null && p.getColor() == c) {
                    moves.addAll(p.getMovesWithCheck(new ChessPosition(x, y, canvas)));
                }
            }
        }
        return moves;
    }

    public boolean fiftyMoves() {
        return amountOfReversableMoves >= 50;
    }

    public Handler clone() {
        return new Handler(this);
    }
}
