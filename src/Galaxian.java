/* Copyright © 2026 Charlie's Tenacity Ltd. All rights reserved. */
import java.awt.event.*;
import java.nio.file.Paths;
import javax.swing.*;

/** Entry point for the independent Galaxian-inspired desktop game. */
public final class Galaxian {
    public static void main(String[] args){
        SwingUtilities.invokeLater(()->{
            try{
                GamePanel panel=new GamePanel(Paths.get("galaxian-scores.bin"),true,true);
                JFrame frame=new JFrame("Galaxian — Charlie's Arcade");
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                frame.setContentPane(panel);frame.pack();frame.setMinimumSize(new java.awt.Dimension(500,660));frame.setLocationRelativeTo(null);
                frame.addWindowFocusListener(new WindowAdapter(){@Override public void windowLostFocus(WindowEvent e){panel.lostFocus();}});
                frame.addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){panel.close();}});
                frame.setVisible(true);panel.requestFocusInWindow();
            }catch(Exception ex){ex.printStackTrace();JOptionPane.showMessageDialog(null,"Unable to start Galaxian: "+ex.getMessage(),"Galaxian",JOptionPane.ERROR_MESSAGE);}
        });
    }
}
