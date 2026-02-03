package nuclear.ui.alt;


import nuclear.control.Manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;

public class AltConfig {

    public static final File file = new File("C:\\Nuclear\\client_1_16\\Nuclear\\altmanager.json");

    public void init() throws Exception {
        if (!file.exists()) {
            file.createNewFile();
        } else {
            readAlts();
        }
    }

    public static void updateFile() {
        try {
            StringBuilder builder = new StringBuilder();
            for (Account alt : Manager.ALT.accounts) {
                builder.append(alt.accountName + ":" + alt.dateAdded + ":" + alt.pinned).append("\n");
            }
            Files.write(file.toPath(), builder.toString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readAlts() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.getAbsolutePath())));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                String username = parts[0];
                long dateAdded = Long.parseLong(parts[1]);
                Account account = new Account(username, dateAdded);

                if (parts.length > 2) {
                    account.pinned = Boolean.parseBoolean(parts[2]);
                }
                Manager.ALT.accounts.add(account);
            }

            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}