package imagevision;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.image.*;
import java.io.IOException;
import java.nio.file.Files;

import javax.imageio.ImageIO;
import java.io.File;


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
        return new ImageProcessor(buf, fileName);
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
