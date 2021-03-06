package GUI;

import Engine.Handler;
import Players.Move;
import pieces.ChessColor;
import pieces.ChessPosition;
import pieces.King;
import pieces.Piece;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * Handles all graphic related stuff
 */
public class ChessCanvas extends JPanel {

    private Color darkBrown = new Color(139,69,19);
    private Color lightBrown = new Color(245,222,179);
    public static int cellWidth = 100;
    private Handler handler;
    private Piece selected;
    private ChessPosition selectedPiecePosition;

    private ChessPosition mousePointer; //the pointer on the board pointing to a cell
    private Point mousePosition;    //the actual mouse position

    private boolean boardRepaintNeeded = true;
    private Image boardAndPiecesBackup;

    public ChessCanvas(Handler handler) {
        this.setPreferredSize(new Dimension(8 * cellWidth, 8 * cellWidth));
        this.handler = handler;
    }

    @Override
    public void paintComponent(Graphics g) {
        //Implement double buffering
        Image offscreen = null;
        Dimension d = this.getSize();
        offscreen = createImage(d.width, d.height);
        Graphics offg = offscreen.getGraphics();
        offg.setColor(Color.BLACK);
        offg.fillRect(0,0, d.width, d.height);

        if (boardRepaintNeeded) {
            redrawBoard(offg);  //draw the board on the original offscreen canvas
            boardAndPiecesBackup = createImage(d.width, d.height);
            Graphics backupGraphics = boardAndPiecesBackup.getGraphics();
            redrawBoard(backupGraphics);    //and draw the board on the backup
            boardRepaintNeeded = false;
        } else {
            offg.drawImage(boardAndPiecesBackup, 0, 0, this);
        }


        drawPointer(offg);
        drawSelectedPiece(offg);
        //copy to real screen
        g.drawImage(offscreen, 0, 0, this);
    }

    private void redrawBoard(Graphics g) {
        //redraw
        paintField(g);
        //draw king red if checked
        paintKingCheck(g);
        //draw last move
        paintLastMove(g);
        //draw the pieces
        handler.drawPieces(g);
    }

    public void setMousePointer(ChessPosition mousePointer) {
        this.mousePointer = mousePointer;
    }

    public void setMousePosition(Point p) {
        this.mousePosition = p;
    }

    public void paintLastMove(Graphics g) {
        Move lastMove = handler.getLastMove();
        if (lastMove == null) return; //do nothing if there is no lastMove
        Color green = new Color(94, 224, 42);
        g.setColor(green);
        Point drawPoint = lastMove.getStartPosition().getPositionOnCanvas();
        //This is a square
        //noinspection SuspiciousNameCombination
        g.fillRect(drawPoint.x, drawPoint.y, cellWidth, cellWidth);
        g.setColor(Color.black);
        //noinspection SuspiciousNameCombination
        g.drawRect(drawPoint.x, drawPoint.y, cellWidth, cellWidth);

        g.setColor(green);
        drawPoint = lastMove.getEndPosition().getPositionOnCanvas();
        //noinspection SuspiciousNameCombination
        g.fillRect(drawPoint.x, drawPoint.y, cellWidth, cellWidth);
        g.setColor(Color.black);
        //noinspection SuspiciousNameCombination
        g.drawRect(drawPoint.x, drawPoint.y, cellWidth, cellWidth);
    }

    /**
     * Paints the field
     * @param g
     */
    private void paintField(Graphics g) {
        for (int x = 0; x < 8 * cellWidth; x += cellWidth) {
            for (int y = 0; y < 8 * cellWidth; y += cellWidth) {
                if ((x+y) % 2 != 0) {       //because they start at 0. It would have been equal if it starts at 1. See also ChessPosition
                    g.setColor(darkBrown);
                } else {
                    g.setColor(lightBrown);
                }
                //This is a square
                //noinspection SuspiciousNameCombination
                g.fillRect(x, y, cellWidth, cellWidth);
                g.setColor(Color.black);
                //noinspection SuspiciousNameCombination
                g.drawRect(x, y, cellWidth, cellWidth);
            }
        }
    }

    /**
     * Draws the mouse pointer
     * @param g
     */
    private void drawPointer(Graphics g) {
        if (mousePointer == null) return;
        int thickness = 5;
        Graphics2D g2 = (Graphics2D) g;
        Point p = mousePointer.getPositionOnCanvas();
        g2.setColor(Color.BLUE);
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(thickness));
        //This is a square
        //noinspection SuspiciousNameCombination
        g.drawRoundRect(p.x, p.y, cellWidth, cellWidth, thickness, thickness);
        g2.setStroke(oldStroke);
    }

    /**
     * Draws the piece that should follow the pointer if there is one.
     * @param g
     */
    private void drawSelectedPiece(Graphics g) {
        if (mousePosition == null || selected == null) return;
        //Draw over original piece
        if (selectedPiecePosition.getColor() == ChessColor.Black) {
            g.setColor(darkBrown);
        } else {
            g.setColor(lightBrown);
        }
        Point drawPosition = selectedPiecePosition.getPositionOnCanvas();
        g.fillRect(drawPosition.x, drawPosition.y, cellWidth, cellWidth);

        //draw the pointer again over this overlay
        drawPointer(g);
        //draw the possible moves
        drawPossibleMoves(g);

        //draw piece on mouse location and transparent on original location
        Image img = selected.getImg();
        float alpha = 0.3f; //transparency
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setComposite(ac); //set transparency
        Point drawPoint = this.selectedPiecePosition.getPositionOnCanvas();
        g2d.drawImage(img, drawPoint.x, drawPoint.y, cellWidth, cellWidth, null);
        //change transparency back before drawing the piece at the mousePosition
        alpha = 1;
        ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        g2d.setComposite(ac);
        g2d.drawImage(img, (int)mousePosition.getX() - cellWidth/2, (int)mousePosition.getY() - cellWidth/2, cellWidth, cellWidth, null);
    }

    /**
     * A helper method that draws a red background if the king is checked.
     * @param g
     */
    private void paintKingCheck(Graphics g) {
        King whiteKing = handler.getWhiteKing();
        if (whiteKing == null) throw new IllegalStateException("There is no white king.");
        if (handler.whiteKingChecked()) {
            g.setColor(Color.RED);
            Point drawPoint = handler.getWhiteKingPosition().getPositionOnCanvas();
            g.fillRect(drawPoint.x, drawPoint.y, cellWidth, cellWidth);
        }
        King blackKing = handler.getBlackKing();
        if (blackKing == null) throw new IllegalStateException("There is no black king");
        if (handler.blackKingChecked()) {
            g.setColor(Color.RED);
            Point drawPoint = handler.getBlackKingPosition().getPositionOnCanvas();
            //This is a square
            //noinspection SuspiciousNameCombination
            g.fillRect(drawPoint.x, drawPoint.y, cellWidth, cellWidth);
        }
    }

    /**
     * A helper method that draws the possible moves of the currently selected piece.
     * @param g
     */
    private void drawPossibleMoves(Graphics g) {
        if (selected == null) return;
        Set<Move> moves = selected.getMovesWithCheck(selectedPiecePosition);
        moves.forEach(m -> drawDot(g, m.getEndPosition()));
    }

    /**
     * Draw a dot on a specific position
     * @param g
     * @param m
     */
    private void drawDot(Graphics g, ChessPosition m) {
        int circleRadius = cellWidth / 7;
        Point drawPoint = m.getPositionOnCanvas();
        g.setColor(Color.CYAN);
        g.fillOval(drawPoint.x + cellWidth/2 - circleRadius, drawPoint.y + cellWidth/2 - circleRadius, circleRadius*2, circleRadius*2);
    }

    public void setCellWidth(int cellWidth) {
        ChessCanvas.cellWidth = cellWidth;
    }

    public void setSelectedPiece(Piece p, ChessPosition position) {
        this.selected = p;
        this.selectedPiecePosition = position;
    }

    /**
     * Request that the board should be repainted. The repaint will be done in the next call of paintComponent
     */
    public void requestBoardRepaint() {
        boardRepaintNeeded = true;
    }

}
