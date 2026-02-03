package nuclear.utils.math;

import static nuclear.utils.IMinecraft.mc;

public class SensUtil {

    /**
     * ��������� ���������������� ��� ��������� ���� ��������.
     *
     * @param rot ���� ��������.
     * @return ����������������.
     */
    public static float getSensitivity(float rot) {
        float gcdValue = getGCDValue();
        return getDeltaMouse(rot, gcdValue) * gcdValue;
    }

    /**
     * ���������� �������� GCD (Greatest Common Divisor) � ������ ���������������� ����.
     *
     * @return �������� GCD.
     */
    public static float getGCDValue() {
        return (float) (getGCD() * 0.15);
    }

    /**
     * ��������� GCD �� ������ ���������������� ����.
     *
     * @return �������� GCD.
     */
    public static float getGCD() {
        float sensitivityFactor = (float) (mc.gameSettings.mouseSensitivity * 0.6 + 0.2);
        return sensitivityFactor * sensitivityFactor * sensitivityFactor * 8;
    }

    /**
     * ��������� ��������� ���� �������� ����.
     *
     * @param delta     ��������� ����.
     * @param gcdValue  �������� GCD.
     * @return ��������� ���� �������� ����.
     */
    public static float getDeltaMouse(float delta, float gcdValue) {
        if (gcdValue == 0) {
            return 0;
        }
        return Math.round(delta / gcdValue);
    }

    /**
     * ���������� ��������� ���������� ��� ����������������.
     * ��� �������� ������� ��������� ���� ����� �������������.
     *
     * @return ��������� ����������.
     */
    public static float getRandomSensitivityOffset() {
        return (float) (Math.random() * 0.1 - 0.05);
    }

    /**
     * ���������� ���������� �������� ���������������� � ������ ���������� ����������.
     *
     * @param rot ���� ��������.
     * @return ���������� �������� ����������������.
     */
    public static float getImprovedSensitivity(float rot) {
        float sensitivity = getSensitivity(rot);
        return sensitivity + getRandomSensitivityOffset();
    }
}