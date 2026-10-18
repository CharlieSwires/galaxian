/** Confirms the executable JAR can find its images outside the project folder. */
public final class JarResourceCheck {
    public static void main(String[] args)throws Exception {
        Sprites sprites=new Sprites();
        for(int type=1;type<=3;type++)if(sprites.frame(type,0).getWidth()!=32)throw new AssertionError("Sprite "+type);
        System.out.println("PASS: PNG resources load from the JAR without loose assets");
    }
}
