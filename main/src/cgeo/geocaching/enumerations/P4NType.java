package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public enum P4NType {

    ACC_G("ACC_G", "Aire gratuite", "", R.string.airegratuite, R.drawable.pins_acc_g, "ACC_G", R.drawable.dot_traditional),
    ACC_P( "ACC_P","Aire payante","",R.string.airepayante,R.drawable.pins_acc_p,"ACC_P",R.drawable.dot_event),
    ACC_PR("ACC_PR", "Aire privée", "", R.string.aireprivee, R.drawable.pins_acc_pr, "ACC_PR", R.drawable.dot_virtual),
    EP("EP", "Particulier", "", R.string.accueilparticulier, R.drawable.pins_ep, "EP", R.drawable.dot_virtual),
    ASS("ASS", "Service", "", R.string.serviceonly, R.drawable.pins_ass, "ASS", R.drawable.dot_event),
    APN("APN", "Aire piquenique", "", R.string.airepiquenique, R.drawable.pins_apn, "APN", R.drawable.dot_traditional),
    AR("AR", "Aire repos", "", R.string.airerepos, R.drawable.pins_ar, "AR", R.drawable.dot_virtual),
    C("C", "Camping", "", R.string.camping, R.drawable.pins_c, "C", R.drawable.dot_event),
    F("F", "Ferme", "", R.string.ferme, R.drawable.pins_f, "F", R.drawable.dot_event),
    PN("PN", "Aire naturelle", "", R.string.airenaturelle, R.drawable.pins_pn, "PN", R.drawable.dot_traditional),
    P("P", "Parking", "", R.string.parkingbleu, R.drawable.pins_p, "P", R.drawable.dot_mystery),
    PJ("PJ", "Parking jour", "", R.string.parkingjour, R.drawable.pins_pj, "PJ", R.drawable.dot_multi),
    OR("OR", "Off road", "", R.string.offroad, R.drawable.pins_or, "OR", R.drawable.dot_multi),
    DS("DS", "multi service", "", R.string.multiservice, R.drawable.pins_ds, "DS", R.drawable.dot_event),

    Test("","","",0,0,"",0);

    public final String id;
    /**
     * human readable name of the park type<br>
     * used in web parsing as well as for gpx import/export.
     */
    public final String pattern;
    public final String guid;
    private final int stringId;
    public final int markerId;
    @NonNull public final String wptTypeId;
    public final int dotMarkerId;

    P4NType(final String id, final String pattern, final String guid, final int stringId, final int markerId, @NonNull final String wptTypeId, final int dotMarkerId) {
        this.id = id;
        this.pattern = pattern;
        this.guid = guid;
        this.stringId = stringId;
        this.markerId = markerId;
        this.wptTypeId = wptTypeId;
        this.dotMarkerId = dotMarkerId;
    }
}
