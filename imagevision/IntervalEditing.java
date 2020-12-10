package imagevision;

import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;

class IntervalEditDialog extends JDialog {

    private int intNumber;
    public int[] fVector;
    public int[] tVector;
    boolean finished = false;

    public IntervalEditDialog(Frame owner) {
        super(owner);
        createDialog();
    }

    public void createDialog() {
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

        add(centerPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        next.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    intNumber = Integer.parseInt(numField.getText());
                    setSize(new Dimension(1000, 1000));
                    next.removeActionListener(this);
                    nextScreen(centerPanel, next);
                } catch (final NumberFormatException ex) {
                    intNumber = 0;
                }
            }
        });

        pack();
        setLocationRelativeTo(getParent());
    }

    private void nextScreen(JPanel centerPanel, JButton next) {
        remove(centerPanel);
        centerPanel = new JPanel(new FlowLayout());
        add(centerPanel, BorderLayout.CENTER);

        setSize(new Dimension(300, 80 + 35 * intNumber));
        setVisible(true);
        var fTextVector = new JTextField[2 * intNumber];
        var tTextVector = new JTextField[2 * intNumber];

        // setResizable(false);
        int pos = 0;
        for (int i = 0; i < intNumber; i++, pos += 2) {
            centerPanel.add(new JLabel("Interval " + i + ": "));

            fTextVector[pos] = new JTextField(3);
            centerPanel.add(fTextVector[pos]);
            fTextVector[pos + 1] = new JTextField(3);
            centerPanel.add(fTextVector[pos + 1]);

            tTextVector[pos] = new JTextField(3);
            centerPanel.add(tTextVector[pos]);
            tTextVector[pos + 1] = new JTextField(3);
            centerPanel.add(tTextVector[pos + 1]);
        }

        fVector = new int[2 * intNumber];
        tVector = new int[2 * intNumber];
        next.setText("Done");
        next.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0, pos = 0; i < intNumber; i++) {
                    for (int j = 0; j < 2; j++, pos++) {
                        try {

                            int valf = Integer.parseInt(fTextVector[pos].getText());
                            int valt = Integer.parseInt(tTextVector[pos].getText());
                            fVector[pos] = valf;
                            tVector[pos] = valt;
                        } catch (final NumberFormatException ex) {
                            return;
                        }

                    }
                }
                finished = true;
                setVisible(false);
            }
        });

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
        // return new int[][] {fVector, tVector};
    }
};