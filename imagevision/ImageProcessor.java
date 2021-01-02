package imagevision;

import java.awt.*;
import java.awt.image.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import javax.imageio.ImageIO;
import java.io.File;
import java.lang.*;

import java.awt.Point;
import java.lang.Math;

class ImageProcessor {
    private String fileName;
    private BufferedImage image;
    private double[] histogram = new double[256];
    public double[] cumHistogram = new double[256];
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

    public static int clamp(double val, int min, int max) {
        return Math.max(min, Math.min(max, (int) Math.round(val)));
    }

    public ImageProcessor difference(ImageProcessor img) {
        BufferedImage buf = new BufferedImage(img.getWidth(), img.getHeight(), img.getImage().getType());
        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                var pixel = Math.abs(getPixel(i, j) - img.getPixel(i, j));
                buf.setRGB(i, j, new Color(pixel, pixel, pixel).getRGB());
            }
        }
        var dif = new ImageProcessor(buf, "Difference");

        return dif;
    }

    public ImageProcessor changesMap(ImageProcessor img, int threshold) {
        BufferedImage buf = new BufferedImage(img.getWidth(), img.getHeight(), img.getImage().getType());
        var dif = difference(img);
        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                var pixeldif = dif.getPixel(i, j);
                var pixel = getPixel(i, j);
                var color = (pixeldif > threshold ? new Color(255, 0, 0) : new Color(pixel, pixel, pixel)).getRGB();
                buf.setRGB(i, j, color);
            }
        }

        return new ImageProcessor(buf, "Changes map");
    }

    public ImageProcessor histogramMatching(double nch[]) {
        var table = new int[256];

        for (int i = 0; i < table.length; i++) {
            double val = cumHistogram[i] / getSize();
            table[i] = Math.abs(Arrays.binarySearch(nch, val));
        }
        return applyTable(table);
    }

    public ImageProcessor gammaCorrection(double gamma) {
        var table = new int[256];

        for (int i = 0; i < table.length; i++) {
            table[i] = (int) Math.round(Math.pow((double) i / 255, gamma) * 255);
        }
        return applyTable(table);
    }

    public ImageProcessor transformFromBC(double b, double c) {
        var table = new int[256];
        double A = c / contrast;
        double B = b - A * brightness;
        for (int i = 0; i < table.length; i++) {
            table[i] = clamp(Math.round(A * i + B), 0, 255);
        }
        return applyTable(table);
    }

    public ImageProcessor transformFromIntervals(int[] f, int[] t) {
        var table = new int[256];
        for (int i = 0; i < 256; i++) {
            table[i] = i;
        }
        for (int i = 0; i < f.length / 2; i++) {
            int xf = f[i * 2];
            int yf = f[i * 2 + 1];

            int xt = t[i * 2];
            int yt = t[i * 2 + 1];

            double m = ((double) (yt - yf)) / (xt - xf);
            double b = (double) (yt) - (m * xt);

            for (int v = xf; v <= xt; ++v) {
                table[v] = clamp(Math.round(m * v + b), 0, 255);
            }
        }
        return applyTable(table);
    }

    public ImageProcessor equalize() {

        var table = new int[256];
        for (int i = 0; i < 256; i++) {
            table[i] = clamp(cumHistogram[i] / getSize() * (255), 0, 255);
        }
        return applyTable(table);
    }

    public ImageProcessor muestrate(int samples) {
        BufferedImage buf = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        for (int i = 0; i < image.getWidth() - samples; i += samples) {
            for (int j = 0; j < image.getHeight() - samples; j += samples) {
                int newVal = image.getRGB(i, j);
                for (int k = i; k < i + samples; k++) {
                    for (int l = j; l < j + samples; l++) {
                        buf.setRGB(k, l, newVal);
                    }
                }
            }
        }
        return new ImageProcessor(buf, fileName);
    }

    public ImageProcessor applyTable(int[] table) {
        BufferedImage buf = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                int newVal = table[getPixel(i, j)];
                buf.setRGB(i, j, new Color(newVal, newVal, newVal).getRGB());
            }
        }
        return new ImageProcessor(buf, fileName);
    }

    public ImageProcessor hflip () {
        BufferedImage buf = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                int newVal = getPixel(i, j);
                buf.setRGB(getWidth()-i-1, j, new Color(newVal, newVal, newVal).getRGB());
            }
        }
        return new ImageProcessor(buf, fileName);
    } 

    public ImageProcessor vflip () {
        BufferedImage buf = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                int newVal = getPixel(i, j);
                buf.setRGB(i, getHeight()-j-1, new Color(newVal, newVal, newVal).getRGB());
            }
        }
        return new ImageProcessor(buf, fileName);
    } 

    public ImageProcessor rotateRight (int times) {
        var temp = this;
        while (times --> 0){
            temp = temp.rotateOnce();
        }
        return temp;
    }
    
    private ImageProcessor rotateOnce () {
        BufferedImage buf = new BufferedImage(image.getHeight(),image.getWidth(), image.getType());
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                int newVal = getPixel(i, j);
                buf.setRGB(getHeight()-j-1, i, new Color(newVal, newVal, newVal).getRGB());
            }
        }
        return new ImageProcessor(buf, fileName);
    }

    


    public ImageProcessor scale (InterpolationMethods method, int width, int height) {
        BufferedImage buf = new BufferedImage(width, height, image.getType());
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int newVal = method.method(this,buf,i,j);
                buf.setRGB(i, j, new Color(newVal, newVal, newVal).getRGB());
            }
        }
        return new ImageProcessor(buf, fileName);
    }

    public int getPixel(int i, int j) {
        return new Color(image.getRGB(i, j)).getGreen();
    }

    public String getFileName() {
        return fileName;
    }

    public int[] getRange() {
        return new int[] { minGray, maxGray };
    }

    public String getMimeType() {
        return mimeType;
    }

    public BufferedImage getImage() {
        return image;
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }

    public int getSize() {
        return image.getWidth() * image.getHeight();
    }

    public double[] getHistogram() {
        return histogram;
    }

    public double[] getGrayValues() {
        var data = new double[getSize()];
        int k = 0;
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                int pixel = getPixel(i, j);
                data[k++] = pixel;
            }
        }
        return data;
    }

    public double[] getNormCumHistogram() {
        var normHisto = new double[256];
        for (int i = 0; i < normHisto.length; i++) {
            normHisto[i] = cumHistogram[i] / getSize();
        }
        return normHisto;
    }

    public double[] getCrossSection(Point ini, Point end) {
        var arr = new double[Math.abs(end.x - ini.x)];
        double m = (end.y - ini.y) / (end.x - ini.x);
        double b = end.y - (m * end.x);
        int xmin = Math.min(ini.x, end.x);
        int xmax = Math.max(ini.x, end.x);
        for (int i = xmin, j = 0; i < xmax; i++, j++) {
            double result = i * m + b;
            int val = getPixel(i, (int) Math.round(result));
            arr[j] = val;
        }
        return arr;
    }

    public ImageProcessor(String filename) throws IOException {
        fileName = filename;
        var f = new File(filename);
        mimeType = Files.probeContentType(f.toPath());
        if (mimeType != null) {
            mimeType = mimeType.split("/")[1];
        } else {
            throw new IOException("Unsoported format");
        }

        image = ImageIO.read(f);
        if (image == null) {
            throw new IOException("Unsoported format");
        }
        convertToGray();
        init();
    }

    public ImageProcessor(BufferedImage img, String name) {
        image = img;
        fileName = name;
        init();
    }

    private void convertToGray() {
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                var pixel = new Color(image.getRGB(i, j));
                int r = pixel.getRed();
                int g = pixel.getGreen();
                int b = pixel.getBlue();
                int gray = (int) (0.222 * (double) r + 0.707 * (double) g + 0.071 * (double) b);

                image.setRGB(i, j, (new Color(gray, gray, gray)).getRGB());
            }
        }
    }

    private void init() {
        fillHistogram();
        initInfo();
    }

    private void fillHistogram() {
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
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
        for (int i = 1; i < histogram.length; ++i) {
            cumHistogram[i] = histogram[i] + cumHistogram[i - 1];
        }
    }

    private void initInfo() {
        // Brightness
        int sum = 0;
        for (int i = 0; i < histogram.length; ++i) {
            sum += i * histogram[i];
        }
        brightness = (Math.floor((double) sum / getSize() * 100)) / 100;

        // Contrast
        sum = 0;
        for (int i = 0; i < histogram.length; ++i) {
            sum += histogram[i] * (i - brightness) * (i - brightness);
        }

        contrast = (Math.floor(Math.sqrt((1.0 / getSize()) * sum) * 100)) / 100;

        // Entropy
        double sumd = 0;
        for (int i = 0; i < histogram.length; ++i) {
            double f = (double) histogram[i] / getSize();
            if (f == 0) {
                continue;
            }
            double log2f = Math.log(f) / Math.log(2.0);

            sumd += f * log2f;
        }
        entropy = (Math.floor(-sumd * 100)) / 100;
    }
}

interface InterpolationMethods {
    public int method (ImageProcessor in, BufferedImage out, int x, int y);

    public static int nearestNeighbour(ImageProcessor in, BufferedImage out, int x, int y) {
        var Xnear = (int) (((float) x) / out.getWidth() * (in.getWidth()-1));
        var Ynear = (int) (((float) y) / out.getHeight() * (in.getHeight()-1));
        return in.getPixel(Xnear,Ynear);
    }

    public static int bilinearAdjust (ImageProcessor in, BufferedImage out, int x, int y) {
        //esto me suena, está feo
        float gx = ((float) x) / out.getWidth() * (in.getWidth() -1);
        float gy = ((float) y) / out.getHeight() * (in.getHeight() -1);

        int gxi = (int) gx; //????????????
        int gyi = (int) gy; //????????????

        int grey = 0;

        int a = in.getPixel(gxi+1, gyi);
        int b = in.getPixel(gxi+1, gyi+1);
        int c = in.getPixel(gxi, gyi);
        int d = in.getPixel(gxi, gyi+1);

        int p = x-gxi;
        int q = y-gyi;

        grey = ImageProcessor.clamp(c+(d-c)*p+(a-c)*q+(b+c-a-d)*p*q,0,255);
        /*
        for (int i=0; i<=2; ++i){
            float b00 = get(c00, i);
            float b10 = get(c10, i);
            float b01 = get(c01, i);
            float b11 = get(c11, i);

            int ble = ((int) blerp(b00,b10,b01,b11,gx-gxi,gy-gyi)) << (8*i);
            grey = grey | ble;
        }
        */
        return grey;
    }
}
