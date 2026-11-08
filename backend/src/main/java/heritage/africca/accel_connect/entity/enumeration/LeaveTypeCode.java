package heritage.africca.accel_connect.entity.enumeration;

import java.util.Optional;


public enum LeaveTypeCode {

    ANNUAL_LEAVE("ANNUAL_LEAVE", "Congé annuel", DeductionRule.FULL, 0),

    BIRTH_BAPTISM("BIRTH_BAPTISM",     "Naissance / Baptême",                   DeductionRule.EXCESS_ONLY, 1),
    MARRIAGE_SELF("MARRIAGE_SELF",     "Mariage Collaborateur",                DeductionRule.EXCESS_ONLY, 3),

    MARRIAGE_KIN("MARRIAGE_KIN",       "Mariage enfant / frère / sœur",         DeductionRule.EXCESS_ONLY, 1),
    DEATH_CLOSE("DEATH_CLOSE",         "Décès conjoint / descendant direct",    DeductionRule.EXCESS_ONLY, 4),
    DEATH_EXTENDED("DEATH_EXTENDED",   "Décès ascendant / frère / beau-parent", DeductionRule.EXCESS_ONLY, 2),
    HOSPITALIZATION("HOSPITALIZATION", "Hospitalisation conjoint / enfant",     DeductionRule.EXCESS_ONLY, 2),
    SICK_LEAVE("SICK_LEAVE", "Arret Maladie", DeductionRule.NONE, 0),
    RECUPERATION("RECUPERATION", "Récupération", DeductionRule.NONE, 0);


    public enum DeductionRule {

        FULL,
        EXCESS_ONLY,
        NONE
    }



    private final String code;
    private final String displayName;
    private final DeductionRule deductionRule;
    /** Days covered by law/policy — only meaningful for {@link DeductionRule#EXCESS_ONLY}. */
    private final int allocatedDays;

    LeaveTypeCode(String code, String displayName, DeductionRule deductionRule, int allocatedDays) {
        this.code = code;
        this.displayName = displayName;
        this.deductionRule = deductionRule;
        this.allocatedDays = allocatedDays;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public DeductionRule getDeductionRule() {
        return deductionRule;
    }

    public int getAllocatedDays() {
        return allocatedDays;
    }


    public static Optional<LeaveTypeCode> fromCode(String code) {
        if (code == null) return Optional.empty();
        for (LeaveTypeCode ltc : values()) {
            if (ltc.code.equals(code)) return Optional.of(ltc);
        }
        return Optional.empty();
    }
}
