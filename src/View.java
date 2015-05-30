
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.net.URL;

public class View extends Panel
    implements MouseWheelListener, KeyListener, FocusListener, ActionListener

{


    public static Area theAreaDrawn;

    Point2D realMousePt;
    Point2D centerPt = new Point2D.Double(0,0);

    final mainthread game;
    final mainthread2 treeThread;
    boolean quit;
    final int screenwidth;
    final int screenheight;
    final Font screenFont = new Font(Font.MONOSPACED, Font.BOLD, 12);

    double pixelsData[];
    double dataMax = 0;

    int pixels[];
    BufferedImage render;
    Graphics2D rg;
    long fpsDraw;
    long fpsLogic;
    long framesThisSecondDrawn;
    long framesThisSecondLogic;
    long oneSecondAgo;

    long samplesThisFrame;
    double samplesNeeded;

    double zoom = 1.0;


    //user params

        boolean antiAliasing;

        boolean infoHidden;
        boolean imgSamples;
        boolean guidesHidden;
        boolean ptsHidden;
        boolean invertColors;
        int sampletotal;
        int iterations;
        int pointselected;


        boolean shiftDown;
        boolean ctrlDown;
        boolean altDown;
        static int mousex;
        static int mousey;
        int mouseScroll;


    int maxPoints;
    int maxLineLength;

    //drag vars
        int mousemode; //current mouse button

    boolean started;


    public View(){
        started=false;
        samplesThisFrame=0;
        oneSecondAgo =0;
        framesThisSecondDrawn = 0;
        framesThisSecondLogic = 0;
        altDown=false;
        ctrlDown=false;
        shiftDown=false;
        game = new mainthread();
        treeThread = new mainthread2();
        quit = false;
        antiAliasing = true;
        infoHidden = false;
        imgSamples = true;
        guidesHidden = false;
        ptsHidden = false;
        invertColors = false;
        screenwidth = 1024;
        screenheight = 512;
        pixels = new int[screenwidth * screenheight];

        pixelsData = new double[screenwidth * screenheight];
        sampletotal = 1000;
        iterations = 2;
        mousemode = 0;
        samplesNeeded = 1;
        maxLineLength = screenwidth;

        mouseScroll = 0;

        pointselected=-1;
    }

    public static void main(String[] args) {
        JFrame f = new JFrame();

        f.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.exit(0);
            };
        });

        View is = new View();
        is.setSize(is.screenwidth, is.screenheight); // same size as defined in the HTML APPLET

        Object rowData2[][] = { { "Row1-Column1", "Row1-Column2", "Row1-Column3"},
                                { "Row2-Column1", "Row2-Column2", "Row2-Column3"} };
        Object columnNames2[] = { "Column One", "Column Two", "Column Three"};
        JTable table2 = new JTable(rowData2, columnNames2);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                is, new JScrollPane(table2));

        splitPane.setDividerLocation(512);

        f.add(splitPane);

        f.pack();
        is.init();
        f.setSize(is.screenwidth, is.screenheight*2 + 20); // add 20, seems enough for the Frame title,
        f.show();
    }

    public void init() {
        System.setProperty("sun.java2d.opengl","True");
        start();
    }


    public void actionPerformed(ActionEvent e) {

    }

    public class mainthread extends Thread{
        public void run(){
            while(!quit){
                //framesThisSecondLogic++;
                Point2D mousePt = new Point2D.Float(mousex,mousey);
                Point2D _mousePt = new Point2D.Float(mousex,mousey);

                try {
                    _mousePt = cameraTransform.inverseTransform(mousePt,_mousePt);
                } catch (NoninvertibleTransformException e) {e.printStackTrace();}

                realMousePt = _mousePt;

                repaint();
                try {
                    sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public mainthread(){
        }
    }


    public class mainthread2 extends Thread{
        public void run(){
            while(!quit){
                framesThisSecondLogic++;
                Evolution.updateTree();
                try {
                    sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public mainthread2(){
        }
    }

    public void start(){
        //setCursor (Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        addMouseWheelListener(this);
        addKeyListener(this);
        render =  new BufferedImage(screenwidth, screenheight, BufferedImage.TYPE_INT_RGB); //createImage(screenwidth, screenheight);
        rg = (Graphics2D)render.getGraphics();

        clearframe();
        treeThread.start();
        game.start();

        started = true;
        //NodeWorld.resetWorld(1);
    }

    public void update(Graphics gr){

        paint((Graphics2D)gr);
    }

    AffineTransform cameraTransform = new AffineTransform();

    public void paint(Graphics2D gr){
        rg = (Graphics2D)render.getGraphics();

        rg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        framesThisSecondDrawn++;
        if(System.currentTimeMillis()- oneSecondAgo >=1000){
            oneSecondAgo = System.currentTimeMillis();
            fpsDraw = framesThisSecondDrawn;
            fpsLogic = framesThisSecondLogic;
            framesThisSecondDrawn =0;
            framesThisSecondLogic =0;
        }

        rg.setColor(new Color(0.1f,0.1f,0.1f));
        rg.fillRect(0, 0, screenwidth, screenheight);

        rg.setColor(Color.white);
        rg.setFont(screenFont);
        int row = 15;
        rg.drawString(Evolution.scoreString, 5, row*1);
        rg.drawString("FPS DRAW " + String.valueOf(fpsDraw) + " ", 5, row*1);
        rg.drawString("FPS LOGIC " + String.valueOf(fpsLogic), 5, row*2);
        rg.drawString("FAMILY " + String.valueOf(TransDescriptor.familyNumber) + " ", 5, row*3);
        try{
            int max = Math.min(50,Evolution.scoreList.size()-2);
            for(int scoreNum=0; scoreNum<max; scoreNum++){
                rg.drawString((1+scoreNum) + ": " + Evolution.scoreList.get(scoreNum).score +
                        " G " + Evolution.scoreList.get(scoreNum).generation +
                        " A " + Evolution.scoreList.get(scoreNum).attempts + " I " + Evolution.scoreList.get(scoreNum).myId, 5, row*(4+scoreNum));
            }

            max = Math.min(50,Evolution.globalScoreList.size()-2);
            for(int scoreNum=0; scoreNum<max; scoreNum++){
                TransDescriptor trans = Evolution.globalScoreList.get(scoreNum);
                rg.drawString((1+scoreNum) + ": " + trans.score + " F_"+trans.famNum+"_" + trans.myId + " g"+trans.generation, 500, row*(4+scoreNum));
            }
        }catch (Exception e){

        }


        cameraTransform = new AffineTransform();
        cameraTransform.translate(screenwidth/2,screenheight/2);
        cameraTransform.scale(zoom, zoom);
        cameraTransform.translate(-centerPt.getX(), -centerPt.getY());

        rg.setTransform(cameraTransform);
        rg.setStroke(new BasicStroke(1.0f / (float)zoom));


        if(theAreaDrawn!=null){


            rg.setColor(Color.darkGray);
            rg.draw(theAreaDrawn);

            rg.setColor(Color.red);
            if(Evolution.theRecordArea!=null)
            rg.draw(Evolution.theRecordArea);


        }

        rg.drawRect((int)centerPt.getX(),(int)centerPt.getY(),20,20);

        gr.drawImage(render, 0, 0, screenwidth, screenheight, this);
    }

    /*

    public Area buildTree(Shape _theShape, AffineTransform atAccum, ArrayList<Integer> depthsList){ //new ArrayList<Integer>(listA);
        //TODO depth per-transform: (much slower)
        Area result = new Area(atAccum.createTransformedShape(_theShape));

        int numTrans = depthsList.size();

        for(int i=0; i<numTrans; i++){
            if(depthsList.get(i)>0){
                ArrayList<Integer> copyList = new ArrayList<Integer>(depthsList);
                copyList.set(i,copyList.get(i)-1);
                result.add(buildTree(_theShape, MyTransformUtils.compose((AffineTransform)atAccum.clone(), trans.get(i)), copyList));
            }
        }

        return result;
    }

     */

    public void clearframe(){
        for(int a = 0; a < screenwidth * screenheight; a++){
            pixels[a] = 0xff000000;
            pixelsData[a] = 0;
        }

        samplesThisFrame=0;
        dataMax = 0;


    }


    public void mouseClicked(MouseEvent mouseevent){
    }

    public void mousePressed(MouseEvent e){

    }

    public void mouseReleased(MouseEvent e){
        //setCursor (Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        mousemode = 0;
    }

    public void mouseEntered(MouseEvent e){
        //setCursor (Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    public void mouseExited(MouseEvent e){
        //setCursor (Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    public void mouseDragged(MouseEvent e){

        clearframe();


    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        mouseScroll += e.getWheelRotation();
        if(e.getWheelRotation()>0){ //scroll down

                zoom *=0.99;

        }else{ //scroll up

                zoom /=0.99;

        }

        clearframe();

    }

    public void keyTyped(KeyEvent e){
    }

    public void keyPressed(KeyEvent e){

        if(e.getKeyChar() == 'a')
            centerPt.setLocation(centerPt.getX()-10,centerPt.getY());
        if(e.getKeyChar() == 'd')
            centerPt.setLocation(centerPt.getX() + 10, centerPt.getY());
        if(e.getKeyChar() == 'w')
            centerPt.setLocation(centerPt.getX(),centerPt.getY()-10);
        if(e.getKeyChar() == 's'){
            centerPt.setLocation(centerPt.getX(),centerPt.getY()+10);
        }
        if(e.getKeyChar() == 'r')
            Evolution.resetShape=true;
        clearframe();
    }

    public void keyReleased(KeyEvent e){
    }

    public void focusGained(FocusEvent focusevent){}
    public void focusLost(FocusEvent focusevent){}
}
