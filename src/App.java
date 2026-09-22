import javax.swing.*;
import java.awt.*;

public final class App {

    public static void main(String[] args) {
        Color couleurFond = Color.decode("#2A72B0");
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Lecteur musique");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(360, 480);
            frame.setResizable(false);
            frame.getContentPane().setBackground(couleurFond);            
            frame.setVisible(true);
        });
    }
}
