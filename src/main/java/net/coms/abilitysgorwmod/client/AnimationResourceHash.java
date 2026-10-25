package net.coms.abilitysgorwmod.client;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
public final class AnimationResourceHash {
    private AnimationResourceHash() {}
    public static String sha256(String resourcePath){
        try{
            ResourceLocation id=ResourceLocation.tryParse(resourcePath);if(id==null)return null;
            byte[] bytes;
            if(id.getNamespace().equals("abilitysgorwmod")&&id.getPath().startsWith("animations/custom/")){
                Path root=FMLPaths.CONFIGDIR.get().resolve("abilitysgorwmod").normalize();Path file=root.resolve(id.getPath()).normalize();
                if(!file.startsWith(root)||!Files.isRegularFile(file)||Files.size(file)>2_000_000L)return null;bytes=Files.readAllBytes(file);
            }else{
                Resource resource=Minecraft.getInstance().getResourceManager().getResource(id).orElse(null);if(resource==null)return null;
                try(InputStream in=resource.open()){bytes=in.readAllBytes();}
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        }catch(Exception ex){return null;}
    }
    public static String findCustomResourceByHash(String expectedHash) {
        if (expectedHash == null || !expectedHash.matches("[0-9a-f]{64}")) {
            return null;
        }
        Path directory = FMLPaths.CONFIGDIR.get()
                .resolve("abilitysgorwmod/animations/custom").normalize();
        if (!Files.isDirectory(directory)) {
            return null;
        }
        try (var files = Files.newDirectoryStream(directory, "*.json")) {
            for (Path file : files) {
                try {
                    if (!Files.isRegularFile(file) || Files.size(file) > 2_000_000L) {
                        continue;
                    }
                    byte[] contents = Files.readAllBytes(file);
                    String hash = HexFormat.of().formatHex(
                            MessageDigest.getInstance("SHA-256").digest(contents)
                    );
                    if (expectedHash.equals(hash)) {
                        return "abilitysgorwmod:animations/custom/"
                                + file.getFileName();
                    }
                } catch (Exception ignored) {
                    // Skip unreadable custom files and keep checking the directory.
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }}