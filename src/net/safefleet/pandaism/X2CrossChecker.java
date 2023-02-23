package net.safefleet.pandaism;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class X2CrossChecker {
    private enum STATES {
        WAITING_FOR_DRIVE, POST_DETECTION, DELETE_DRIVE, IDLE
    }

    private static boolean running = false;

    public static void main(String[] args) throws InterruptedException {
        Map<Character, String> driveMap = new HashMap<>();
        STATES currentState = STATES.WAITING_FOR_DRIVE;

        int startResult = JOptionPane.showConfirmDialog(null, "Do you want to start BWC background checker?", "BWC Background Detective", JOptionPane.YES_NO_OPTION);

        if(startResult == JOptionPane.YES_OPTION) {
            running = true;
        }

        while(running) {
            switch (currentState) {
                case WAITING_FOR_DRIVE:
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
                        int result = JOptionPane.showConfirmDialog(null, "BWC mounting detect, please make sure this unit is completely wipe before packing and shipping. To wipe with this interface, please click Yes", "Detected BWC", JOptionPane.YES_NO_OPTION);

                        if(result == JOptionPane.YES_OPTION) {
                            currentState = STATES.DELETE_DRIVE;
                        } else {
                            currentState = STATES.IDLE;
                        }
                    } else {
                        currentState = STATES.WAITING_FOR_DRIVE;
                    }
                    break;
                case DELETE_DRIVE:
                    File[] deleteRoots = File.listRoots();
                    File bwcRoot = null;

                    for(File root : deleteRoots) {
                        FileSystemView fileSystemView = FileSystemView.getFileSystemView();
                        if(fileSystemView != null) {
                            String systemDisplayName = fileSystemView.getSystemDisplayName(root);

                            if(systemDisplayName.length() > 0) {
                                if(systemDisplayName.substring(0, systemDisplayName.lastIndexOf(" ")).equals("FOCUS-X1")) {
                                    bwcRoot = root;
                                }
                            }
                        }
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
                            if(systemDisplayName.length() > 0) {
                                disconnectMap.put(root.getAbsolutePath().charAt(0), systemDisplayName.substring(0, systemDisplayName.lastIndexOf(" ")));
                            }
                        }
                    }

                    if(!disconnectMap.containsValue("FOCUS-X1")) {
                        currentState = STATES.WAITING_FOR_DRIVE;
                        driveMap = new HashMap<>();
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
