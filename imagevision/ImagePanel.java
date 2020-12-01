package imagevision;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.image.*;

public class ImagePanel extends JPanel {
    private BufferedImage image;
    private float ratio;
    public BufferedImage getImage () {
        return image;
    }

    public ImagePanel(BufferedImage img) {
        image = img;
        ratio = (float)img.getWidth()/img.getHeight();
    }

    @Override
    public Dimension getPreferredSize () {
        return new Dimension(image.getWidth(),image.getHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, this.getWidth(), Math.round(this.getWidth()/ratio), this); // see javadoc for more info on the parameters            
    }

}