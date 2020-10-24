package imagevision;

import javax.swing.*;  

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class FirstSwingExample {  
    public static void main(String[] args) {  

        JFrame frame = new JFrame();//creating instance of JFrame 
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setBounds(0,0, dim.width, dim.height); 

        // CENTER PANEL
        var centerpanel = new JPanel();
        centerpanel.setLayout(new GridLayout(1,2));
        frame.add(centerpanel, BorderLayout.CENTER);

        var panel = new ImagePanel("/home/osboxes/Pictures/coche.jpg");
        centerpanel.add(panel);
        var img = panel.getImage();
        var processor = new ImageProcessor(panel.getImage());
        
        var panel2 = new ImagePanel(processor.getBWImage());
        centerpanel.add(panel2);
        
        // CHART
        var chart = new ChartPanel(processor.getHistogram(), new String[]{},"HISTOGRAMA");
        
        var chartFrame = new JFrame();
        chartFrame.add(chart);
        chartFrame.setBounds(0,0, dim.width/3, dim.height/3);

        // TOOLBAR
        //JToolBar barraBotones = new JToolBar();
        JMenuBar mb = new JMenuBar();
        JMenu menu1=new JMenu("Datos");
        JMenuItem mi1, mi2;
        
        frame.setJMenuBar(mb);
        
        mb.add(menu1);
        mi1=new JMenuItem("Información");
        menu1.add(mi1);
        mi2=new JMenuItem("Histograma");
        menu1.add(mi2);
        
        mi2.addActionListener(new ActionListener() { 
            public void actionPerformed(ActionEvent e) { 
                chartFrame.setVisible(true);
            } 
        });

        mi1.addActionListener(new ActionListener() { 
            public void actionPerformed(ActionEvent e) { 
                JOptionPane.showMessageDialog(frame,
    "Size: " + img.getWidth() + "x" + img.getHeight());
            } 
        });
        // VISIBLES FRAMES
        frame.setVisible(true);
        centerpanel.setVisible(true);
        
    }  
} 

class ImagePanel extends JPanel{

    private BufferedImage image;

    public BufferedImage getImage () {
        return image;
    }

    public ImagePanel(String filename) {
       try {                
            image = ImageIO.read(new File(filename));
            System.out.println("constructor called");

       } catch (IOException ex) {
        System.out.println("NOOOOOO");
       }
    }
    public ImagePanel(BufferedImage img) {
        image = img;
     }
    @Override
        public Dimension getPreferredSize () {
        return new Dimension(image.getWidth(),image.getHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        System.out.println("paintcomp called");
        super.paintComponent(g);
        g.drawImage(image, 0, 0, this); // see javadoc for more info on the parameters            
    }

}

class ImageProcessor {
    private BufferedImage image;
    private BufferedImage bwimage;
    private double [] histogram = new double[256];

    public ImageProcessor(BufferedImage img) {
        image = img;
        convertToGray();
        fillHistogram();
        
    }
    public double[] getHistogram() {
        return histogram;
    }
    private void fillHistogram() {
        DataBuffer raster = bwimage.getRaster().getDataBuffer();
        for (int i=0; i<raster.getSize(); i++) {
            histogram[raster.getElem(i)]++;
        }
    }

    private void convertToGray() {
        bwimage = new BufferedImage(
            image.getWidth(),
            image.getHeight(),
            BufferedImage.TYPE_BYTE_GRAY
        );

        for (int i=0; i<image.getWidth(); i++){
            for (int j=0; j<image.getHeight(); j++){
                int pixel = image.getRGB(i,j);
                
                bwimage.setRGB(i,j,pixel);
            }
        }
    }
    public BufferedImage getBWImage() {
        return bwimage;
    }

}
