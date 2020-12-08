package imagevision;

import javax.swing.*;
import javax.swing.event.*;
import javax.imageio.*;
import java.io.IOException;
import java.awt.image.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.*;
import org.jfree.data.statistics.*;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.AbstractRenderer;

public class MainFrame extends JFrame { 

    private static final long serialVersionUID = -477785003793521810L;

    private ArrayList<ImageProcessor> images = new ArrayList<ImageProcessor>();
    private ImageProcessor activeImage;
    private ImagePanel activePanel;
    private JDesktopPane desktopPane = new JDesktopPane();
    private JLabel mouseLabel;
    private Dimension dim;
    private int pixelsSelected = 0;
    private ArrayList<Point> pixels = new ArrayList<Point> (2);
    private State state = State.NONE;
    private JPanel statePanel;


    private enum State {NONE, CROPPING, LINE};
    private Consumer<ImageProcessor> selected = null;
    private int map(int istart, int iend, int ostart, int oend, int val) {
        val = ImageProcessor.clamp(val, istart, iend);
        double slope = (double)(oend - ostart)/(iend - istart);
        return ostart + (int)Math.round(slope * (val - istart));
        
    }
    private double[] getDerivative(double[] f) {
        var der = new double[f.length];
        int i;
        for (i=0; i<f.length-1; i++) {
            der[i] = f[i+1] - f[i];
        }
        der[i] = der[i-1];
        return der;
    }

    private double[] getSmoothed(double [] f) {
        var smo = new double[f.length];
        smo[0] = (f[0] + f[1])/2;
        int i;
        for (i=1; i<f.length-1; i++) {
            smo[i] = (f[i-1] + f[i] + f[i+1])/3;
        }
        smo[i] = (f[i-1] + f[i])/2;
        return smo;
    }

    
    private MouseInputAdapter mouseListener = new MouseInputAdapter(){
        int xB = 0, yB = 0;
        @Override
        public void mouseReleased(MouseEvent e) {
            if (state == State.CROPPING) {
                int x = e.getX();
                int y = e.getY();
                x = map(0, activePanel.getWidth(), 0, activeImage.getImage().getWidth(), x);
                y = map(0, activePanel.getHeight(), 0, activeImage.getImage().getHeight(), y);
                
                System.out.println("xFinal: " + x + " Inicial: " +xB);
                System.out.println("yFinal: " + y + " Inicial: " +yB);
                var oldImg = activeImage.getImage();
                var newImg = new BufferedImage(Math.abs(x-xB), Math.abs(y-yB), oldImg.getType());
                //int iN = 0, jN = 0; 
                xB = Math.min(x, xB);
                yB = Math.min(y, yB);

                for (int i = 0; i < newImg.getWidth(); i++) {
                    for (int j = 0; j < newImg.getHeight(); j++) {
                        newImg.setRGB(i, j, oldImg.getRGB(i+xB, j+yB));
                    }
                }
                addImage(new ImageProcessor(newImg, "Cropped"));
                state = State.NONE;
            }
            return;
        }
        @Override
        public void mousePressed(MouseEvent e) {
            if (state == State.NONE) {
                return;
            }
           
            if (state == State.CROPPING || state == State.LINE) {
                int x = e.getX();
                int y = e.getY();
                x = map(0, activePanel.getWidth(), 0, activeImage.getImage().getWidth(), x);
                y = map(0, activePanel.getHeight(), 0, activeImage.getImage().getHeight(), y);
                xB = x;
                yB = y;
            }

            if (state == State.LINE){
                if (pixelsSelected < 2){
                    pixels.add(new Point (xB,yB));
                    pixelsSelected++;
                }
                if (pixelsSelected == 2){
                    pixelsSelected = 0;
                    state = State.NONE;
                    XYSeries xyseries1 = new XYSeries("Cross Section");
                    double [] data = activeImage.getCrossSection(pixels.get(0), pixels.get(1));
                    
                    pixels.clear();
                    for (int i=0; i<data.length; i++) {
                        xyseries1.add(i, data[i]);
                    }
                    
                    XYSeries xyseries2 = new XYSeries("Derivative");
                    double[] derivative = getDerivative(data);
                    for (int i=0; i<derivative.length; i++) {
                        xyseries2.add(i, derivative[i]);
                    }
    
                    XYSeries xyseries3 = new XYSeries("Smoothed");
                    double[] smoothed = getSmoothed(data);
                    for (int i=0; i<smoothed.length; i++) {
                        xyseries3.add(i, smoothed[i]);
                    }
    
                    XYSeries xyseries4 = new XYSeries("Smoothed derivative");
                    double[] smoothedder = getDerivative(smoothed);
                    for (int i=0; i<smoothedder.length; i++) {
                        xyseries4.add(i, smoothedder[i]);
                    }
    
                    XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
            
                    xySeriesCollection.addSeries(xyseries1);
                    xySeriesCollection.addSeries(xyseries2);
                    xySeriesCollection.addSeries(xyseries3);
                    xySeriesCollection.addSeries(xyseries4);
    
                    XYDataset dataset = xySeriesCollection;//xySeriesCollection;
    
                    JFreeChart jfreechart = ChartFactory.createXYLineChart ( "Image-Cross Section", // title
                                     "Pixel", // categoryAxisLabel (category axis, horizontal axis, X-axis labels)
                                     "Gray Level", // valueAxisLabel (value axis, vertical axis, Y-axis labels)
                            dataset, // dataset
                            PlotOrientation.VERTICAL, true, // legend
                            false, // tooltips
                            false); // URLswidth
            
                            // Use CategoryPlot set various parameters. The following settings can be omitted.
                    XYPlot plot = jfreechart.getXYPlot();
                            // background color transparency
                    plot.setBackgroundAlpha(0.5f);
                            // foreground color transparency
                    plot.setForegroundAlpha(0.5f);

                    var renderer = plot.getRenderer();
                    renderer.setBaseStroke(new BasicStroke(3.0f));    
                    ((AbstractRenderer)renderer).setAutoPopulateSeriesStroke(false);            
    
                    var img = jfreechart.createBufferedImage(700, 400);
                    var panel = new ImagePanel(img);
                    var chartFrame = new JFrame();
                    chartFrame.add(panel);
                    chartFrame.pack();
                    chartFrame.setResizable(false);
                    chartFrame.setVisible(true);
                }   
            }
        }
        @Override
        public void mouseMoved(MouseEvent e) { 
            int x = e.getX();
            int y = e.getY();
            x = map(0, activePanel.getWidth(), 0, activeImage.getImage().getWidth(), x);
            y = map(0, activePanel.getHeight(), 0, activeImage.getImage().getHeight(), y);
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
        activateImgMenus(false);
        add(desktopPane);
        desktopPane.setVisible(true);
	statePanel = new JPanel(new CardLayout());
        mouseLabel = new JLabel("x: y: ");
        statePanel.add(mouseLabel, "Coords");
        statePanel.add(new JLabel("Select an image by clicking it"), "SelImgs");
        add(statePanel, BorderLayout.SOUTH);
        setVisible(true);
    }
     
    public static String chooseFile(boolean read) {
        JFileChooser fc = new JFileChooser();
        int ret;
        fc.addChoosableFileFilter(new Filters.png());

        if (read) {
            ret = fc.showOpenDialog(null);
        }
        else {
            fc.setAcceptAllFileFilterUsed(false);
            ret = fc.showSaveDialog(null);
        }
        
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String filename = file.getAbsolutePath();
            if (!read) {
                if (!filename.endsWith(fc.getFileFilter().getDescription())){
                    filename += fc.getFileFilter().getDescription();
                }
            }
            return filename;
        }
        else {
            return null;
        }
    }
    private void activateImgMenus(boolean activated) {
        var menu = getJMenuBar();
        var fileM = (JMenu)menu.getComponent(0);
        var dataM = (JMenu)menu.getComponent(1);
        var editM = (JMenu)menu.getComponent(2);
        var saveMI = (JMenuItem)fileM.getItem(1);
        var selectMI = (JMenuItem)fileM.getItem(2);

        dataM.setEnabled(activated);
        editM.setEnabled(activated);
        saveMI.setEnabled(activated);
        selectMI.setEnabled(activated);
    }
    private void addImage(ImageProcessor imgP) {
        images.add(imgP);
        var img = imgP.getImage();
        var panel = new ImagePanel(img);
        var internalFrame = new JInternalFrame(imgP.getFileName(),true,true,false,true);
        internalFrame.addInternalFrameListener(new InternalFrameAdapter(){
            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                if (activePanel != null) {
                    activePanel.removeMouseMotionListener(mouseListener);
                    activePanel.removeMouseListener(mouseListener);
                }
                
                var comp = e.getInternalFrame().getContentPane().getComponent(0);
                activePanel = (ImagePanel)comp;
                System.out.println("Detectadio" + getActiveImage().get().getFileName());
                if (selected != null) {
                    chooseStateBar("Coords");
               	    var called = selected;
               	    selected = null;
                    called.accept(getActiveImage().get());
                }
                activeImage = getActiveImage().get();
                
                activePanel.addMouseMotionListener(mouseListener);
                activePanel.addMouseListener(mouseListener);
            }
            public void internalFrameClosed(InternalFrameEvent e) {
                images.remove(getImageFromFrame(e.getInternalFrame()).get());
                if (images.isEmpty()) {
                    activateImgMenus(false);
                }
            }
        });

        internalFrame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                Component cmp = event.getComponent();
                Rectangle rect = cmp.getBounds();
                cmp.setBounds(rect.x, rect.y, rect.width, Math.round(rect.width/panel.ratio));
            }
        });
        internalFrame.add(panel);
        desktopPane.add(internalFrame);
        
        internalFrame.pack();
        internalFrame.setVisible(true);
        activateImgMenus(true);
    }
    private void chooseStateBar(String c) {
        var cl = (CardLayout)(statePanel.getLayout());
    	cl.show(statePanel, c);
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
        var miSave = new JMenuItem("Save");
        menuFile.add(miSave);
        var miSel = new JMenuItem("Select");
        menuFile.add(miSel);

        var miInfo = new JMenuItem("Info");
        menuData.add(miInfo);
        var miHisto = new JMenuItem("Histogram");
        menuData.add(miHisto);
        var miCumHisto = new JMenuItem("Cumulative Histo");
        menuData.add(miCumHisto);
        var miDifference = new JMenuItem("Difference");
        menuData.add(miDifference);
        var miMapChanges = new JMenuItem("Map changes");
        menuData.add(miMapChanges);
        var miCrossSection = new JMenuItem("Image-Cross Section");
        menuData.add(miCrossSection);

        var miInterval = new JMenuItem("Interval Defined");
        menuEdit.add(miInterval);
        var miAdjust = new JMenuItem("Brightness and Contrast");
        menuEdit.add(miAdjust);
        var miEqualize = new JMenuItem("Equalize");
        menuEdit.add(miEqualize);
        var miHistoMatch = new JMenuItem("Histogram Matching");
        menuEdit.add(miHistoMatch);
        var miGammaCorrection = new JMenuItem("Gamma Correction");
        menuEdit.add(miGammaCorrection);
        var miMuestrate = new JMenuItem("Muestrate");
        menuEdit.add(miMuestrate);
        var miDigitalize = new JMenuItem("Digitalize");
        menuEdit.add(miDigitalize);

        // Menu Listeners
        miOpen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String filename = chooseFile(true);
                if (filename == null) {
                    return;
                }
                try {
                    var imgP = new ImageProcessor(filename);
                    addImage(imgP);
                } catch(IOException ex) {}
            }
        });
        miSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String filename = chooseFile(false);
                    if (filename == null){
                        return;
                    }

                    var outputfile = new File(filename);
                    int i = filename.lastIndexOf('.');
                    String extension = filename.substring(i+1);
                    ImageIO.write( activeImage.getImage(), extension,  outputfile);
                }
                catch (final IOException ex) {} 
            }
        });
        miSel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                state = State.CROPPING;
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
                var dataset = new HistogramDataset();
                dataset.setType(HistogramType.FREQUENCY);
                double[] data = activeImage.getGrayValues();
                dataset.addSeries("Histogram", data, 256, 0, 255);
                var histo = getActiveImage().get().getHistogram();

                System.out.println(Arrays.toString(histo));
                String plotTitles = "Histogram - " + activeImage.getFileName();
                String xaxis = "Gray Level";
                String yaxis = "Value";
                
                boolean show = true;
                boolean toolTips = true;
                boolean urls = false;
                var chart = ChartFactory.createHistogram(plotTitles, xaxis, yaxis, dataset, PlotOrientation.VERTICAL, show, toolTips, urls);
                var panel = new ImagePanel(chart.createBufferedImage(700, 400));
                var chartFrame = new JFrame();
                chartFrame.add(panel);
                chartFrame.pack();
                chartFrame.setResizable(false);
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
                if (dialog.doModal()) {
                    ImageProcessor newImg = activeImage.transformFromIntervals(dialog.fVector, dialog.tVector);
                    addImage(newImg);
                }
            }
        });
        miDifference.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*String filename = chooseFile(true);
                if (filename == null) {
                    return;
                }
                try {
                    var imgP = new ImageProcessor(filename);
                    addImage(activeImage.difference(imgP));
                } catch(IOException ex) {}*/
                chooseStateBar("SelImgs");
                selected = (ImageProcessor img) -> addImage(activeImage.difference(img));
            }
        });
        miMapChanges.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*String filename = chooseFile(true);
                if (filename == null) {
                    return;
                }
                try {
                    String thresholdStr = JOptionPane.showInputDialog( MainFrame.this, "Input threshold: ", JOptionPane.QUESTION_MESSAGE);
                    int threshold = Integer.parseInt(thresholdStr);
                    var imgP = new ImageProcessor(filename);
                    addImage(activeImage.changesMap(imgP, threshold));
                    
                } catch(IOException ex) {}*/
                
                String thresholdStr = JOptionPane.showInputDialog( MainFrame.this, "Input threshold: ", JOptionPane.QUESTION_MESSAGE);
                int threshold = Integer.parseInt(thresholdStr);
                chooseStateBar("SelImgs");
                selected = (ImageProcessor img) -> addImage(activeImage.changesMap(img, threshold));
            }
        });
        miCrossSection.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                state = State.LINE;
                pixelsSelected = 0;
            }
        });

        miAdjust.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                var dialog = new AdjustDialog(MainFrame.this, activeImage.getBrightness(), activeImage.getContrast());
                if (dialog.doModal()) {
                    ImageProcessor newImg = activeImage.transformFromBC(dialog.brightness, dialog.contrast);
                    addImage(newImg);
                }
            }
        });
        miEqualize.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addImage(activeImage.equalize());
            }
        });
        miHistoMatch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*String filename = chooseFile(true);
                if (filename == null) {
                    return;
                }
                try {
                    var imgP = new ImageProcessor(filename);
                    addImage(activeImage.histogramMatching(imgP.getNormCumHistogram()));
                } catch(IOException ex) {}*/
                chooseStateBar("SelImgs");
                selected = (ImageProcessor img) -> addImage(activeImage.histogramMatching(img.getNormCumHistogram()));
            }
        });
        miGammaCorrection.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String gamma = JOptionPane.showInputDialog( MainFrame.this, "Input gamma", JOptionPane.QUESTION_MESSAGE);
                try {
                    addImage(activeImage.gammaCorrection(Double.parseDouble(gamma)));
                }
                catch (final NumberFormatException ex) {}
            }
        });
        miMuestrate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String samples = JOptionPane.showInputDialog( MainFrame.this, "Input sample size", JOptionPane.QUESTION_MESSAGE);
                try {
                    addImage(activeImage.muestrate(Integer.parseInt(samples)));
                }
                catch (final NumberFormatException ex) {}
            }
        });
        miDigitalize.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int bits = Integer.parseInt(JOptionPane.showInputDialog( MainFrame.this, "Input bit number", JOptionPane.QUESTION_MESSAGE));
                    int[] tvector = {255, 1<<bits};
                    
                    //int[] tvector = {255, 64};
                    int[] fvector = {0,0};

                    var img = activeImage.transformFromIntervals(fvector, tvector);

                    int []tvector2 = {1<<bits, 255};

                    addImage(img.transformFromIntervals(fvector, tvector2));
                }
                catch (final NumberFormatException ex) {}
            }
        });
        return mb;
    }
} 
