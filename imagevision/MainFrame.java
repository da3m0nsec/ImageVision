package imagevision;

import javax.swing.*;  
import java.nio.file.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import java.util.*;

// TO-DO
// Implement active image listener

public class MainFrame extends JFrame { 

    private ArrayList<ImageProcessor> images = new ArrayList<ImageProcessor>();
    private int activeImage = 0;
    private JDesktopPane desktopPane = new JDesktopPane();

    private Dimension dim;

    public MainFrame() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        dim = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(0, 0, dim.width, dim.height); 
    }

    private void createLayout() {
        setJMenuBar(createMenu());
        
        add(desktopPane);

        desktopPane.setVisible(true);
        setVisible(true);
    }

    public static String chooseFile() {
        JFileChooser fc = new JFileChooser();
        int ret = fc.showOpenDialog(null);

        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String filename = file.getAbsolutePath();
            return filename;
        }
        else {
            return null;
        }
    }

    private void addImage(ImageProcessor imgP) {
        images.add(imgP);
        var img = imgP.getImage();
        var panel = new ImagePanel(img);
        var internalFrame = new JInternalFrame(imgP.getFileName(),false,true,true,true);

        internalFrame.add(panel);
        desktopPane.add(internalFrame);
        ImagePanel imgPanel = (ImagePanel) internalFrame.getContentPane().getComponent(0);
        
        internalFrame.pack();
        internalFrame.setVisible(true);
    }

    private Optional<ImageProcessor> getActiveImage () {
        var comp = desktopPane.getSelectedFrame().getContentPane().getComponent(0);
        var img = (ImagePanel)comp;
        return findImage(img.getImage());
    }

    private Optional<ImageProcessor> findImage (BufferedImage img) {
        for (ImageProcessor i : images){
            if (i.getImage() == img)
                return Optional.of(i);
        }
        return Optional.empty();
    }

    public static void main(String[] args) {  

        var frame = new MainFrame(); //creating instance of JFrame 
        frame.createLayout();      
    }

    private JMenuBar createMenu() {
        // Top level menu
        var mb = new JMenuBar();
        var menuFile = new JMenu("File");
        var menuData = new JMenu("Data");

        mb.add(menuFile);
        mb.add(menuData);

        // Menu Items
        var miOpen = new JMenuItem("Open");
        menuFile.add(miOpen);

        var miInfo = new JMenuItem("Info");
        menuData.add(miInfo);
        var miHisto = new JMenuItem("Histogram");
        menuData.add(miHisto);

        // Menu Listeners
        miOpen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String filename = chooseFile();
                var imgP = new ImageProcessor(filename);
                addImage(imgP);
            }
        });

        miInfo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                var imgP = getActiveImage().get();
                var range = imgP.getRange();
                JOptionPane.showMessageDialog(null,
                    "Size: " + imgP.getImage().getWidth() + "x" + imgP.getImage().getHeight() +
                    "\nFile type: " + imgP.getMimeType() +
                    "\nGrat Range: [" + range[0] + ", " + range[1] + "]"
                );
            }
        });

        miHisto.addActionListener(new ActionListener() { 
            public void actionPerformed(ActionEvent e) { 
                var chart = new ChartPanel(getActiveImage().get().getHistogram(), new String[]{},"HISTOGRAM");
                var chartFrame = new JFrame();
                chartFrame.add(chart);
                chartFrame.setBounds(dim.width/3,dim.height/3, dim.width/3, dim.height/3);
                chartFrame.setVisible(true);
            } 
        });
        
        return mb;
    }
} 

class ImagePanel extends JPanel{

    public String str = "hello";

    private BufferedImage image;

    public BufferedImage getImage () {
        return image;
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
        super.paintComponent(g);
        g.drawImage(image, 0, 0, this); // see javadoc for more info on the parameters            
    }

}

class ImageProcessor {
    private String fileName;
    private BufferedImage image;
    private double [] histogram = new double[256];
    private String mimeType;
    private int minGray = 256, maxGray = 0;

    public String getFileName() {
        return fileName;
    }
    public int[] getRange() {
        return new int[]{minGray, maxGray};
    }
    public String getMimeType() {
        return mimeType;
    }

    public BufferedImage getImage () {
        return image;
    }
    public ImageProcessor(String filename) {

        try {
            fileName = filename;
            var f = new File(filename);
            mimeType = Files.probeContentType(f.toPath()).split("/")[1];
            image = ImageIO.read(f);
            System.out.println("constructor called");
            convertToGray();
            init();
        } catch (IOException ex) {
            System.out.println("NOOOOO");
        }
    }
    private void convertToGray() {
        /*image = new BufferedImage(
            image.getWidth(),
            image.getHeight(),
            BufferedImage.TYPE_BYTE_GRAY
        );*/

        for (int i=0; i<image.getWidth(); i++){
            for (int j=0; j<image.getHeight(); j++){
                var pixel = new Color(image.getRGB(i,j));
                int r = pixel.getRed();
                int g = pixel.getGreen();
                int b = pixel.getBlue();
                int gray = (int)(0.222*(double)r + 0.707*(double)g + 0.071*(double)b);

                image.setRGB(i,j, (new Color(gray, gray, gray)).getRGB());
            }
        }
    }

    public ImageProcessor(BufferedImage img) {
        image = img;
        init();
        
    }
    public double[] getHistogram() {
        return histogram;
    }
    private void init() {
        fillHistogram();
    }
    private void fillHistogram() {
        //DataBuffer raster = image.getRaster().getDataBuffer();
        for (int i=0; i<image.getWidth(); i++){
            for (int j=0; j<image.getHeight(); j++){
                //int pixel = raster.getElem(i);
                int pixel = (new Color(image.getRGB(i, j))).getGreen();
                if (pixel < minGray) {
                    minGray = pixel;
                }
                if (pixel > maxGray) {
                    maxGray = pixel;
                }
                histogram[pixel]++;
            }
        }
    }

}
