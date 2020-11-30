
package imagevision;

import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;


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