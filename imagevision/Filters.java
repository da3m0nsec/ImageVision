package imagevision;

import java.io.File;

import javax.swing.filechooser.*;

public class Filters {
    public static class png extends FileFilter {
        public boolean accept(File f) {
            return f.getName().endsWith(".png") | f.isDirectory();
        }

        public String getDescription() {
            return ".png";
        }
    }

    public static class bmp extends FileFilter {
        public boolean accept(File f) {
            return f.getName().endsWith(".bmp") | f.isDirectory();
        }

        public String getDescription() {
            return ".bmp";
        }
    }

    public static class jpeg extends FileFilter {
        public boolean accept(File f) {
            return f.getName().endsWith(".jpeg") | f.getName().endsWith(".jpg") | f.isDirectory();
        }

        public String getDescription() {
            return ".jpeg";
        }
    }

}
