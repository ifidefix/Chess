package pieces;

import Players.Move;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import Engine.Engine;
import Players.PawnPromotion;

public class Pawn extends Piece {

    public Pawn(ChessPosition p, ChessColor c, int cellWidth, Engine e) {
        super(p, c, cellWidth, e);
        if (c == ChessColor.Black) {
            file += "bPawn.png";
        } else {
            file += "wPawn.png";
        }
        setupImg();
    }

    @Override
    public Piece copy() {
        return new Pawn(this.getPosition(), this.getColor(), cellWidth, e);
    }

    @Override
    public Set<Move> getMoves() {
        Set<ChessPosition> possibleChessPositions = new HashSet<>();
        Set<Move> possibleMoves = new HashSet<>();
        int x = getPosition().x;
        int y = getPosition().y;
        if (this.getColor() == ChessColor.White) {
            if (handler.getPiece(x, y + 1) == null) {
                if (y < Engine.CELL_AMOUNT) {   //not the last row
                    possibleChessPositions.add(new ChessPosition(x, y + 1, canvas));
                }
                if (y == 2 && handler.getPiece(x, y + 2) == null) { //if on home row and no blocking piece, add the double step
                    possibleChessPositions.add(new ChessPosition(x, y + 2, canvas));
                }
            }
            //captures
            Piece p = handler.getPiece(x - 1, y + 1);
            if (p != null) {
                if (p.getColor() != this.getColor()) {
                    possibleChessPositions.add(new ChessPosition(x - 1, y + 1, canvas));
                }
            }
            p = handler.getPiece(x + 1, y + 1);
            if (p != null) {
                if (p.getColor() != this.getColor()) {
                    possibleChessPositions.add(new ChessPosition(x + 1, y + 1, canvas));
                }
            }
            //en-passent
            if (y == 5) {
                Move lastMove = e.getLastMove();
                Piece pawn = handler.getPiece(x+1, y);
                if (pawn != null) {
                    if (pawn.equals(lastMove.getPiece())) { //if the piece next has moved last
                        if (new ChessPosition(x + 1, y + 2, canvas).equals(lastMove.getStartPosition())) {  //and it did a double step
                            possibleMoves.add(new Move(this, new ChessPosition(x + 1, y + 1, canvas), pawn, e));
                        }
                    }
                }
                pawn = handler.getPiece(x - 1, y);
                if (pawn != null) {
                    if (pawn.equals(lastMove.getPiece())) {
                        if (new ChessPosition(x - 1, y + 2, canvas).equals(lastMove.getStartPosition())) {
                            possibleMoves.add(new Move(this, new ChessPosition(x - 1, y + 1, canvas), pawn, e));
                        }
                    }
                }
            }
        } else {    //color is black
            if (handler.getPiece(x, y-1) == null) {
                if (y > 0) {    //not the last row
                    possibleChessPositions.add(new ChessPosition(x, y-1, canvas));
                }
                if (y == 7 && handler.getPiece(x, y - 2) == null) { //if on home row and no blocking piece, add the double step
                    possibleChessPositions.add(new ChessPosition(x, y - 2, canvas));
                }
            }
            //captures
            Piece p = handler.getPiece(x-1, y - 1);
            if (p != null) {
                if (p.getColor() != this.getColor()) {
                    possibleChessPositions.add(new ChessPosition(x -1, y - 1, canvas));
                }
            }
            p = handler.getPiece(x + 1, y - 1);
            if (p != null) {
                if (p.getColor() != this.getColor()) {
                    possibleChessPositions.add(new ChessPosition(x + 1, y - 1, canvas));
                }
            }
            //en-passent
            if (y == 4) {
                Move lastMove = e.getLastMove();
                Piece pawn = handler.getPiece(x+1, y);
                if (pawn != null) {
                    if (pawn.equals(lastMove.getPiece())) { //if the piece next has moved last
                        if (new ChessPosition(x + 1, y - 2, canvas).equals(lastMove.getStartPosition())) {  //and it did a double step
                            possibleMoves.add(new Move(this, new ChessPosition(x + 1, y - 1, canvas), pawn, e));
                        }
                    }
                }
                pawn = handler.getPiece(x-1, y);
                if (pawn != null) {
                    if (pawn.equals(lastMove.getPiece())) {
                        if (new ChessPosition(x - 1, y - 2, canvas).equals(lastMove.getStartPosition())) {
                            possibleMoves.add(new Move(this, new ChessPosition(x - 1, y - 1, canvas), pawn, e));
                        }
                    }
                }
            }
        }
        //pawn promotions:
        Set<ChessPosition> promotions;
        if (this.getColor() == ChessColor.White) {
            promotions = possibleChessPositions.stream().filter(p -> p.y == 8).collect(Collectors.toSet());
        } else { //black
            promotions = possibleChessPositions.stream().filter(p -> p.y == 1).collect(Collectors.toSet());
        }
        possibleChessPositions.removeAll(promotions);
        Set<Move> promotionMoves = new HashSet<>();
        for (ChessPosition p : promotions) {
            promotionMoves.addAll(getPromotions(p));
        }
        possibleMoves.addAll(possibleChessPositions.stream().map(m -> new Move(this, m, handler.getPiece(m), e)).collect(Collectors.toSet()));
        possibleMoves.addAll(promotionMoves);
        return possibleMoves;
    }

    private Set<Move> getPromotions(ChessPosition p) {
        Queen queen = new Queen(p, this.getColor(), cellWidth, e);
        Knight knight = new Knight(p, this.getColor(), cellWidth, e);
        Rook rook = new Rook(p, this.getColor(), cellWidth, e);
        Bishop bishop = new Bishop(p, this.getColor(), cellWidth, e);
        Set<Move> promotions = new HashSet<>();
        promotions.add(new PawnPromotion(this, p, handler.getPiece(p), e, queen));
        promotions.add(new PawnPromotion(this, p, handler.getPiece(p), e, knight));
        promotions.add(new PawnPromotion(this, p, handler.getPiece(p), e, rook));
        promotions.add(new PawnPromotion(this, p, handler.getPiece(p), e, bishop));
        return promotions;
    }
}
