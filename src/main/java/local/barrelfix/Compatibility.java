package local.barrelfix;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

/** Only enable the patch for published builds whose target classes were inspected. */
public final class Compatibility {
    public static final String STORAGE_CLASS = "net/p3pp3rf1y/sophisticatedstorage/client/render/BarrelBakedModelBase.class";
    public static final String CORE_CLASS = "net/p3pp3rf1y/sophisticatedcore/util/model/ModelData.class";
    private static final Set<String> STORAGE = Set.of(
        "1.20.1-1.0.10.1.100", "1.20.1-1.3.5.1.114", "1.20.1-1.3.5.4.126",
        "1.20.1-1.3.5.5.128", "1.20.1-1.3.5.6.130", "1.20.1-1.3.5.7.132",
        "1.20.1-1.3.5.8.134", "1.20.1-1.3.5.9.136", "1.20.1-1.3.5.10.141", "1.20.1-1.3.5.11.142");
    private static final Set<String> CORE = Set.of(
        "1.20.1-1.0.8.1.119", "1.20.1-1.2.7.2.139", "1.20.1-1.2.7.7.156",
        "1.20.1-1.2.7.9.158", "1.20.1-1.2.7.10.159", "1.20.1-1.2.7.12.160",
        "1.20.1-1.2.7.13.162", "1.20.1-1.2.7.14.163", "1.20.1-1.2.7.15.166");
    private static final Set<String> STORAGE_HASHES = Set.of(
        "3cf55ccb06526937b85e41adf435b2cafd525e0ff0f862409a52a2a1636ec6bb",
        "ff729b08cdbf588a09ab548517fdd33fa2ea53599352223c488a8db09ce79856");
    private static final String CORE_HASH = "27f5ed528b6b99dbfad9a14bf2cb189d7899aeec62019d91c4bf2104f6fd0b81";

    public static String check(String storage, String core, byte[] storageClass, byte[] coreClass) {
        if (!STORAGE.contains(storage)) return "unsupported Sophisticated Storage version: " + storage;
        if (!CORE.contains(core)) return "unsupported Sophisticated Core version: " + core;
        if (storageClass == null || coreClass == null) return "required rendering class is missing";
        try {
            var sha = MessageDigest.getInstance("SHA-256");
            if (!STORAGE_HASHES.contains(HexFormat.of().formatHex(sha.digest(storageClass))))
                return "Sophisticated Storage rendering code differs from the checked releases";
            if (!CORE_HASH.equals(HexFormat.of().formatHex(sha.digest(coreClass))))
                return "Sophisticated Core model data differs from the checked releases";
            return null;
        } catch (java.security.NoSuchAlgorithmException e) {
            return "could not verify rendering code: " + e.getMessage();
        }
    }
    private Compatibility() {}
}
