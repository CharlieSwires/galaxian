import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicLong;
import javax.sound.sampled.*;
/** Checks the real audio worker/queue/PCM writes using a simulated output line. */
public final class MixerPlaybackTests {
    public static void main(String[] args)throws Exception {
        for(int[] setup:new int[][]{{22050,1},{44100,2},{48000,2}}){
            AudioFormat format=new AudioFormat(setup[0],16,setup[1],true,false);
            AtomicLong nonzero=new AtomicLong(),written=new AtomicLong();
            SourceDataLine line=(SourceDataLine)Proxy.newProxyInstance(SourceDataLine.class.getClassLoader(),
                new Class<?>[]{SourceDataLine.class},(proxy,method,params)->{
                    switch(method.getName()){
                    case "getFormat":return format;
                    case "write":
                        byte[] bytes=(byte[])params[0];int offset=(Integer)params[1],length=(Integer)params[2];
                        for(int i=offset;i<offset+length;i++)if(bytes[i]!=0)nonzero.incrementAndGet();
                        written.addAndGet(length);
                        try{Thread.sleep(5);}catch(InterruptedException ignored){}
                        return length;
                    case "isOpen":case "isRunning":case "isActive":return true;
                    case "getControls":return new Control[0];
                    case "getLevel":return 1f;
                    default:
                        Class<?> type=method.getReturnType();if(type==boolean.class)return false;
                        if(type==int.class)return 0;if(type==long.class)return 0L;return null;
                    }
                });
            SoundEffects effects=new SoundEffects(()->line);
            effects.play(SoundEffects.Effect.SHOT);effects.play(SoundEffects.Effect.EXPLOSION);
            long deadline=System.nanoTime()+2000000000L;
            while(nonzero.get()==0&&System.nanoTime()<deadline)Thread.sleep(10);
            if(nonzero.get()==0||written.get()==0||!effects.status().equals("READY"))throw new AssertionError("Audio worker wrote no sound: "+format);
            effects.close();
        }
        System.out.println("PASS: real worker consumes effects and writes non-silent PCM at 22050 mono, 44100 stereo, 48000 stereo");
    }
}
