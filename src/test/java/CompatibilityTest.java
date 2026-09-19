package local.barrelfix;

import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import java.lang.reflect.Proxy;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModMetadata;

public final class CompatibilityTest {
    static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static byte[] read(Path jar, String entry) throws Exception {
        try (var zip = new ZipFile(jar.toFile())) { return zip.getInputStream(zip.getEntry(entry)).readAllBytes(); }
    }
    static ModContainer mod(String version, Path root) throws Exception {
        var parsed = Version.parse(version);
        var metadata = (ModMetadata) Proxy.newProxyInstance(CompatibilityTest.class.getClassLoader(), new Class<?>[]{ModMetadata.class},
            (proxy, method, args) -> method.getName().equals("getVersion") ? parsed : null);
        return (ModContainer) Proxy.newProxyInstance(CompatibilityTest.class.getClassLoader(), new Class<?>[]{ModContainer.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getMetadata" -> metadata;
                case "findPath" -> Optional.of(root.resolve((String) args[0])).filter(Files::exists);
                default -> null;
            });
    }
    public static void main(String[] args) throws Exception {
        Path fixtures = Path.of(args[0]);
        String latestStorage = "1.20.1-1.3.5.11.142", latestCore = "1.20.1-1.2.7.15.166";
        byte[] core = read(fixtures.resolve("sophisticatedcore-" + latestCore + ".jar"), Compatibility.CORE_CLASS);
        byte[] storage = read(fixtures.resolve("sophisticatedstorage-" + latestStorage + ".jar"), Compatibility.STORAGE_CLASS);
        int accepted=0, rejected=0, cores=0;
        for (String line : Files.readAllLines(fixtures.resolve("storage-fixtures.tsv"))) {
            String[] parts=line.split("\t");
            boolean expected=Boolean.parseBoolean(parts[2]);
            String reason=Compatibility.check(parts[0], latestCore, read(fixtures.resolve(parts[1]), Compatibility.STORAGE_CLASS), core);
            require((reason==null)==expected, "Storage decision wrong: " + parts[0]);
            if(expected) accepted++; else rejected++;
        }
        for (String line : Files.readAllLines(fixtures.resolve("core-fixtures.tsv"))) {
            String[] parts=line.split("\t");
            require(Compatibility.check(latestStorage, parts[0], storage, read(fixtures.resolve(parts[1]), Compatibility.CORE_CLASS))==null, "Core rejected: " + parts[0]);
            cores++;
        }
        require(Compatibility.check("1.20.1-1.3.5.12.143",latestCore,storage,core)!=null,"Future Storage accepted");
        require(Compatibility.check(latestStorage,"1.20.1-1.2.7.16.167",storage,core)!=null,"Future Core accepted");
        require(Compatibility.check(latestStorage,latestCore,null,core)!=null,"Missing class accepted");
        require(Compatibility.check(latestStorage,latestCore,new byte[]{1},core)!=null,"Changed Storage accepted");
        require(Compatibility.check(latestStorage,latestCore,storage,new byte[]{1})!=null,"Changed Core accepted");
        try (var storageFs=FileSystems.newFileSystem(fixtures.resolve("sophisticatedstorage-"+latestStorage+".jar"));
             var coreFs=FileSystems.newFileSystem(fixtures.resolve("sophisticatedcore-"+latestCore+".jar"))) {
            ModContainer sm=mod(latestStorage,storageFs.getPath("/")), cm=mod(latestCore,coreFs.getPath("/"));
            var plugin=new CompatibilityPlugin(id -> Optional.of(id.equals("sophisticatedstorage") ? sm : cm));
            require(!plugin.shouldApplyMixin("target","mixin"),"Enabled before check");
            plugin.onLoad("local.barrelfix");
            require(plugin.shouldApplyMixin("target","mixin"),"Supported plugin disabled");
            ModContainer unsupported=mod("1.20.1-1.3.5.12.143",storageFs.getPath("/"));
            plugin=new CompatibilityPlugin(id -> Optional.of(id.equals("sophisticatedstorage") ? unsupported : cm));
            plugin.onLoad("local.barrelfix");
            require(!plugin.shouldApplyMixin("target","mixin"),"Unsupported plugin enabled");
        }
        var missing=new CompatibilityPlugin(id -> Optional.empty());
        missing.onLoad("local.barrelfix");
        require(!missing.shouldApplyMixin("target","mixin"),"Missing dependency enabled");
        var failed=new CompatibilityPlugin(id -> {throw new IllegalStateException("simulated read failure");});
        failed.onLoad("local.barrelfix");
        require(!failed.shouldApplyMixin("target","mixin"),"Failed check enabled");
        System.out.println("PASS: " + accepted + " Storage accepted, " + rejected + " rejected, " + cores + " Core accepted; altered/missing classes, future versions, and plugin enable/skip/error paths.");
    }
}
