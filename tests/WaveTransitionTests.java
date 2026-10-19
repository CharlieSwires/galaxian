import java.nio.file.*;
import javax.swing.*;
import java.util.Random;
public final class WaveTransitionTests {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void key(GamePanel p,String key){p.getActionMap().get(key).actionPerformed(null);}
    static void clearWave(GamePanel p){p.game.aliens.clear();p.advanceGame(0.01);check(p.game.state==GameModel.State.WAVE_CLEAR,"wave-clear state");}
    public static void main(String[] args)throws Exception {
        GamePanel[] holder=new GamePanel[1];Path directory=Files.createTempDirectory("galaxian-wave-test-");
        SwingUtilities.invokeAndWait(()->{
            try {
                GamePanel p=new GamePanel(directory.resolve("scores.bin"),false,false);holder[0]=p;
                p.start();key(p,"pressed A");key(p,"pressed SPACE");clearWave(p);p.advanceGame(2.2);
                check(p.game.wave==2&&p.game.state==GameModel.State.PLAYING,"next wave ready");
                double x=p.game.playerX;p.advanceGame(0.05);
                check(p.game.playerX<x,"held movement works after wave");
                check(p.game.shots.stream().anyMatch(s->!s.enemy),"held fire works after wave");
                clearWave(p);key(p,"released A");key(p,"released SPACE");p.advanceGame(2.2);x=p.game.playerX;p.advanceGame(0.05);
                check(p.game.playerX==x&&!p.game.fire,"release during intermission prevents stuck inputs");
                clearWave(p);key(p,"pressed D");key(p,"pressed SPACE");p.advanceGame(2.2);x=p.game.playerX;p.advanceGame(0.05);
                check(p.game.playerX>x&&p.game.fire,"press during intermission works next wave");
                for(int i=0;i<20;i++){clearWave(p);p.advanceGame(2.2);check(p.game.state==GameModel.State.PLAYING&&p.game.fire,"repeat transition "+i);}
                key(p,"released D");key(p,"released SPACE");
                key(p,"pressed A");key(p,"pressed LEFT");key(p,"released A");check(p.game.left,"alternative movement key remains held");
                key(p,"released LEFT");check(!p.game.left,"all movement aliases released");
                key(p,"pressed SPACE");p.lostFocus();check(!p.game.fire,"losing focus releases held fire");
                key(p,"released P");key(p,"pressed D");key(p,"pressed SPACE");
            } catch(Exception ex){throw new RuntimeException(ex);}
        });
        // Let every queued focus request run before testing continuing controls.
        SwingUtilities.invokeAndWait(()->{});
        SwingUtilities.invokeAndWait(()->{
            GamePanel p=holder[0];check(p.game.right&&p.game.fire,"deferred focus restoration preserves held keys");
            double x=p.game.playerX;p.advanceGame(0.01);check(p.game.playerX>x,"movement after queued focus requests");p.close();
        });
        SaturnExplosion explosion=new SaturnExplosion(0,0,new Random(42));
        double vx=explosion.dx[0];explosion.update(1);
        check(explosion.getCount()==205,"twice-as-fast fade after one second");
        check(Math.abs(explosion.x[0]-vx*25)<1e-10,"original particle movement speed");
        explosion.update(4.09);check(explosion.getCount()>0,"visible before 5.1 seconds");
        explosion.update(0.01);check(explosion.getCount()==0,"expires at 5.1 seconds");
        Files.delete(directory);
        System.out.println("PASS: "+checks+" checks; held/pressed/released wave-transition controls and 2x explosion fade.");
    }
}
