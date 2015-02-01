package cat.urv.imas.gui;

import cat.urv.imas.agent.CentralAgent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Created by Philipp Oliver on 31/1/15.
 */
public class ControlWindow extends JFrame {
    private final CentralAgent centralAgent;
    private final JButton nextStepBtn;

    public ControlWindow(String title, CentralAgent parent) throws HeadlessException {
        super(title);
        this.centralAgent = parent;
        parent.setControllerWindow(this);
        this.setLayout( new FlowLayout() );
        this.setLocation(50, 100);

        nextStepBtn = new JButton("Next Step");
        nextStepBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                centralAgent.nextTurn();
            }
        });
        nextStepBtn.setMnemonic(KeyEvent.VK_N);

        JButton quitBtn = new JButton("Quit");
        quitBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        this.add(nextStepBtn);
        //this.add(quitBtn);

        this.getRootPane().setDefaultButton(nextStepBtn);
        nextStepBtn.requestFocusInWindow();
        pack();
    }

    public void setReadyForNewTurn(boolean readyForNewTurn) {
        nextStepBtn.setEnabled(readyForNewTurn);
    }
}
