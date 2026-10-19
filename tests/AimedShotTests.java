import java.util.*;
/** Focused tests of attacker eligibility, one-time aim, spread and fixed trajectories. */
public final class AimedShotTests {
    static int checks;
    static void check(boolean condition,String label){checks++;if(!condition)throw new AssertionError(label);}
    static GameModel model(){GameModel m=new GameModel(new Random(42),e->{});m.startGame();return m;}
    static GameModel.Alien attacker(GameModel m,double x,double y) {
        GameModel.Alien a=m.aliens.get(0);m.aliens.clear();m.aliens.add(a);
        a.x=x;a.y=y;a.diving=true;a.returning=false;a.age=1;return a;
    }
    static GameModel.Shot last(GameModel m){return m.shots.get(m.shots.size()-1);}
    public static void main(String[] args){
        GameModel m=model();check(!m.fireEnemyShot()&&m.shots.isEmpty(),"formation never fires");
        GameModel.Alien a=attacker(m,320,200);a.age=0.5;
        check(!m.fireEnemyShot(),"opening loop cannot shoot");a.age=1;a.returning=true;
        check(!m.fireEnemyShot(),"returning alien cannot shoot");a.returning=false;a.y=GameModel.HEIGHT-70;
        check(!m.fireEnemyShot(),"alien below player cannot shoot");a.y=200;
        m.playerX=600;check(m.fireEnemyShot(),"diving attacker shoots");GameModel.Shot s=last(m);
        check(s.x==a.x&&s.y==a.y+14,"starts at attacker's position");
        check(s.vx>0&&s.vy>0,"aims right towards player");
        double expected=Math.atan2(m.playerX-s.x,GameModel.HEIGHT-56-s.y);
        check(Math.abs(Math.atan2(s.vx,s.vy)-expected)<=GameModel.AIM_SPREAD_RADIANS+1e-12,"bounded random spread");
        check(Math.abs(Math.hypot(s.vx,s.vy)-219)<1e-9,"wave-one bullet speed");
        m.playerX=24;check(m.fireEnemyShot(),"second shot");check(last(m).vx<0,"aims left after player moves");
        double vx=s.vx,vy=s.vy,x=s.x,y=s.y;
        m.aliens.remove(a);m.aliens.add(new GameModel.Alien(0,0));m.invulnerable=100;
        m.update(0.04);
        check(s.vx==vx&&s.vy==vy&&Math.abs(s.x-x-vx*0.04)<1e-9&&Math.abs(s.y-y-vy*0.04)<1e-9,"fixed trajectory after player moves and shooter dies");
        m=model();a=attacker(m,320,200);m.playerX=320;
        Set<Long> angles=new HashSet<>();
        for(int i=0;i<100;i++){m.fireEnemyShot();GameModel.Shot shot=last(m);angles.add(Double.doubleToLongBits(shot.vx));check(shot.vy>0,"always downward");}
        check(angles.size()>1,"spread varies");
        m=model();a=attacker(m,220,210);
        GameModel.Alien other=new GameModel.Alien(1,8);other.x=450;other.y=250;other.diving=true;other.age=1;m.aliens.add(other);
        GameModel.Alien resting=new GameModel.Alien(2,4);resting.x=40;resting.y=150;m.aliens.add(resting);
        Set<Double> origins=new HashSet<>();
        for(int i=0;i<50;i++){m.fireEnemyShot();origins.add(last(m).x);check(last(m).x!=resting.x,"formation member never selected");}
        check(origins.size()==2,"random choice among eligible divers");
        m=model();m.invulnerable=100;
        for(int i=0;i<120;i++)m.update(1.0/120);
        check(m.shots.isEmpty(),"no shots before first dive");
        for(int i=0;i<220;i++)m.update(1.0/120);
        check(m.shots.stream().anyMatch(shot->shot.enemy),"normal game fires after attackers descend");
        System.out.println("PASS: "+checks+" checks; diver-only fire, spread, aim and non-homing trajectories.");
    }
}
