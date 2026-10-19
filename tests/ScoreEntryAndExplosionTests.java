import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Regression tests for actual score-form completion and the Saturn particle equations. */
public final class ScoreEntryAndExplosionTests {
    static int checks;
    static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
    static void key(GamePanel p,String key){p.getActionMap().get(key).actionPerformed(null);}
    public static void main(String[] args)throws Exception {
        SaturnExplosion e=new SaturnExplosion(300,350,new Random(1234));
        Random expected=new Random(1234);
        check(e.x.length==200,"Saturn particle count");
        for(int i=0;i<200;i++) {
            double bearing=2*Math.PI*expected.nextDouble(),speed=expected.nextDouble()*5;
            check(e.x[i]==300&&e.y[i]==350,"origin");
            check(e.dx[i]==speed*Math.sin(bearing)&&e.dy[i]==speed*Math.cos(bearing),"original velocity equations");
            check(Math.hypot(e.dx[i],e.dy[i])<5.00000001,"original speed range");
        }
        double[] original=e.x.clone();e.update(0.04);
        check(e.getCount()==253,"one Saturn step now fades two intensity levels");
        for(int i=0;i<200;i++)check(Math.abs(e.x[i]-original[i]-e.dx[i])<1e-10,"one Saturn step displacement");
        SaturnExplosion a=new SaturnExplosion(300,350,new Random(1234));
        SaturnExplosion b=new SaturnExplosion(300,350,new Random(1234));
        for(int i=0;i<25;i++)a.update(1.0/25);for(int i=0;i<60;i++)b.update(1.0/60);
        for(int i=0;i<200;i++)check(Math.abs(a.x[i]-b.x[i])<1e-8&&Math.abs(a.y[i]-b.y[i])<1e-8,"frame-rate-independent motion");
        check(a.getCount()==205&&b.getCount()==205,"frame-rate-independent fade");
        e.update(5.06);check(e.getCount()==0,"expired explosion");e.update(100);check(e.getCount()==0,"clamped lifetime");
        BufferedImage particle=new BufferedImage(32,32,BufferedImage.TYPE_INT_RGB);Graphics2D pg=particle.createGraphics();
        new SaturnExplosion(16,16,new Random(1)).draw(pg);pg.dispose();int lit=0;
        for(int y=0;y<32;y++)for(int x=0;x<32;x++)if(particle.getRGB(x,y)!=Color.BLACK.getRGB())lit++;
        check(lit==9,"three by three particle drawing");
        Path temp=Files.createTempDirectory("galaxian-form-test-");GamePanel[] holder=new GamePanel[1];
        SwingUtilities.invokeAndWait(()->{
            try {
                GamePanel p=new GamePanel(temp.resolve("scores.bin"),false,false);holder[0]=p;
                p.setSize(640,780);p.start();p.game.score=550;p.game.state=GameModel.State.GAME_OVER;
                p.finishGame();p.doLayout();p.scoreName.setText("Pilot One");
                check(p.scoreName.isShowing()==false,"test is headless");
                key(p,"pressed SPACE");check(!p.game.fire,"name form blocks game bindings");
                BufferedImage form=new BufferedImage(640,780,BufferedImage.TYPE_INT_RGB);Graphics2D g=form.createGraphics();p.printAll(g);g.dispose();
                ImageIO.write(form,"png",Paths.get("build","preview-name-entry.png").toFile());
                p.scoreName.postActionEvent();
                check(p.game.state==GameModel.State.SCORES,"Enter submits and leaves entry mode");
                check(p.scores.entries().size()==1&&p.scores.entries().get(0).name.equals("Pilot One"),"name saved once");
                p.submitHighScore();check(p.scores.entries().size()==1,"duplicate submission ignored");
                key(p,"released ENTER");check(p.game.state==GameModel.State.SCORES,"submission release does not skip scores");
            } catch(Exception ex){throw new RuntimeException(ex);}
        });
        // Flush the queued focus-restoration callbacks before simulating new input.
        SwingUtilities.invokeAndWait(()->{});
        SwingUtilities.invokeAndWait(()->{
            GamePanel p=holder[0];key(p,"released ENTER");check(p.game.state==GameModel.State.PLAYING,"new game after real name submission");
        });
        SwingUtilities.invokeAndWait(()->{});
        SwingUtilities.invokeAndWait(()->{
            GamePanel p=holder[0];double oldX=p.game.playerX;key(p,"pressed D");p.game.update(0.05);
            check(p.game.playerX>oldX,"movement after high-score restart");
            key(p,"released D");check(!p.game.right,"release after restart");
            key(p,"pressed SPACE");p.game.update(0.01);check(p.game.shots.stream().anyMatch(s->!s.enemy),"shooting after high-score restart");
            p.game.resetControls();p.game.score=650;p.game.state=GameModel.State.GAME_OVER;p.finishGame();p.scoreName.setText("Button Pilot");p.saveScore.doClick();
            check(p.game.state==GameModel.State.SCORES&&p.scores.entries().size()==2,"button submission also completes state transition");
            key(p,"released ENTER");check(p.game.state==GameModel.State.PLAYING,"Enter works immediately after mouse submission");
            p.game.bursts.add(new SaturnExplosion(320,370,new Random(123)));p.game.bursts.get(0).update(0.45);
            BufferedImage explosion=new BufferedImage(640,780,BufferedImage.TYPE_INT_RGB);Graphics2D g=explosion.createGraphics();p.paint(g);g.dispose();
            try{ImageIO.write(explosion,"png",Paths.get("build","preview-saturn-explosion.png").toFile());}catch(Exception ex){throw new RuntimeException(ex);}
            p.close();
        });
        Files.delete(temp.resolve("scores.bin"));Files.delete(temp);
        System.out.println("PASS: "+checks+" checks; score form submit/restart/movement/fire and Saturn's exact particle equations.");
    }
}
