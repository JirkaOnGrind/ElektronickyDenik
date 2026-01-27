package com.example.authdemo.config;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.util.Properties;

public class SimpleSshTunnel { // Tady musí být SimpleSshTunnel

    private static Session session;

    public static void start() {
        String sshPassword = System.getenv("SSH_PASSWORD");
        
        if (sshPassword == null || sshPassword.isEmpty()) {
            System.out.println(">>> SSH Tunel: SKIPPING (SSH_PASSWORD not set)");
            return;
        }

        System.out.println(">>> SSH Tunel: FORCING STARTUP...");

        try {
            // Tady si pripadne uprav SSH uzivatele a hosta, pokud se lisi
            String sshHost = "www.provoznidenik.com";
            String sshUser = "provoznidenik_com";
            int sshPort = 22;
            int remoteDbPort = 3306;
            int localPort = 3306;

            JSch jsch = new JSch();
            session = jsch.getSession(sshUser, sshHost, sshPort);
            session.setPassword(sshPassword);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            System.out.println(">>> SSH Tunel: Connecting to " + sshHost + "...");
            session.connect();

            int assignedPort = session.setPortForwardingL(localPort, "127.0.0.1", remoteDbPort);

            System.out.println(">>> SSH Tunel: SUCCESS! 🟢");
            System.out.println(">>> Forwarding: localhost:" + assignedPort + " -> 127.0.0.1:" + remoteDbPort);

        } catch (Exception e) {
            System.err.println("!!! SSH TUNEL FAILED !!!");
            e.printStackTrace();
            System.exit(1);
        }
    }
}