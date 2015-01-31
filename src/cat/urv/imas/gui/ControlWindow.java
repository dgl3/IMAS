package cat.urv.imas.gui;

import cat.urv.imas.agent.CentralAgent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Philipp Oliver on 31/1/15.
 */
public class ControlWindow extends JFrame {
    private final CentralAgent centralAgent;

    public ControlWindow(String title, CentralAgent parent) throws HeadlessException {
        super(title);
        this.centralAgent = parent;
        JButton nextStepBtn = new JButton("Next Step");

        nextStepBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                centralAgent.nextTurn();
            }
        });

        this.add(nextStepBtn);
        pack();
    }
}
