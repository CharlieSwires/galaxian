/* Copyright © 2026 Charlie's Tenacity Ltd. All rights reserved. */
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

/** Loads the CC0 PNG sprite sheets from either the JAR or the assets directory. */
final class Sprites {
    private final BufferedImage[][] frames=new BufferedImage[4][];
    Sprites() throws IOException {
        frames[1]=load("alien1_g.png",3,3);
        frames[2]=load("alien2_p.png",3,3);
        frames[3]=load("alien3_r.png",2,2);
    }
    private BufferedImage[] load(String name,int cols,int rows)throws IOException {
        String resource="assets/aliens/"+name;
        InputStream stream=Sprites.class.getClassLoader().getResourceAsStream(resource);
        if(stream==null)stream=new FileInputStream(resource);
        BufferedImage sheet;
        try(InputStream in=stream){sheet=ImageIO.read(in);}
        if(sheet==null || sheet.getWidth()!=cols*32 || sheet.getHeight()!=rows*32)
            throw new IOException("Invalid 32-pixel sprite sheet: "+name);
        BufferedImage[] result=new BufferedImage[cols*rows];
        for(int i=0;i<result.length;i++)result[i]=sheet.getSubimage(i%cols*32,i/cols*32,32,32);
        return result;
    }
    BufferedImage frame(int type,double time){BufferedImage[] f=frames[type];return f[(int)(time*9)%f.length];}
}
