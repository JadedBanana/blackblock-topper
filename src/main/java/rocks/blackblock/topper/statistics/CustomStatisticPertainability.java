package rocks.blackblock.topper.statistics;

public enum CustomStatisticPertainability {

    ALL(0),
    MAINTAINS(1),
    OWNS(2);

    private int value;

    private CustomStatisticPertainability(int value) {
        this.value = value;
    }
}
