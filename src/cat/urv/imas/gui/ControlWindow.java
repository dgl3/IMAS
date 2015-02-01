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
    private final JButton autoPlayBtn;

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
                centralAgent.newTurn();
            }
        });

        autoPlayBtn = new JButton("Play");
        autoPlayBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (centralAgent.isAutoPlay()) {
                    autoPlayBtn.setText("Play");
                    autoPlayBtn.setEnabled(centralAgent.isReadyForNextTurn());
                    centralAgent.setAutoPlay(false);
                } else {
                    autoPlayBtn.setText("Pause");
                    centralAgent.setAutoPlay(true);
                }

            }
        });


        this.add(nextStepBtn);
        this.add(autoPlayBtn);

        this.getRootPane().setDefaultButton(nextStepBtn);
        nextStepBtn.requestFocusInWindow();
        pack();
    }

    public void setReadyForNewTurn(boolean readyForNewTurn) {
        nextStepBtn.setEnabled(readyForNewTurn);

        if( autoPlayBtn.getText().equals("Play") ){
            autoPlayBtn.setEnabled(readyForNewTurn);
        }
    }
}
