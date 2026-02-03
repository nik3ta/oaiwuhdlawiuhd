package nuclear.control.cmd.macro;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.Value;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static nuclear.utils.IMinecraft.mc;

public class MacroManager {
    private List<Macro> macros = new ArrayList<>();
    private static final File macroFile = new File("C:\\Nuclear\\client_1_16\\Nuclear\\macros.json");

    public List<Macro> getMacros() {
        return macros;
    }

    public boolean isEmpty() {
        return macros.isEmpty();
    }

    public void init() throws IOException {
        if (!macroFile.exists()) {
            macroFile.createNewFile();
        } else {
            readFile();
        }
    }

    public void addMacro(String message, int key) {
        macros.add(new Macro(message, key));
        writeFile();
    }

    public boolean hasMacro(int key) {
        return macros.stream().anyMatch(macro -> macro.getKey() == key);
    }

    public void deleteMacro(int key) {
        if (macros.stream().anyMatch(macro -> macro.getKey() == key)) {
            macros.removeIf(macro -> macro.getKey() == key);
            writeFile();
        }
    }

    public void clearList() {
        if (!macros.isEmpty()) {
            macros.clear();
        }
        writeFile();
    }

    public void onKeyPressed(int key) {
        if (mc.player == null) {
            return;
        }

        macros.stream()
                .filter(macro -> macro.getKey() == key)
                .findFirst()
                .ifPresent(macro -> {
                    try {
                        mc.player.sendChatMessage(macro.getMessage());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    @SneakyThrows
    public void writeFile() {
        StringBuilder builder = new StringBuilder();
        macros.forEach(macro -> builder.append(macro.getMessage())
                .append(":").append(String.valueOf(macro.getKey()).toUpperCase())
                .append("\n"));
        Files.write(macroFile.toPath(), builder.toString().getBytes());
    }

    @SneakyThrows
    private void readFile() {
        FileInputStream fileInputStream = new FileInputStream(macroFile.getAbsolutePath());
        @Cleanup
        BufferedReader reader = new BufferedReader(new InputStreamReader(new DataInputStream(fileInputStream)));
        String line;
        while ((line = reader.readLine()) != null) {
            String curLine = line.trim();
            String[] parts = curLine.split(":");
            if (parts.length == 2) {
                String command = parts[0];
                String key = parts[1];
                macros.add(new Macro(command, Integer.parseInt(key)));
            }
        }
    }

    @Value
    public static class Macro {
        String message;
        int key;
    }
}