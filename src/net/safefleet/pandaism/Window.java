package net.safefleet.pandaism;

import javax.swing.*;
import java.awt.*;

public class Window {
    private JPanel basePanel;

    private JButton updateMCUButton;
    private JButton updateFirmwareButton;
    private JButton certificateButton;
    private JButton wipeButton;

    private JLabel mcuVersionLabel;
    private JLabel firmwareVersionLabel;
    private JLabel serialnumberLabel;

    public Window(String serialNumber, String firmwareVersion, String mcuVersion) {
        this.serialnumberLabel.setText(serialNumber);
        this.firmwareVersionLabel.setText(firmwareVersion);
        this.mcuVersionLabel.setText(mcuVersion);

        if(this.mcuVersionLabel.getText().equals("0x0147")) {
            this.mcuVersionLabel.setForeground(new Color(39, 149, 70));
        }

        if(this.firmwareVersionLabel.getText().equals("V03.00.00.15")) {
            this.firmwareVersionLabel.setForeground(new Color(39, 149, 70));
        }

        this.updateFirmwareButton.addActionListener(actionEvent -> {
            X2CrossChecker.currentState = X2CrossChecker.STATES.UPDATE_FIRMWARE;
            X2CrossChecker.wipeBypass = true;
        });
        this.updateMCUButton.addActionListener(actionEvent -> {
            X2CrossChecker.currentState = X2CrossChecker.STATES.UPDATE_MCU;
            X2CrossChecker.wipeBypass = true;
        });
        this.certificateButton.addActionListener(actionEvent -> {
            X2CrossChecker.currentState = X2CrossChecker.STATES.CERTIFY;
            X2CrossChecker.wipeBypass = true;
        });
        this.wipeButton.addActionListener(actionEvent -> {
            X2CrossChecker.currentState = X2CrossChecker.STATES.DELETE_DRIVE;
            X2CrossChecker.wiped = true;
        });
    }

    public JPanel getBasePanel() {
        return basePanel;
    }
}
