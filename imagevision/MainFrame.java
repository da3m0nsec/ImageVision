package imagevision;

import javax.swing.*;
import javax.swing.event.*;

import sun.awt.image.BufferedImageDevice;

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
import java.lang.Math;
import java.net.InterfaceAddress;
// TO-DO
// Implement active image listener

public class MainFrame extends JFrame { 

    private static final long serialVersionUID = -477785003793521810L;

    private ArrayList<ImageProcessor> images = new ArrayList<ImageProcessor>();
    private ImageProcessor activeImage;
    private ImagePanel activePanel;
    private JDesktopPane desktopPane = new JDesktopPane();
    private JLabel mouseLabel;
    private Dimension dim;
    private MouseMotionListener mouseListener = new MouseMotionListener() {
        @Override
        public void mouseMoved(MouseEvent e) { 
            int x = e.getX();
            int y = e.getY();
            int grayLevel = new Color(activePanel.getImage().getRGB(x, y)).getGreen();
            String mouse = String.format("x:%d y:%d (%d)", x,y,grayLevel);
            mouseLabel.setText(mouse);
        }
        @Override
        public void mouseDragged (MouseEvent e){
        }
    };;

    public MainFrame() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        dim = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(0, 0, dim.width, dim.height); 
    }

    private void createLayout() {
        setJMenuBar(createMenu());
        add(desktopPane);
        desktopPane.setVisible(true);

        mouseLabel = new JLabel("x: y: ");
        add(mouseLabel, BorderLayout.SOUTH);
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
        var internalFrame = new JInternalFrame(imgP.getFileName(),false,true,false,true);
        internalFrame.addInternalFrameListener(new InternalFrameAdapter(){
            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                if (activePanel != null)
                    activePanel.removeMouseMotionListener(mouseListener);
                var comp = e.getInternalFrame().getContentPane().getComponent(0);
                activePanel = (ImagePanel)comp;
                activeImage = getActiveImage().get();
                activePanel.addMouseMotionListener(mouseListener);
            }
            public void internalFrameClosed(InternalFrameEvent e) {
                images.remove(getImageFromFrame(e.getInternalFrame()).get());
            }
        });
        internalFrame.add(panel);
        desktopPane.add(internalFrame);
        //var imgPanel = (ImagePanel) internalFrame.getContentPane().getComponent(0);
        
        internalFrame.pack();
        internalFrame.setVisible(true);
    }

    private Optional<ImageProcessor> getImageFromFrame (JInternalFrame j) {
        var comp = j.getContentPane().getComponent(0);
        var img = (ImagePanel)comp;
        return findImage(img.getImage());
    }

    private Optional<ImageProcessor> getActiveImage () {
        return getImageFromFrame(desktopPane.getSelectedFrame());
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
        var menuEdit = new JMenu("Edit");

        mb.add(menuFile);
        mb.add(menuData);
        mb.add(menuEdit);


        // Menu Items
        var miOpen = new JMenuItem("Open");
        menuFile.add(miOpen);

        var miInfo = new JMenuItem("Info");
        menuData.add(miInfo);
        var miHisto = new JMenuItem("Histogram");
        menuData.add(miHisto);
        var miCumHisto = new JMenuItem("Cumulative Histo");
        menuData.add(miCumHisto);

        var miInterval = new JMenuItem("Interval Defined");
        menuEdit.add(miInterval);

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
                var imgP = activeImage;
                var range = imgP.getRange();
                JOptionPane.showMessageDialog(null,
                    "Size: " + imgP.getImage().getWidth() + "x" + imgP.getImage().getHeight() +
                    "\nFile type: " + imgP.getMimeType() +
                    "\nGray Range: [" + range[0] + ", " + range[1] + "]" +
                    "\nBrightness: " + imgP.getBrightness() + " Contrast: " + imgP.getContrast() +
                    "\nEntropy: " + imgP.getEntropy()
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

        miCumHisto.addActionListener(new ActionListener() { 
            public void actionPerformed(ActionEvent e) { 
                var chart = new ChartPanel(getActiveImage().get().cumHistogram, new String[]{},"CUMULATIVE HISTOGRAM");
                var chartFrame = new JFrame();
                chartFrame.add(chart);
                chartFrame.setBounds(dim.width/3,dim.height/3, dim.width/3, dim.height/3);
                chartFrame.setVisible(true);
            } 
        });
        
        miInterval.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                var dialog = new IntervalEditDialog(MainFrame.this);
                var v = dialog.doModal();

                System.out.println(Arrays.deepToString(v));
            }
        });
        return mb;
    }
} 

class ImagePanel extends JPanel{
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
    public double [] cumHistogram = new double[256];
    private String mimeType;
    private int minGray = 256, maxGray = 0;
    private double entropy;
    private double brightness;
    private double contrast;

    public double getEntropy() {
        return entropy;
    }
    public double getBrightness() {
        return brightness;
    }
    public double getContrast() {
        return contrast;
    }

    public ImageProcessor transform(int[] f, int[] t) {
        var tabla = new int[256];
        for (int i=0; i<f.length/2; i++) {
            int xf = f[i];
            int yf = f[i+1];

            int xt = t[i];
            int yt = t[i+1];

            double m = ((double)(xt-xf))/(yt-yf);
            double b = (double)(yt)/m*xt;

            for (int v=xf; v<xt; ++v) {
                tabla[v] = (int)(m*v + b);
            }
        }

        BufferedImage buf = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        for (int i=0; i<image.getWidth(); i++){
            for (int j=0; j<image.getHeight(); j++) {
                int newVal = tabla[getPixel(i, j)];
                buf.setRGB(i,j,new Color(newVal, newVal, newVal).getRGB());
            }
        }
        return new ImageProcessor(image, fileName);
    }
    public int getPixel(int i, int j) {
        return new Color(image.getRGB(i,j)).getGreen();
    }
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
    public int getSize() {
        return image.getWidth() * image.getHeight();
    }
    public ImageProcessor(String filename) {
        try {
            fileName = filename;
            var f = new File(filename);
            mimeType = Files.probeContentType(f.toPath()).split("/")[1];
            image = ImageIO.read(f);
            convertToGray();
            init();
        } catch (IOException ex) {
            System.out.println("NOOOOO");
        }
    }
    
    private void convertToGray() {
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

    public ImageProcessor(BufferedImage img, String name) {
        image = img;
        fileName = name;
        init();
    }

    public double[] getHistogram() {
        return histogram;
    }

    private void init() {
        fillHistogram();
        initInfo();
    }

    private void fillHistogram() {
        for (int i=0; i<image.getWidth(); i++){
            for (int j=0; j<image.getHeight(); j++){
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
        cumHistogram[0] = histogram[0];
        for (int i=1; i<histogram.length; ++i){
            cumHistogram[i] = histogram[i] + cumHistogram[i-1];
        }
    }
    private void initInfo() {
        // Brightness
        int sum = 0;
        for (int i=0; i<histogram.length; ++i) {
            sum += i*histogram[i];
        }
        brightness = (Math.floor((double)sum/getSize()*100))/100;

        // Contrast
        sum = 0;
        for (int i=0; i<histogram.length; ++i) {
            sum += histogram[i] * (i - brightness)*(i - brightness);
        }

        contrast = (Math.floor(Math.sqrt((1.0/getSize()) * sum)*100))/100;

        // Entropy
        double sumd = 0;
        for (int i=0; i<histogram.length; ++i) {
            double f = (double)histogram[i] / getSize();
            if (f == 0) {
                continue;
            }
            double log2f = Math.log(f) / Math.log(2.0);
            

            sumd += f * log2f;
        }
        entropy = (Math.floor(-sumd*100))/100;
    }

}

class IntervalEditDialog extends JDialog{

    private int intNumber;
    private int[] fVector;
    private int[] tVector;

    public IntervalEditDialog (Frame owner){
        super(owner);
        createDialog();
    }

    public void createDialog (){
        setPreferredSize(new Dimension(250, 140));
        setLayout(new BorderLayout());
        setTitle("Interval Editing");

        var next = new JButton("Next");
        var numField = new JTextField(3);
        var centerPanel = new JPanel(new FlowLayout());
        centerPanel.add(new JLabel("Introduce number of intervals: "));
        centerPanel.add(numField);

        var southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(next);

        add (centerPanel, BorderLayout.CENTER);
        add (southPanel, BorderLayout.SOUTH);

        next.addActionListener(new ActionListener (){
            public void actionPerformed(ActionEvent e) { 
                try {
                    intNumber = Integer.parseInt(numField.getText());
                    setSize(new Dimension (1000,1000));
                    next.removeActionListener(this);
                    nextScreen(centerPanel, next);
                }
                catch (final NumberFormatException ex){
                    intNumber = 0;
                }
            }
        });

        pack();
        setLocationRelativeTo(getParent());
    }

    private void nextScreen (JPanel centerPanel, JButton next){
        remove(centerPanel);
        centerPanel = new JPanel(new FlowLayout());
        add (centerPanel, BorderLayout.CENTER);

        setSize(new Dimension(300, 80+35*intNumber));
        setVisible(true);
        var textVector = new JTextField [4 * intNumber];
        //setResizable(false);
        for (int i=0; i<intNumber; i++){
            centerPanel.add(new JLabel("Interval " + i + ": "));
            for (int j=0; j<4; ++j) {
                textVector[i+j] = new JTextField(3);
                centerPanel.add(textVector[i+j]);
            }           
        }
        
        fVector = new int [2 * intNumber];
        tVector = new int [2 * intNumber];
        next.setText("Done");
        next.addActionListener(new ActionListener (){
            public void actionPerformed(ActionEvent e) { 
                for (int i=0; i<intNumber; i++){
                    for (int j=0; j<2; j++) {
                        try {
                            fVector[i+j] = Integer.parseInt(textVector[i+j].getText());
                            tVector[i+j] = Integer.parseInt(textVector[i+j+2].getText());
                        }
                        catch (final NumberFormatException ex){
                            return;
                        }

                    } 
                }
                    
                setVisible(false);
            }
        });
        
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    public int[][] doModal() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setModal(true);
        setVisible(true);
        return new int[][] {fVector, tVector};
    }
};
