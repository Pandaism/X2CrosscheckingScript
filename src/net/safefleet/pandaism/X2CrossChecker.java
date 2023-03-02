package net.safefleet.pandaism;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class X2CrossChecker {
    public enum STATES {
        WAITING_FOR_DRIVE, POST_DETECTION, DELETE_DRIVE, IDLE, UPDATE_MCU, UPDATE_FIRMWARE, CERTIFY
    }

    private static boolean running = false;
    public static STATES currentState = STATES.WAITING_FOR_DRIVE;
    public static boolean wipeBypass = false;
    public static boolean wiped = false;

    private static File getBWCRootDirectory() {
        File[] deleteRoots = File.listRoots();

        for(File root : deleteRoots) {
            FileSystemView fileSystemView = FileSystemView.getFileSystemView();
            if (fileSystemView != null) {
                String systemDisplayName = fileSystemView.getSystemDisplayName(root);

                if (systemDisplayName != null) {
                    if (systemDisplayName.length() > 0) {
                        if (systemDisplayName.substring(0, systemDisplayName.lastIndexOf(" ")).equals("FOCUS-X1")) {
                            return root;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static void main(String[] args) throws InterruptedException {
        Map<Character, String> driveMap = new HashMap<>();
        File bwcRoot = null;
        String serialNumber = "";
        String firmwareVersion = "";
        String mcuVersion = "";
        JFrame frame = new JFrame("BWC Detected");

        int startResult = JOptionPane.showConfirmDialog(null, "Do you want to start BWC background checker?", "BWC Background Detective", JOptionPane.YES_NO_OPTION);

        if(startResult == JOptionPane.YES_OPTION) {
            running = true;
        }

        while(running) {
            switch (currentState) {
                case WAITING_FOR_DRIVE:
                    wipeBypass = false;
                    wiped = false;

                    File[] wfdRoots = File.listRoots();
                    for (File root : wfdRoots) {
                        char driveLetter = root.getAbsolutePath().charAt(0);

                        if(!driveMap.containsKey(driveLetter)) {
                            String systemDisplayName = FileSystemView.getFileSystemView().getSystemDisplayName(root);
                            driveMap.put(driveLetter, systemDisplayName.substring(0, systemDisplayName.lastIndexOf(" ")));
                        }
                    }
                    currentState = STATES.POST_DETECTION;
                    break;
                case POST_DETECTION:
                    if(driveMap.containsValue("FOCUS-X1")) {
                        bwcRoot = getBWCRootDirectory();

                        if(bwcRoot != null) {
                            File serialNumberFile = new File(bwcRoot.getAbsolutePath() + "/USBSEL.lct");
                            File versionFile = new File(bwcRoot.getAbsolutePath() + "/version.json");
                            try {
                                if(serialNumberFile.exists()) {
                                    BufferedReader reader = new BufferedReader(new FileReader(serialNumberFile));
                                    String line;
                                    while((line = reader.readLine()) != null) {
                                        serialNumber = line;
                                    }
                                    reader.close();
                                }

                                if(versionFile.exists()) {
                                    BufferedReader reader = new BufferedReader(new FileReader(versionFile));
                                    String line;
                                    while((line = reader.readLine()) != null) {
                                        if(line.contains("BWC Firmware")) {
                                            firmwareVersion = line.substring(line.lastIndexOf(" ") + 1, line.length() - 2);
                                        }

                                        if(line.contains("MCU Firmware")) {
                                            mcuVersion = line.substring(line.lastIndexOf("\t") + 2, line.length() - 2);
                                        }
                                    }
                                    reader.close();
                                }

                                frame.setContentPane(new Window(serialNumber, firmwareVersion, mcuVersion).getBasePanel());
                                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                                frame.pack();
                                frame.setVisible(true);

                                JOptionPane.showMessageDialog(null, "Be sure to wipe the camera before packing.");
                                currentState = STATES.IDLE;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    } else {
                        currentState = STATES.WAITING_FOR_DRIVE;
                    }
                    break;
                case UPDATE_MCU:
                    try {
                        File[] mcuFile = new File("./X2Crosschecking/mcu/").listFiles();

                        if(mcuFile != null) {
                            if(bwcRoot != null) {
                                File hex = new File(bwcRoot.getAbsolutePath() + "MCU.hex");
                                if(!hex.exists()) {
                                    Files.copy(mcuFile[0].toPath(), hex.toPath());
                                    JOptionPane.showMessageDialog(null, "Please eject the BWC from dock to start MCU Update Process");
                                    currentState = STATES.IDLE;

                                    Desktop desktop = Desktop.getDesktop();
                                    try {
                                        desktop.open(bwcRoot);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case UPDATE_FIRMWARE:
                    try {
                        File[] firmwareFiles = new File("./X2Crosschecking/firmware/").listFiles();

                        if(firmwareFiles != null) {
                            if(bwcRoot != null) {
                                deleteFiles(bwcRoot);

                                for(File file : firmwareFiles) {
                                    Files.copy(file.toPath(), new File(bwcRoot.getAbsolutePath() + "/" + file.getName()).toPath());
                                }

                                Desktop desktop = Desktop.getDesktop();
                                try {
                                    desktop.open(bwcRoot);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                JOptionPane.showMessageDialog(null, "Please eject the BWC from dock to start Firmware Update Process");
                                currentState = STATES.IDLE;

                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case CERTIFY:
                    try {
                        File[] certificateFiles = new File("./X2Crosschecking/certificate/").listFiles();
                        if(certificateFiles != null) {
                            if(bwcRoot != null) {
                                deleteFiles(bwcRoot);

                                for(File file : certificateFiles) {
                                    Files.copy(file.toPath(), new File(bwcRoot.getAbsolutePath() + "/" + file.getName()).toPath());

                                }

                                Desktop desktop = Desktop.getDesktop();
                                try {
                                    desktop.open(bwcRoot);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                JOptionPane.showMessageDialog(null, "Please eject the BWC from dock and start certification process");
                                currentState = STATES.IDLE;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case DELETE_DRIVE:
                    if(bwcRoot == null) {
                        bwcRoot = getBWCRootDirectory();
                    }

                    if(bwcRoot != null) {
                        String message = deleteFiles(bwcRoot);
                        long freeSpace = bwcRoot.getFreeSpace();

                        Desktop desktop = Desktop.getDesktop();
                        try {
                            desktop.open(bwcRoot);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if(freeSpace <= 63_987_908_608L) {
                            JOptionPane.showMessageDialog(null, message + "\nFree Space on the drive is " + freeSpace + " bytes.\nBWC is now empty. Please process with testing process.");
                        } else {
                            JOptionPane.showMessageDialog(null, message + "\nFree Space on the drive is " + freeSpace + " bytes.\nThere must be something wrong with the unit, please check the files to see if the delete process was successful.");
                        }
                        currentState = STATES.IDLE;
                    }
                    break;
                case IDLE:
                    File[] idleRoots = File.listRoots();
                    Map<Character, String> disconnectMap = new HashMap<>();

                    for(File root : idleRoots) {
                        FileSystemView fileSystemView = FileSystemView.getFileSystemView();
                        if(fileSystemView != null) {
                            String systemDisplayName = fileSystemView.getSystemDisplayName(root);
                            if(systemDisplayName != null) {
                                if(systemDisplayName.length() > 0) {
                                    disconnectMap.put(root.getAbsolutePath().charAt(0), systemDisplayName.substring(0, systemDisplayName.lastIndexOf(" ")));
                                }
                            }
                        }
                    }

                    if(!disconnectMap.containsValue("FOCUS-X1")) {
                        currentState = STATES.WAITING_FOR_DRIVE;
                        driveMap = new HashMap<>();
                        bwcRoot = null;
                        serialNumber = "";
                        firmwareVersion = "";
                        mcuVersion = "";
                        frame.dispose();

                        if(!wipeBypass) {
                            if(!wiped) {
                                JOptionPane.showMessageDialog(null, "BWC was not wiped, Please double-checked the camera recently ejected.", "ERROR", JOptionPane.WARNING_MESSAGE);
                            }
                        }
                    }
                    break;
            }
            Thread.sleep(125);
        }
    }

    private static String deleteFiles(File directory) {
        StringBuilder result = new StringBuilder();
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    long fileSize = file.length();
                    if (file.isDirectory()) {
                        String childResult = deleteFiles(file);
                        if (!childResult.isEmpty()) {
                            result.append(childResult);
                            result.append('\n');
                        }
                    }
                    file.delete();
                    result.append("Deleted ");
                    result.append(fileName);
                    result.append(" (");
                    result.append(fileSize);
                    result.append(" bytes)\n");
                }
            }
        }
        return result.toString();
    }

}
