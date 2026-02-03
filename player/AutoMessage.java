package nuclear.module.impl.player;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventDestroyTotem;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.impl.combat.AttackAura;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.utils.font.ReplaceUtil;

import java.util.Random;

@Annotation(
        name = "AutoMessage",
        type = TypeList.Player,
        desc = "Автоматически фразы убийства в чате"
)
public class AutoMessage extends Module {
    private static MultiBoxSetting setting = new MultiBoxSetting("Писать при",
            new BooleanSetting("Убистве", true),
            new BooleanSetting("Тотеме", false));

    public BooleanSetting soft = new BooleanSetting("Упоминать чит", true).setVisible(() -> setting.get("Убистве"));

    private static final String[] killMessages = {
        " соси мою хуяку червяк by Nuc!ear",
        " разъебал сучку посиди на спавне by Nuc!ear",
        " котак пас чо с ебалом by Nuc!ear",
        " хоть какой-то отпор дай шалава by Nuc!ear",
        " выебаный в рот уёбок сиди на спавне by Nuc!ear",
        " пососал хуй теперь сиди на спавне by Nuc!ear",
        " пососал хуй теперь поплачь by Nuc!ear",
        " пососал хуй теперь плачь на спавне by Nuc!ear",
        " пацан Nuc!ear не дал бы тебе так умереть",
        " пососал хуй теперь сиди на спавне хуйня by Nuc!ear",
        " проебался как сука иди на спавн блядуха by Nuc!ear",
        " ты ногами играешь или почему на столько слабый by Nuc!ear"
    };

    private static final String[] catlavanMessages = {
        " чо-то твой горлаван хуйяку пососал by Nuc!ear",
        " твой горлаван хуйяку пососал by Nuc!ear",
        " чо-то твой горлаван хуй пососал by Nuc!ear"
    };

    private static final String[] energyMessages = {
        " ебать литэнерджи банально тотем не берёт by Nuc!ear",
        " чо твой литэнерджи хуяку глотает by Nuc!ear"
    };

    private static final String[] newcodeMessages = {
        " чо-то твой ньюкод хуйяку пососал by Nuc!ear",
        " твой ньюкод хуйяку пососал by Nuc!ear",
        " чо-то твой ньюкод хуй пососал by Nuc!ear"
    };

    private static final String[] monotonMessages = {
        " чо-то твой монотон хуйяку пососал by Nuc!ear",
        " твой монотон хуйяку пососал by Nuc!ear",
        " чо-то твой монотон хуй пососал by Nuc!ear"
    };

    private static final String[] quickClientMessages = {
        " твой квикалл сакает по страшному by Nuc!ear",
        " пиздец с квикаллом на спавн by Nuc!ear"
    };

    private static final String[] dimasikMessages = {
        " чо-то твой дилдасик хуйяку пососал by Nuc!ear",
        " твой дилдасик хуйяку пососал by Nuc!ear",
        " чо-то твой дилдасик хуй пососал by Nuc!ear"
    };

    private static final String[] valhalaMessages = {
        " чо-то твой пахлава хуйяку пососал by Nuc!ear",
        " твой пахлава хуйяку пососал by Nuc!ear",
        " чо-то твой пахлава хуй пососал by Nuc!ear"
    };

    private static final String[] totemMessages = {
        "{name} ливай сучка дееспособная by Nuc!ear",
        " чо {name} пастoчка твоя наяривает? by Nuc!ear",
        "{name} слабость убегай пока не трахнул твою мамочку by Nuc!ear",
        "{name} ты буквально дырка от бублика by Nuc!ear",
        "{name} ты просто попуск Nuc!ear ебёт твою пасту",
        "чо с ебалом {name} пиздец Nuc!ear тебя ебёт"
    };

    private final Random random = new Random();
    private float lastTargetHealth = -1.0f;
    private boolean messageSent = false;
    private int lastMessageIndex = -1;
    private int lastTotemMessageIndex = -1;
    private long lastMessageTime = 0;
    private String pendingKillMessage = null;
    private long pendingKillMessageTime = 0;

    public AutoMessage() {
        this.addSettings(setting, soft);
    }

    /**
     * Заменяет одну случайную английскую букву на похожую русскую в нике
     */
    private String replaceRandomLetter(String name) {
        java.util.Map<Character, Character> letterMap = new java.util.HashMap<>();
        letterMap.put('p', 'р');
        letterMap.put('a', 'а');
        letterMap.put('e', 'е');
        letterMap.put('o', 'о');
        letterMap.put('k', 'к');
        letterMap.put('c', 'с');
        letterMap.put('x', 'х');
        letterMap.put('y', 'у');
        letterMap.put('P', 'Р');
        letterMap.put('A', 'А');
        letterMap.put('E', 'Е');
        letterMap.put('O', 'О');
        letterMap.put('K', 'К');
        letterMap.put('T', 'Т');
        letterMap.put('H', 'Н');
        letterMap.put('B', 'В');
        letterMap.put('M', 'М');
        letterMap.put('C', 'С');
        letterMap.put('X', 'Х');
        letterMap.put('Y', 'У');
        letterMap.put('r', 'г');
        letterMap.put('n', 'п');
        letterMap.put('N', 'И');
        letterMap.put('m', 'м');
        letterMap.put('b', 'ь');
        letterMap.put('w', 'ш');
        letterMap.put('W', 'Ш');
        letterMap.put('0', 'O');

        java.util.List<Integer> replaceablePositions = new java.util.ArrayList<>();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (letterMap.containsKey(ch)) {
                replaceablePositions.add(i);
            }
        }

        if (replaceablePositions.isEmpty()) {
            return name;
        }

        int randomPosition = replaceablePositions.get(random.nextInt(replaceablePositions.size()));
        char originalChar = name.charAt(randomPosition);
        char russianChar = letterMap.get(originalChar);

        StringBuilder modified = new StringBuilder(name);
        modified.setCharAt(randomPosition, russianChar);
        return modified.toString();
    }

    @Override
    public boolean onEvent(Event event) {
        if (!state) {
            return false;
        }

        if (setting.get("Тотеме") && event instanceof EventDestroyTotem) {
            EventDestroyTotem totemEvent = (EventDestroyTotem) event;
            if (mc.player == null) {
                return false;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMessageTime < 15000) {
                return false;
            }

            if (totemEvent.entity instanceof PlayerEntity) {
                PlayerEntity target = (PlayerEntity) totemEvent.entity;
                LivingEntity auraTarget = AttackAura.getTarget();
                if (target != mc.player && auraTarget != null && target == auraTarget) {
                    String targetName = target.getName().getString();
                    String displayName = target.getDisplayName().getString();

                    String processedDisplayName = ReplaceUtil.replaceCustomFonts(displayName);
                    String displayNameLower = processedDisplayName.toLowerCase();

                    boolean isCatlavan = displayNameLower.contains("catlavan");

                    String modifiedName;
                    if (isCatlavan) {
                        modifiedName = replaceRandomLetter(targetName);
                    } else {
                        modifiedName = replaceRandomLetter(targetName);
                    }

                    java.util.List<Integer> availableIndices = new java.util.ArrayList<>();
                    for (int i = 0; i < totemMessages.length; i++) {
                        if (!soft.get() && totemMessages[i].contains("Nuc!ear") && !totemMessages[i].contains("by Nuc!ear")) {
                            continue;
                        }
                        availableIndices.add(i);
                    }

                    if (!availableIndices.isEmpty()) {
                        int messageIndex;
                        do {
                            messageIndex = availableIndices.get(random.nextInt(availableIndices.size()));
                        } while (messageIndex == lastTotemMessageIndex && availableIndices.size() > 1);

                        lastTotemMessageIndex = messageIndex;
                        String totemMessage = totemMessages[messageIndex];

                        totemMessage = totemMessage.replace("{name}", modifiedName);

                        if (!soft.get() && totemMessage.endsWith(" by Nuc!ear")) {
                            totemMessage = totemMessage.substring(0, totemMessage.length() - " by Nuc!ear".length());
                        }

                        String message = totemMessage;
                        mc.player.sendChatMessage(message);
                        lastMessageTime = currentTime;
                    }
                }
            }
            return false;
        }

        if (!setting.get("Убистве")) {
            return false;
        }

        if (event instanceof EventUpdate) {
            if (mc.player == null) {
                return false;
            }

            long currentTime = System.currentTimeMillis();

            if (pendingKillMessage != null && currentTime - pendingKillMessageTime >= 3000) {
                mc.player.sendChatMessage(pendingKillMessage);
                lastMessageTime = currentTime;
                pendingKillMessage = null;
                pendingKillMessageTime = 0;
            }

            LivingEntity target = AttackAura.getTarget();

            if (target != null) {
                if (!(target instanceof PlayerEntity) || target == mc.player) {
                    lastTargetHealth = -1.0f;
                    messageSent = false;
                    return false;
                }

                float currentHealth = target.getHealth();

                if (currentHealth <= 0.05f && lastTargetHealth > 0.0f && !messageSent) {
                    String targetName = target.getName().getString();
                    String displayName = target.getDisplayName().getString();

                    String processedDisplayName = ReplaceUtil.replaceCustomFonts(displayName);
                    String displayNameLower = processedDisplayName.toLowerCase();

                    boolean isCatlavan = displayNameLower.contains("catlavan");
                    boolean isEnergy = displayNameLower.contains("energy");
                    boolean isQuickClient = displayNameLower.contains("quickclient");
                    boolean isDimasik = displayNameLower.contains("dimasik");
                    boolean isValhala = displayNameLower.contains("valhala");
                    boolean isNewcode = displayNameLower.contains("newcode");
                    boolean isMonoton = displayNameLower.contains("monoton");

                    String modifiedName;
                    String randomMessage;

                    if (isCatlavan) {
                        modifiedName = replaceRandomLetter(targetName);;

                        boolean useSpecial = random.nextBoolean();
                        String[] messagesToUse = useSpecial ? catlavanMessages : killMessages;

                        java.util.List<Integer> availableIndices = new java.util.ArrayList<>();
                        for (int i = 0; i < messagesToUse.length; i++) {
                            if (!soft.get() && messagesToUse[i].contains("Nuc!ear") && !messagesToUse[i].contains("by Nuc!ear")) {
                                continue;
                            }
                            availableIndices.add(i);
                        }

                        if (!availableIndices.isEmpty()) {
                            int messageIndex = availableIndices.get(random.nextInt(availableIndices.size()));
                            randomMessage = messagesToUse[messageIndex];
                            if (!soft.get() && randomMessage.endsWith(" by Nuc!ear")) {
                                randomMessage = randomMessage.substring(0, randomMessage.length() - " by Nuc!ear".length());
                            }
                        } else {
                            return false;
                        }
                    } else if (isEnergy) {
                        modifiedName = replaceRandomLetter(targetName);;

                        boolean useSpecial = random.nextBoolean();
                        String[] messagesToUse = useSpecial ? energyMessages : killMessages;

                        java.util.List<Integer> availableIndices = new java.util.ArrayList<>();
                        for (int i = 0; i < messagesToUse.length; i++) {
                            if (!soft.get() && messagesToUse[i].contains("Nuc!ear") && !messagesToUse[i].contains("by Nuc!ear")) {
                                continue;
                            }
                            availableIndices.add(i);
                        }

                        if (!availableIndices.isEmpty()) {
                            int messageIndex = availableIndices.get(random.nextInt(availableIndices.size()));
                            randomMessage = messagesToUse[messageIndex];
                            if (!soft.get() && randomMessage.endsWith(" by Nuc!ear")) {
                                randomMessage = randomMessage.substring(0, randomMessage.length() - " by Nuc!ear".length());
                            }
                        } else {
                            return false;
                        }
                    } else if (isQuickClient) {
                        modifiedName = replaceRandomLetter(targetName);;

                        boolean useSpecial = random.nextBoolean();
                        String[] messagesToUse = useSpecial ? quickClientMessages : killMessages;

                        java.util.List<Integer> availableIndices = new java.util.ArrayList<>();
                        for (int i = 0; i < messagesToUse.length; i++) {
                            if (!soft.get() && messagesToUse[i].contains("Nuc!ear") && !messagesToUse[i].contains("by Nuc!ear")) {
                                continue;
                            }
                            availableIndices.add(i);
                        }

                        if (!availableIndices.isEmpty()) {
                            int messageIndex = availableIndices.get(random.nextInt(availableIndices.size()));
                            randomMessage = messagesToUse[messageIndex];
                            if (!soft.get() && randomMessage.endsWith(" by Nuc!ear")) {
                                randomMessage = randomMessage.substring(0, randomMessage.length() - " by Nuc!ear".length());
                            }
                        } else {
                            return false;
                        }
                    } else if (isDimasik) {
                        modifiedName = replaceRandomLetter(targetName);;

                        boolean useSpecial = random.nextBoolean();
                        String[] messagesToUse = useSpecial ? dimasikMessages : killMessages;

                        java.util.List<Integer> availableIndices = new java.util.ArrayList<>();
                        for (int i = 0; i < messagesToUse.length; i++) {
                            if (!soft.get() && messagesToUse[i].contains("Nuc!ear") && !messagesToUse[i].contains("by Nuc!ear")) {
                                continue;
                            }
                            availableIndices.add(i);
                        }

                        if (!availableIndices.isEmpty()) {
                            int messageIndex = availableIndices.get(random.nextInt(availableIndices.size()));
                            randomMessage = messagesToUse[messageIndex];
                            if (!soft.get() && randomMessage.endsWith(" by Nuc!ear")) {
                                randomMessage = randomMessage.substring(0, randomMessage.length() - " by Nuc!ear".length());
                            }
                        } else {
                            return false;
                        }
                    } else if (isValhala) {
                        modifiedName = replaceRandomLetter(targetName);;

                        boolean useSpecial = random.nextBoolean();
                        String[] messagesToUse = useSpecial ? valhalaMessages : killMessages;

                        java.util.List<Integer> availableIndices = new java.util.ArrayList<>();
                        for (int i = 0; i < messagesToUse.length; i++) {
                            if (!soft.get() && messagesToUse[i].contains("Nuc!ear") && !messagesToUse[i].contains("by Nuc!ear")) {
                                continue;
                            }
                            availableIndices.add(i);
                        }

                        if (!availableIndices.isEmpty()) {
                            int messageIndex = availableIndices.get(random.nextInt(availableIndices.size()));
                            randomMessage = messagesToUse[messageIndex];
                            if (!soft.get() && randomMessage.endsWith(" by Nuc!ear")) {
                                randomMessage = randomMessage.substring(0, randomMessage.length() - " by Nuc!ear".length());
                            }
                        } else {
                            return false;
                        }
                    } else if (isNewcode) {
                        modifiedName = replaceRandomLetter(targetName);;

                        boolean useSpecial = random.nextBoolean();
                        String[] messagesToUse = useSpecial ? newcodeMessages : killMessages;

                        java.util.List<Integer> availableIndices = new java.util.ArrayList<>();
                        for (int i = 0; i < messagesToUse.length; i++) {
                            if (!soft.get() && messagesToUse[i].contains("Nuc!ear") && !messagesToUse[i].contains("by Nuc!ear")) {
                                continue;
                            }
                            availableIndices.add(i);
                        }

                        if (!availableIndices.isEmpty()) {
                            int messageIndex = availableIndices.get(random.nextInt(availableIndices.size()));
                            randomMessage = messagesToUse[messageIndex];
                            if (!soft.get() && randomMessage.endsWith(" by Nuc!ear")) {
                                randomMessage = randomMessage.substring(0, randomMessage.length() - " by Nuc!ear".length());
                            }
                        } else {
                            return false;
                        }
                    } else if (isMonoton) {
                        modifiedName = replaceRandomLetter(targetName);;

                        boolean useSpecial = random.nextBoolean();
                        String[] messagesToUse = useSpecial ? monotonMessages : killMessages;

                        java.util.List<Integer> availableIndices = new java.util.ArrayList<>();
                        for (int i = 0; i < messagesToUse.length; i++) {
                            if (!soft.get() && messagesToUse[i].contains("Nuc!ear") && !messagesToUse[i].contains("by Nuc!ear")) {
                                continue;
                            }
                            availableIndices.add(i);
                        }

                        if (!availableIndices.isEmpty()) {
                            int messageIndex = availableIndices.get(random.nextInt(availableIndices.size()));
                            randomMessage = messagesToUse[messageIndex];
                            if (!soft.get() && randomMessage.endsWith(" by Nuc!ear")) {
                                randomMessage = randomMessage.substring(0, randomMessage.length() - " by Nuc!ear".length());
                            }
                        } else {
                            return false;
                        }
                    } else {
                        modifiedName = replaceRandomLetter(targetName);

                        java.util.List<Integer> availableIndices = new java.util.ArrayList<>();
                        for (int i = 0; i < killMessages.length; i++) {
                            if (!soft.get() && killMessages[i].contains("Nuc!ear") && !killMessages[i].contains("by Nuc!ear")) {
                                continue;
                            }
                            availableIndices.add(i);
                        }

                        if (!availableIndices.isEmpty()) {
                            int messageIndex;
                            do {
                                messageIndex = availableIndices.get(random.nextInt(availableIndices.size()));
                            } while (messageIndex == lastMessageIndex && availableIndices.size() > 1);

                            lastMessageIndex = messageIndex;
                            randomMessage = killMessages[messageIndex];
                            if (!soft.get() && randomMessage.endsWith(" by Nuc!ear")) {
                                randomMessage = randomMessage.substring(0, randomMessage.length() - " by Nuc!ear".length());
                            }
                        } else {
                            return false;
                        }
                    }

                    String message = "!-" + modifiedName + randomMessage;

                    if (currentTime - lastMessageTime >= 3000) {
                        mc.player.sendChatMessage(message);
                        lastMessageTime = currentTime;
                        messageSent = true;
                    } else {
                        pendingKillMessage = message;
                        pendingKillMessageTime = currentTime;
                        messageSent = true;
                    }
                }

                lastTargetHealth = currentHealth;
            } else {
                lastTargetHealth = -1.0f;
                messageSent = false;
            }
        }

        return false;
    }
}