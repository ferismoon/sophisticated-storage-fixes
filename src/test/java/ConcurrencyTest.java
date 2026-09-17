import java.lang.reflect.Method;
import java.util.concurrent.*;
import local.barrelfix.BarrelModelDataMixin;
import net.p3pp3rf1y.sophisticatedcore.util.model.ModelData;

public class ConcurrencyTest {
    public static void main(String[] args) throws Exception {
        var model = new BarrelModelDataMixin();
        Method write = model.getClass().getDeclaredMethod("barrelfix$write", Object.class, ModelData.class);
        Method read = model.getClass().getDeclaredMethod("barrelfix$read", Object.class);
        write.setAccessible(true);
        read.setAccessible(true);
        var dirt = ModelData.builder().build();
        var stone = ModelData.builder().build();
        var sharedOriginal = new ModelData[]{null};
        var barrier = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> {
                try {
                    for (int i = 0; i < 10000; i++) {
                        sharedOriginal[0] = dirt;
                        write.invoke(model, model, dirt);
                        barrier.await(10, TimeUnit.SECONDS);
                        barrier.await(10, TimeUnit.SECONDS);
                        if (sharedOriginal[0] != stone) throw new AssertionError("Original race not reproduced");
                        if (read.invoke(model, model) != dirt) throw new AssertionError("Chunk A contaminated");
                        barrier.await(10, TimeUnit.SECONDS);
                    }
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            Future<?> second = executor.submit(() -> {
                try {
                    for (int i = 0; i < 10000; i++) {
                        barrier.await(10, TimeUnit.SECONDS);
                        sharedOriginal[0] = stone;
                        write.invoke(model, model, stone);
                        barrier.await(10, TimeUnit.SECONDS);
                        if (read.invoke(model, model) != stone) throw new AssertionError("Chunk B contaminated");
                        write.invoke(model, model, null);
                        if (read.invoke(model, model) != null) throw new AssertionError("Item rendering did not clear state");
                        barrier.await(10, TimeUnit.SECONDS);
                    }
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            first.get(60, TimeUnit.SECONDS);
            second.get(60, TimeUnit.SECONDS);
            if (read.invoke(model, model) != null) throw new AssertionError("Main thread contaminated");
            System.out.println("PASS: 10,000 forced chunk interleavings; original shared field fails, patched handlers isolate data and item reset.");
        } finally { executor.shutdownNow(); }
    }
}
