/* Copyright © 2026 Charlie's Tenacity Ltd. All rights reserved. */
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Ranked top ten with the Saturn XOR sequence and score-row format. */
final class HighScores {
    static final class Entry {
        final int score;final String name;
        Entry(int score,String name){this.score=score;this.name=name;}
        @Override public String toString(){return "ScoreName [score="+score+", name="+name+"]";}
    }
    private final Path file;
    private List<Entry> entries=new ArrayList<>(); // highest first
    HighScores(Path file){this.file=file;}
    List<Entry> entries(){return Collections.unmodifiableList(entries);}
    int best(){return entries.isEmpty()?0:entries.get(0).score;}
    boolean qualifies(int score){return entries.size()<10 || score>entries.get(entries.size()-1).score;}
    boolean add(int score,String name) {
        if(!qualifies(score))return false;
        name=name==null?"PLAYER":name.replaceAll("[\\p{Cntrl}\\[\\],=]"," ").trim();
        if(name.isEmpty())name="PLAYER";
        // Retain the legacy single-byte-safe format; name entry advertises this limit.
        name=name.replaceAll("[^\\x20-\\x7E]","?");
        if(name.length()>20)name=name.substring(0,20);
        List<Entry> updated=new ArrayList<>(entries);updated.add(new Entry(score,name));
        updated.sort((a,b)->Integer.compare(b.score,a.score));
        if(updated.size()>10)updated.remove(10);entries=updated;return true;
    }
    void load() throws IOException {
        if(!Files.exists(file))return;
        byte[] data=Files.readAllBytes(file);transform(data);
        String text=new String(data,StandardCharsets.UTF_8).trim();
        List<Entry> loaded=new ArrayList<>();
        if(!text.equals("[]")) {
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("ScoreName \\[score=(-?\\d+), name=([^\\]]*)\\]").matcher(text);
            try {while(m.find())loaded.add(new Entry(Integer.parseInt(m.group(1)),m.group(2)));}
            catch(NumberFormatException ex){throw new IOException("Invalid high-score number",ex);}
            if(loaded.isEmpty())throw new IOException("Unrecognised high-score file; retained unchanged");
        }
        loaded.sort((a,b)->Integer.compare(b.score,a.score));
        if(loaded.size()>10)loaded=new ArrayList<>(loaded.subList(0,10));entries=loaded;
    }
    void save() throws IOException {
        List<Entry> ascending=new ArrayList<>(entries);Collections.reverse(ascending);
        byte[] data=ascending.toString().getBytes(StandardCharsets.UTF_8);transform(data);
        Path absolute=file.toAbsolutePath();Files.createDirectories(absolute.getParent());
        Path temp=Files.createTempFile(absolute.getParent(),"galaxian-scores-",".tmp");
        try {
            Files.write(temp,data);
            try{Files.move(temp,absolute,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){Files.move(temp,absolute,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temp);}
    }
    static void transform(byte[] data){Random r=new Random(73890);for(int i=0;i<data.length;i++)data[i]^=(byte)(int)(r.nextDouble()*255.0);}
}
