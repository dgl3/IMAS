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
        this.setSize(200, 50);

        JButton nextStepBtn = new JButton("Next Step");

        if( parent == null ) System.out.println("######################## Problem!######################## Problem!######################## Problem!######################## Problem!");

        nextStepBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                centralAgent.nextTurn();
                System.out.println("Hi!");
            }
        });
        this.add(nextStepBtn);
    }
}
