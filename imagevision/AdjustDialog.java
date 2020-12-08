package imagevision;

import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;

class AdjustDialog extends JDialog{
    public double brightness, contrast;
    private boolean finished = false;

    public AdjustDialog (Frame owner, double oldB, double oldC){
        super(owner);
        createDialog(oldB, oldC);
    }

    public void createDialog (double oldB, double oldC){
        setPreferredSize(new Dimension(200, 140));
        setLayout(new BorderLayout());
        setTitle("Adjust Brightness and Contrast");
        
        var done = new JButton("Done");
        var centerPanel = new JPanel(new FlowLayout());
        centerPanel.add(new JLabel("Brightness: "));

        var brightField = new JTextField(6);
        brightField.setText(String.valueOf(oldB));
        centerPanel.add(brightField);
        centerPanel.add(new JLabel("Contrast: "));

        var contrastField = new JTextField(6);
        contrastField.setText(String.valueOf(oldC));
        centerPanel.add(contrastField);


        var southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(done);

        add (centerPanel, BorderLayout.CENTER);
        add (southPanel, BorderLayout.SOUTH);

        done.addActionListener(new ActionListener (){
            public void actionPerformed(ActionEvent e) { 
                try {
                    brightness = Double.parseDouble(brightField.getText());
                    contrast = Double.parseDouble(contrastField.getText());
                }
                catch (final NumberFormatException ex){
                    return;
                }
                finished = true;
                setVisible(false);
            }
        });

        pack();
        setLocationRelativeTo(getParent());
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    public boolean doModal() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setModal(true);
        setVisible(true);
        return finished;
        //return new int[][] {fVector, tVector};
    }
};
