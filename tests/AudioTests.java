import java.util.Arrays;
/** PCM tests without a sound device; hardware probing is optional. */
public final class AudioTests {
    public static void main(String[] args)throws Exception {
        for(int rate:new int[]{22050,44100,48000}) {
            for(SoundEffects.Effect effect:SoundEffects.Effect.values()) {
                short[] pcm=SoundEffects.synthesize(effect,rate);
                boolean nonzero=false;for(short sample:pcm)if(sample!=0)nonzero=true;
                if(!nonzero || pcm[0]!=0 || pcm.length<rate*0.04 || pcm.length>rate)
                    throw new AssertionError("Invalid waveform: "+effect+" at "+rate);
            }
        }
        byte[] stereo=new byte[4];SoundEffects.writeFrame(stereo,0,0x1234,2);
        if(!Arrays.equals(stereo,new byte[]{0x34,0x12,0x34,0x12}))throw new AssertionError("Stereo PCM encoding");
        SoundEffects.writeFrame(stereo,0,99999,2);
        if(!Arrays.equals(stereo,new byte[]{-1,127,-1,127}))throw new AssertionError("Positive clipping");
        SoundEffects.writeFrame(stereo,0,-99999,2);
        if(!Arrays.equals(stereo,new byte[]{0,-128,0,-128}))throw new AssertionError("Negative clipping");
        byte[] mono=new byte[2];SoundEffects.writeFrame(mono,0,-1234,1);
        if(mono[0]!=(byte)-1234 || mono[1]!=(byte)(-1234>>8))throw new AssertionError("Mono PCM encoding");
        if(args.length>0 && args[0].equals("probe")) {
            SoundEffects sound=new SoundEffects();sound.play(SoundEffects.Effect.LEVEL_UP);
            long deadline=System.nanoTime()+3000000000L;
            while(sound.status().equals("STARTING")&&System.nanoTime()<deadline)Thread.sleep(20);
            System.out.println("Hardware probe status: "+sound.status());sound.close();
        }
        System.out.println("PASS: 30 effect/rate combinations, mono/stereo PCM and clipping");
    }
}
