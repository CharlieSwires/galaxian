import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Dependency-free checks; runs without a display or audio device. */
public final class GalaxianTests {
    static int checks;
    static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    static GameModel model(){return new GameModel(new Random(42),effect->{});}
    public static void main(String[] args)throws Exception{
        GameModel m=model();m.startGame();check(m.aliens.size()==44,"44 enemies per wave");
        check(m.lives==3&&m.wave==1&&m.score==0,"new game reset");
        m.left=true;for(int i=0;i<200;i++)m.update(1.0/60);check(m.playerX>=24,"left bound");
        m.resetControls();m.right=true;for(int i=0;i<200;i++)m.update(1.0/60);check(m.playerX<=616,"right bound");
        m=model();m.startGame();m.fire=true;for(int i=0;i<30;i++)m.update(1.0/60);
        check(m.shots.stream().filter(s->!s.enemy).count()<=2,"two player shots");
        m=model();m.startGame();GameModel.Alien a=m.aliens.get(0);
        m.shots.add(new GameModel.Shot(a.x,a.y,0,0,false));m.update(0.001);
        check(m.aliens.size()==43&&m.score==150,"formation hit scores");
        m=model();m.startGame();a=m.aliens.get(0);m.beginDive(a);
        m.shots.add(new GameModel.Shot(a.x,a.y,0,0,false));m.update(0.001);
        check(m.score==300,"diving doubles points");
        m=model();m.startGame();a=m.aliens.get(0);m.beginDive(a);
        boolean sawReturn=false,sawHome=false;
        for(int i=0;i<1200;i++){m.update(1.0/120);if(a.returning)sawReturn=true;if(sawReturn&&!a.diving){sawHome=true;break;}}
        check(sawReturn&&sawHome,"diver exits and rejoins formation");
        m=model();m.startGame();m.invulnerable=0;m.hitPlayer();check(m.lives==2,"player hit");m.hitPlayer();check(m.lives==2,"respawn protection");
        m.invulnerable=0;m.hitPlayer();m.invulnerable=0;m.hitPlayer();check(m.state==GameModel.State.GAME_OVER,"game over");
        m.startGame();check(m.lives==3&&m.score==0&&!m.fire,"restart controls and lives");
        m.aliens.clear();m.update(0.01);check(m.state==GameModel.State.WAVE_CLEAR,"wave clear");m.update(2.2);check(m.wave==2&&m.aliens.size()==44,"next wave");
        m.addPoints(10000);check(m.lives==4,"bonus life");m.addPoints(10000);check(m.lives==5,"second bonus life");
        // A single surviving enemy proves the origin; player position must not affect aim.
        m=model();m.startGame();m.playerX=600;m.invulnerable=100;
        GameModel.Alien shooter=m.aliens.get(0);m.aliens.clear();m.aliens.add(shooter);
        for(int i=0;i<156;i++)m.update(1.0/120);
        check(!m.shots.isEmpty(),"enemy fired");
        for(GameModel.Shot shot:m.shots)if(shot.enemy) {
            check(shot.vx==0 && shot.vy>0,"enemy bullet travels straight down");
            double x=shot.x;m.update(0.01);check(shot.x==x,"enemy bullet has no horizontal drift");break;
        }
        // Both random choices and a fixed vertical trajectory are required.
        m=model();m.startGame();m.invulnerable=100;java.util.Set<Double> origins=new java.util.HashSet<>();
        java.util.Set<GameModel.Shot> seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<GameModel.Shot,Boolean>());
        for(int i=0;i<2400;i++) {
            m.update(1.0/120);
            for(GameModel.Shot shot:m.shots)if(shot.enemy&&seen.add(shot)) {
                check(shot.vx==0 && shot.vy>0,"every random enemy shot is vertical");origins.add(shot.x);
            }
        }
        check(origins.size()>1,"random enemies fire from different origins");
        // Run a deterministic simulation to catch list mutation and numeric problems.
        m=model();m.startGame();for(int i=0;i<20000;i++){
            m.fire=true;m.right=(i/150)%2==0;m.left=!m.right;m.invulnerable=1;m.update(1.0/120);
            for(GameModel.Alien enemy:m.aliens)check(Double.isFinite(enemy.x)&&Double.isFinite(enemy.y),"finite enemy position");
            if(m.state==GameModel.State.GAME_OVER)m.startGame();
        }
        Path folder=Files.createTempDirectory("galaxian-test-");Path file=folder.resolve("scores.bin");
        HighScores scores=new HighScores(file);scores.load();check(scores.entries().isEmpty(),"missing score file");
        for(int i=1;i<=10;i++)scores.add(i*100,"P"+i);
        check(scores.add(550,"Middle"),"qualifying middle score");check(scores.entries().get(5).score==550,"ranked position");
        check(scores.entries().size()==10&&!scores.qualifies(200),"ten entries and cutoff tie");
        scores.add(500,"Tie");int index=-1;for(int i=0;i<10;i++)if(scores.entries().get(i).name.equals("Tie"))index=i;
        check(index>0&&scores.entries().get(index-1).name.equals("P5"),"new tie below older score");
        scores.save();HighScores loaded=new HighScores(file);loaded.load();check(loaded.best()==1000&&loaded.entries().size()==10,"save and load");
        byte[] bytes={0,1,127,-128,-1};byte[] original=bytes.clone();HighScores.transform(bytes);HighScores.transform(bytes);check(Arrays.equals(bytes,original),"same XOR transform twice restores bytes");
        // Headless render uses the real PNG loader and real game panel.
        SwingUtilities.invokeAndWait(()->{
            try{
                GamePanel panel=new GamePanel(folder.resolve("ui-scores.bin"),false,false);panel.setSize(640,780);
                for(GameModel.State state:new GameModel.State[]{GameModel.State.TITLE,GameModel.State.PLAYING,GameModel.State.SCORES}){
                    if(state==GameModel.State.PLAYING)panel.start();panel.game.state=state;
                    BufferedImage image=new BufferedImage(640,780,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.paint(g);g.dispose();
                    ImageIO.write(image,"png",Paths.get("build","preview-"+state.name().toLowerCase()+".png").toFile());
                }
                panel.game.state=GameModel.State.PLAYING;
                panel.getActionMap().get("pressed A").actionPerformed(null);check(panel.game.left,"window key binding");
                panel.getActionMap().get("released A").actionPerformed(null);check(!panel.game.left,"key release");
                panel.lostFocus();check(!panel.game.fire&&!panel.game.left,"focus loss reset");
                panel.game.state=GameModel.State.SCORES;panel.getActionMap().get("pressed SPACE").actionPerformed(null);check(!panel.game.fire,"score screen blocks shooting");
                panel.start();panel.getActionMap().get("pressed SPACE").actionPerformed(null);check(panel.game.fire,"restart shooting works");panel.close();
            }catch(Exception ex){throw new RuntimeException(ex);}
        });
        Files.deleteIfExists(file);Files.deleteIfExists(folder.resolve("ui-scores.bin"));Files.delete(folder);
        System.out.println("PASS: "+checks+" assertions; model, scoring, waves, attacks, keys, assets and rendering.");
    }
}
