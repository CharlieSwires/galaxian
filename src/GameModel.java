/* Copyright © 2026 Charlie's Tenacity Ltd. All rights reserved. */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/** Game simulation, updated only by Swing's event thread. Coordinates are logical pixels. */
final class GameModel {
    static final int WIDTH = 640, HEIGHT = 780;
    enum State { TITLE, PLAYING, WAVE_CLEAR, GAME_OVER, SCORES }
    static final class Alien {
        final int row, column, type;
        double x, y, age, originX, originY, targetX;
        int direction;
        boolean diving, returning;
        Alien(int row, int column) { this.row=row; this.column=column; type=row==0?3:row<=2?2:1; }
        int points() { return type==3?150:type==2?80:50; }
    }
    static final class Shot {
        double x,y; final double vx,vy; final boolean enemy;
        Shot(double x,double y,double vx,double vy,boolean enemy) { this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.enemy=enemy; }
    }
    final List<Alien> aliens = new ArrayList<>();
    final List<Shot> shots = new ArrayList<>();
    final List<SaturnExplosion> bursts = new ArrayList<>();
    private final Random random;
    private final Consumer<SoundEffects.Effect> sound;
    State state = State.TITLE;
    int score, lives=3, wave=1;
    double playerX=WIDTH/2.0, time, invulnerable, stateTime;
    boolean left,right,fire;
    private double fireDelay,diveDelay,enemyDelay;
    private int nextExtraLife;

    GameModel(Random random, Consumer<SoundEffects.Effect> sound) { this.random=random;this.sound=sound; }
    void resetControls() { left=false;right=false;fire=false; }
    void startGame() {
        score=0;lives=3;wave=1;time=0;playerX=WIDTH/2.0;nextExtraLife=10000;
        resetControls(); invulnerable=2; startWave();
    }
    private void startWave() {
        aliens.clear();shots.clear();bursts.clear();fireDelay=0;diveDelay=1.5;enemyDelay=1.2;
        for(int row=0;row<5;row++) for(int col=0;col<10;col++) {
            if(row==0 && (col<3 || col>6)) continue;
            Alien a=new Alien(row,col);a.x=homeX(a);a.y=homeY(a);aliens.add(a);
        }
        state=State.PLAYING;stateTime=0;
    }
    double homeX(Alien a) { return 92+a.column*48+Math.sin(time*0.8)*32; }
    double homeY(Alien a) { return 142+a.row*40+Math.sin(time*1.4)*3; }
    void update(double dt) {
        if(dt<=0) return;
        time+=dt;stateTime+=dt;
        for(Iterator<SaturnExplosion> i=bursts.iterator();i.hasNext();) {
            SaturnExplosion explosion=i.next();explosion.update(dt);if(explosion.getCount()==0)i.remove();
        }
        if(state==State.WAVE_CLEAR) { if(stateTime>=2.1){wave++;invulnerable=1.5;startWave();}return; }
        if(state!=State.PLAYING) return;
        invulnerable=Math.max(0,invulnerable-dt);
        playerX=Math.max(24,Math.min(WIDTH-24,playerX+((right?1:0)-(left?1:0))*330*dt));
        fireDelay-=dt;
        if(fire && fireDelay<=0 && playerShotCount()<2) {
            shots.add(new Shot(playerX,HEIGHT-73,0,-610,false));fireDelay=0.23;sound.accept(SoundEffects.Effect.SHOT);
        }
        diveDelay-=dt;enemyDelay-=dt;
        if(diveDelay<=0) {
            long diving=aliens.stream().filter(a->a.diving).count();
            List<Alien> ready=new ArrayList<>();for(Alien a:aliens)if(!a.diving)ready.add(a);
            if(!ready.isEmpty() && diving<Math.min(6,1+wave)) beginDive(ready.get(random.nextInt(ready.size())));
            diveDelay=Math.max(0.38,1.7-wave*0.12)*(0.8+random.nextDouble()*0.4);
        }
        if(enemyDelay<=0 && !aliens.isEmpty() && shots.size()<70) {
            Alien a=aliens.get(random.nextInt(aliens.size()));
            shots.add(new Shot(a.x,a.y+14,0,210+Math.min(wave,20)*9,true));
            sound.accept(SoundEffects.Effect.ENEMY_SHOT);
            enemyDelay=Math.max(0.22,0.9-wave*0.045);
        }
        for(Alien a:aliens) updateAlien(a,dt);
        for(Shot s:shots){s.x+=s.vx*dt;s.y+=s.vy*dt;}
        for(Iterator<Shot> i=shots.iterator();i.hasNext();) {
            Shot s=i.next();boolean remove=s.y < -30 || s.y>HEIGHT+30 || s.x < -30 || s.x>WIDTH+30;
            if(!remove && s.enemy) {
                if(Math.abs(s.x-playerX)<15 && Math.abs(s.y-(HEIGHT-56))<17) {
                    remove=true;hitPlayer();
                }
            } else if(!remove) {
                for(Iterator<Alien> ai=aliens.iterator();ai.hasNext();) {
                    Alien a=ai.next();
                    if(Math.abs(s.x-a.x)<18 && Math.abs(s.y-a.y)<17) {
                        ai.remove();addPoints(a.points()*(a.diving?2:1));
                        bursts.add(new SaturnExplosion(a.x,a.y,random));sound.accept(SoundEffects.Effect.EXPLOSION);remove=true;break;
                    }
                }
            }
            if(remove)i.remove();
        }
        if(state==State.GAME_OVER) { shots.clear();return; }
        for(Alien a:aliens) if(Math.abs(a.x-playerX)<27 && Math.abs(a.y-(HEIGHT-56))<25) hitPlayer();
        if(state==State.GAME_OVER) { shots.clear();return; }
        if(aliens.isEmpty()) {
            shots.clear();state=State.WAVE_CLEAR;stateTime=0;
            sound.accept(SoundEffects.Effect.LEVEL_UP);
        }
    }
    private long playerShotCount(){return shots.stream().filter(s->!s.enemy).count();}
    void beginDive(Alien a) {
        a.diving=true;a.returning=false;a.age=0;a.originX=a.x;a.originY=a.y;
        a.targetX=playerX;a.direction=a.x<WIDTH/2?1:-1;
        sound.accept(SoundEffects.Effect.BONUS);
    }
    private void updateAlien(Alien a,double dt) {
        if(!a.diving){a.x=homeX(a);a.y=homeY(a);return;}
        a.age+=dt;
        if(a.returning) {
            double dx=homeX(a)-a.x,dy=homeY(a)-a.y,d=Math.hypot(dx,dy),step=270*dt;
            if(d<=step){a.diving=false;a.returning=false;a.x=homeX(a);a.y=homeY(a);}
            else {a.x+=dx/d*step;a.y+=dy/d*step;}
        } else if(a.age<0.75) {
            double t=a.age/0.75;
            a.x=a.originX+a.direction*Math.sin(t*Math.PI)*62;
            a.y=a.originY-Math.sin(t*Math.PI)*55;
        } else {
            double t=a.age-0.75;
            a.y=a.originY+t*(195+Math.min(wave,20)*12);
            double approach=Math.min(1,t/1.9);
            a.x=a.originX+(a.targetX-a.originX)*approach+Math.sin(t*4)*65*a.direction;
            a.x=Math.max(18,Math.min(WIDTH-18,a.x));
            if(a.y>HEIGHT+35){a.returning=true;a.x=homeX(a);a.y=-35;}
        }
    }
    void hitPlayer() {
        if(invulnerable>0 || state!=State.PLAYING)return;
        bursts.add(new SaturnExplosion(playerX,HEIGHT-56,random));lives--;resetControls();
        sound.accept(SoundEffects.Effect.PLAYER_HIT);
        if(lives<=0){state=State.GAME_OVER;stateTime=0;sound.accept(SoundEffects.Effect.GAME_OVER);}
        else {invulnerable=2.5;playerX=WIDTH/2.0;}
    }
    void addPoints(int points) {
        score+=points;
        while(score>=nextExtraLife) {lives++;nextExtraLife+=10000;sound.accept(SoundEffects.Effect.LEVEL_UP);}
    }
}
