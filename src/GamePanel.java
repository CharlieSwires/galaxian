/* Copyright © 2026 Charlie's Tenacity Ltd. All rights reserved. */
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.nio.file.*;
import java.util.Random;
import javax.swing.*;

/** Swing double-buffered game surface. Simulation, drawing and input share the EDT. */
final class GamePanel extends JPanel {
    private static final long serialVersionUID=1L;
    private static final Color CYAN=new Color(87,225,244),GOLD=new Color(255,210,91),MUTED=new Color(143,161,190);
    final GameModel game;
    final HighScores scores;
    private final Sprites sprites;
    private final SoundEffects sounds;
    private final Timer timer;
    private final double[][] stars=new double[140][4];
    private boolean paused,muted,handlingGameOver,savingAllowed=true;
    private long previous;
    private double elapsed;
    String notice="";

    GamePanel(Path scoreFile,boolean audio,boolean animate)throws IOException {
        setPreferredSize(new Dimension(GameModel.WIDTH,GameModel.HEIGHT));
        setBackground(new Color(5,9,22));setFocusable(true);setDoubleBuffered(true);
        sprites=new Sprites();scores=new HighScores(scoreFile);
        try{scores.load();}catch(IOException ex){savingAllowed=false;notice="Saved scores could not be loaded; file left unchanged.";}
        sounds=audio?new SoundEffects():null;
        game=new GameModel(new Random(),effect->{if(sounds!=null)sounds.play(effect);});
        Random r=new Random(1509);
        for(double[] star:stars){star[0]=r.nextDouble()*GameModel.WIDTH;star[1]=r.nextDouble()*GameModel.HEIGHT;star[2]=12+r.nextDouble()*55;star[3]=r.nextInt(3);}
        bind("pressed A",()->game.left=true,true);bind("released A",()->game.left=false,true);
        bind("pressed LEFT",()->game.left=true,true);bind("released LEFT",()->game.left=false,true);
        bind("pressed D",()->game.right=true,true);bind("released D",()->game.right=false,true);
        bind("pressed RIGHT",()->game.right=true,true);bind("released RIGHT",()->game.right=false,true);
        bind("pressed SPACE",()->game.fire=true,true);bind("released SPACE",()->game.fire=false,true);
        bind("released M",()->{muted=!muted;if(sounds!=null)sounds.toggleMute();},false);
        bind("released P",()->{if(game.state==GameModel.State.PLAYING){paused=!paused;game.resetControls();}},false);
        bind("released ENTER",()->{
            if(game.state==GameModel.State.TITLE || game.state==GameModel.State.SCORES)start();
        },false);
        bind("released H",()->{
            if(game.state==GameModel.State.TITLE){game.state=GameModel.State.SCORES;}
            else if(game.state==GameModel.State.SCORES){game.state=GameModel.State.TITLE;}
        },false);
        bind("released ESCAPE",()->{
            if(game.state==GameModel.State.SCORES){game.state=GameModel.State.TITLE;}
            else if(game.state==GameModel.State.PLAYING){paused=!paused;game.resetControls();}
        },false);
        previous=System.nanoTime();
        timer=new Timer(16,e->tick());timer.setCoalesce(true);
        if(animate)timer.start();
    }
    private void bind(String key,Runnable command,boolean gameplay) {
        AbstractAction action=new AbstractAction(){
            @Override public boolean isEnabled(){return !handlingGameOver && (!gameplay || (game.state==GameModel.State.PLAYING && !paused));}
            @Override public void actionPerformed(ActionEvent e){if(isEnabled())command.run();}
        };
        KeyStroke base=KeyStroke.getKeyStroke(key);
        for(int mask:new int[]{0,InputEvent.SHIFT_DOWN_MASK}) {
            KeyStroke stroke=KeyStroke.getKeyStroke(base.getKeyCode(),mask,base.isOnKeyRelease());
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(stroke,key);
        }
        getActionMap().put(key,action);
    }
    void start(){paused=false;handlingGameOver=false;game.startGame();previous=System.nanoTime();requestFocusInWindow();}
    void lostFocus(){game.resetControls();if(game.state==GameModel.State.PLAYING)paused=true;}
    void close(){timer.stop();if(sounds!=null)sounds.close();}
    private void tick(){
        long now=System.nanoTime();double dt=Math.min(0.05,(now-previous)/1e9);previous=now;
        elapsed+=dt;
        if(!paused && !handlingGameOver) {
            for(double[] star:stars){star[1]+=star[2]*dt;if(star[1]>GameModel.HEIGHT)star[1]-=GameModel.HEIGHT;}
            game.update(dt);
            if(game.state==GameModel.State.GAME_OVER && game.stateTime>=1.7)finishGame();
        }
        repaint();
    }
    private void finishGame(){
        if(handlingGameOver)return;
        handlingGameOver=true;game.resetControls();
        // Dispatch after the timer event so the game-over frame can be painted first.
        SwingUtilities.invokeLater(()->{
            if(scores.qualifies(game.score)) {
                String name=JOptionPane.showInputDialog(this,"Top ten! Score: "+game.score+"\nName (up to 20 ASCII characters):","High score",JOptionPane.PLAIN_MESSAGE);
                // Cancel records an anonymous PLAYER entry, once for this game.
                scores.add(game.score,name);
                if(savingAllowed)try{scores.save();}catch(IOException ex){notice="Could not save scores: "+ex.getMessage();}
            }
            game.state=GameModel.State.SCORES;handlingGameOver=false;
            previous=System.nanoTime();requestFocusInWindow();repaint();
        });
    }
    @Override protected void paintComponent(Graphics graphics){
        super.paintComponent(graphics);
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            double scale=Math.min((double)getWidth()/GameModel.WIDTH,(double)getHeight()/GameModel.HEIGHT);
            g.translate((getWidth()-GameModel.WIDTH*scale)/2,(getHeight()-GameModel.HEIGHT*scale)/2);g.scale(scale,scale);
            g.clipRect(0,0,GameModel.WIDTH,GameModel.HEIGHT);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setColor(new Color(5,9,22));g.fillRect(0,0,GameModel.WIDTH,GameModel.HEIGHT);
            for(double[] star:stars){g.setColor(star[3]==0?new Color(43,67,98):star[3]==1?new Color(94,126,153):new Color(151,174,194));int size=star[2]>50?2:1;g.fillRect((int)star[0],(int)star[1],size,size);}
            if(game.state==GameModel.State.TITLE)drawTitle(g);
            else if(game.state==GameModel.State.SCORES)drawScores(g);
            else drawGame(g);
            if(paused)overlay(g,"PAUSED","P / ESC TO RESUME");
            if(!notice.isEmpty()) {text(g,notice,18,MUTED,18,GameModel.HEIGHT-12);}
        } finally {g.dispose();}
    }
    private void drawTitle(Graphics2D g){
        text(g,"CHARLIE'S ARCADE",13,CYAN,58,74);
        text(g,"GALAXIAN",64,Color.WHITE,54,151);
        text(g,"DIVE INTO THE STARFIELD",15,GOLD,58,183);
        g.setColor(new Color(35,53,78));g.drawLine(58,213,582,213);
        text(g,"THE FLEET DOESN'T STAY IN FORMATION.",15,MUTED,58,250);
        String[] labels={"RED COMMANDER","VIOLET ESCORT","GREEN SCOUT"};int[] types={3,2,1};
        for(int i=0;i<3;i++){
            int y=313+i*84;g.drawImage(sprites.frame(types[i],elapsed),66,y-20,48,48,null);
            text(g,labels[i],17,Color.WHITE,140,y);text(g,(i==0?"150":i==1?"80":"50")+" PTS / DOUBLE WHILE DIVING",12,MUTED,140,y+24);
        }
        roundBox(g,58,566,524,62,new Color(14,42,56));
        centered(g,"ENTER  /  START GAME",22,CYAN,606);
        centered(g,"A / D OR ARROWS TO MOVE     SPACE TO FIRE",13,Color.WHITE,664);
        centered(g,"P PAUSE     M SOUND     H HIGH SCORES",12,MUTED,693);
        centered(g,"GALAXIAN-INSPIRED  /  INDEPENDENT JAVA EDITION",10,MUTED,748);
    }
    private void drawGame(Graphics2D g){
        roundBox(g,18,18,604,66,new Color(9,19,35));
        text(g,"SCORE",11,MUTED,34,41);text(g,String.format("%06d",game.score),23,Color.WHITE,34,68);
        text(g,"BEST",11,MUTED,260,41);text(g,String.format("%06d",Math.max(scores.best(),game.score)),23,GOLD,260,68);
        text(g,"WAVE "+game.wave,12,CYAN,490,44);text(g,muted?"SOUND OFF":"SOUND ON",10,MUTED,490,65);
        for(GameModel.Alien a:game.aliens){
            AffineTransform old=g.getTransform();g.translate(a.x,a.y);
            if(a.diving && !a.returning)g.rotate(Math.sin(a.age*3)*0.55);
            g.drawImage(sprites.frame(a.type,game.time),-19,-19,38,38,null);g.setTransform(old);
        }
        for(GameModel.Shot s:game.shots){g.setColor(s.enemy?new Color(255,115,125):CYAN);g.fillRoundRect((int)s.x-2,(int)s.y-7,s.enemy?5:4,14,3,3);}
        if(game.state!=GameModel.State.GAME_OVER && (game.invulnerable<=0 || (int)(game.invulnerable*10)%2==0))ship(g,game.playerX,GameModel.HEIGHT-56,1);
        for(GameModel.Burst b:game.bursts){
            double t=b.age/0.55;g.setColor(new Color(255,193,92,(int)(255*(1-t))));
            for(int i=0;i<10;i++){double angle=i*Math.PI/5;int x=(int)(b.x+Math.cos(angle)*t*44),y=(int)(b.y+Math.sin(angle)*t*44);g.fillOval(x-3,y-3,6,6);}
        }
        g.setColor(new Color(35,53,78));g.drawLine(20,GameModel.HEIGHT-30,620,GameModel.HEIGHT-30);
        text(g,"LIVES "+game.lives,11,MUTED,25,GameModel.HEIGHT-12);
        text(g,"P PAUSE  /  M SOUND",10,MUTED,469,GameModel.HEIGHT-12);
        if(game.invulnerable>0 && game.state==GameModel.State.PLAYING)centered(g,"READY",15,CYAN,GameModel.HEIGHT-115);
        if(game.state==GameModel.State.WAVE_CLEAR)overlay(g,"WAVE CLEAR","NEXT WAVE INCOMING");
        if(game.state==GameModel.State.GAME_OVER)overlay(g,"GAME OVER","FINAL SCORE  "+game.score);
    }
    private void drawScores(Graphics2D g){
        text(g,"FLIGHT RECORDS",12,CYAN,58,72);text(g,"HIGH SCORES",44,Color.WHITE,54,129);
        text(g,"RANK",11,MUTED,60,188);text(g,"PILOT",11,MUTED,140,188);text(g,"SCORE",11,MUTED,492,188);
        for(int i=0;i<10;i++){
            int y=224+i*39;
            if(i%2==0)roundBox(g,48,y-23,544,35,new Color(10,23,40));
            Color c=i==0?GOLD:Color.WHITE;text(g,String.format("%02d",i+1),15,c,62,y);
            if(i<scores.entries().size()){
                HighScores.Entry e=scores.entries().get(i);text(g,e.name,15,c,140,y);
                String n=String.format("%06d",e.score);text(g,n,15,c,490,y);
            } else text(g,"---",15,MUTED,140,y);
        }
        roundBox(g,58,665,524,57,new Color(14,42,56));centered(g,"ENTER  /  NEW GAME",21,CYAN,702);
        centered(g,"H / ESC  BACK TO TITLE",12,MUTED,749);
    }
    private void overlay(Graphics2D g,String heading,String subtitle){
        roundBox(g,58,329,524,119,new Color(5,12,26,235));centered(g,heading,36,Color.WHITE,382);centered(g,subtitle,14,GOLD,417);
    }
    private static void ship(Graphics2D g,double x,double y,double scale){
        AffineTransform old=g.getTransform();g.translate(x,y);g.scale(scale,scale);
        g.setColor(CYAN);g.fillPolygon(new int[]{0,-8,-20,-20,-9,-5,5,9,20,20,8},new int[]{-22,-3,6,17,13,7,7,13,17,6,-3},11);
        g.setColor(Color.WHITE);g.fillPolygon(new int[]{0,-5,0,5},new int[]{-22,5,11,5},4);
        g.setColor(new Color(255,157,75));g.fillRect(-3,12,6,9);g.setTransform(old);
    }
    private static void roundBox(Graphics2D g,int x,int y,int w,int h,Color c){g.setColor(c);g.fillRoundRect(x,y,w,h,12,12);}
    private static void text(Graphics2D g,String s,int size,Color c,int x,int y){g.setFont(new Font(Font.MONOSPACED,Font.BOLD,size));g.setColor(c);g.drawString(s,x,y);}
    private static void centered(Graphics2D g,String s,int size,Color c,int y){g.setFont(new Font(Font.MONOSPACED,Font.BOLD,size));text(g,s,size,c,(GameModel.WIDTH-g.getFontMetrics().stringWidth(s))/2,y);}
}
