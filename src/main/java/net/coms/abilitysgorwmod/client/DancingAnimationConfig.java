package net.coms.abilitysgorwmod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.coms.abilitysgorwmod.network.AbilityNetwork;
import net.coms.abilitysgorwmod.network.AnimationPlayRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DancingAnimationConfig {
    private static final Logger LOG=LoggerFactory.getLogger("abilitysgorwmod/Dancing");
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE=new TypeToken<List<Entry>>(){}.getType();
    private static final List<Entry> ENTRIES=new ArrayList<>();
    private static final Set<String> HELD=new HashSet<>();
    private static boolean loaded;
    private DancingAnimationConfig() {}
    public static final class Entry {
        public String id,displayName,resource,clip;
        /** Optional legacy direct key; 0 means use the N-key selection list. */
        public int keyCode;
        public Entry() {}
        Entry(String id,String display,String resource,String clip,int key){this.id=id;this.displayName=display;this.resource=resource;this.clip=clip;this.keyCode=key;}
    }
    private static Path configFile(){return FMLPaths.CONFIGDIR.get().resolve("abilitysgorwmod/dancing_animations.json");}
    public static Path importDirectory() throws java.io.IOException {Path path=FMLPaths.CONFIGDIR.get().resolve("abilitysgorwmod/animations/import");Files.createDirectories(path);return path;}
    private static Path resolveAnimationFile(String source) throws java.io.IOException {
        Path supplied = Path.of(source.trim());
        Path direct = supplied.toAbsolutePath().normalize();
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        if (!supplied.isAbsolute()) {
            Path importRoot = importDirectory().toAbsolutePath().normalize();
            Path imported = importRoot.resolve(supplied).normalize();
            if (imported.startsWith(importRoot) && Files.isRegularFile(imported)) {
                return imported;
            }
        }
        return direct;
    }
    public static List<String> animationNames(String source) throws java.io.IOException {
        if (source == null || source.isBlank()) {
            throw new java.io.IOException("JSONファイルを選択してください。");
        }
        Path path = resolveAnimationFile(source);
        if (!Files.isRegularFile(path) || Files.size(path) > 2_000_000L) {
            throw new java.io.IOException("JSONファイルが見つからないか、2MBを超えています。");
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject animations = root.getAsJsonObject("animations");
            if (animations == null || animations.size() == 0) {
                throw new java.io.IOException("JSONにanimationsがありません。");
            }
            return List.copyOf(animations.keySet());
        } catch (com.google.gson.JsonParseException | IllegalStateException ex) {
            throw new java.io.IOException("JSONの形式を読み取れません。", ex);
        }
    }
    private static Path customDirectory(){return FMLPaths.CONFIGDIR.get().resolve("abilitysgorwmod/animations/custom");}
    private static void load(){
        if(loaded)return;loaded=true;Path path=configFile();if(!Files.isRegularFile(path))return;
        try(Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)){
            List<Entry> parsed=GSON.fromJson(reader,LIST_TYPE);if(parsed!=null)for(Entry e:parsed)if(validEntry(e)&&!duplicate(e))ENTRIES.add(e);
        }catch(Exception ex){LOG.error("ダンシング設定を読み込めません: {}",path,ex);}
    }
    public static List<Entry> entries(){load();return List.copyOf(ENTRIES);}
    private static boolean validEntry(Entry e){return e!=null&&e.id!=null&&e.id.matches("[a-z0-9_-]{1,32}")&&e.displayName!=null&&!e.displayName.isBlank()&&e.resource!=null&&e.resource.matches("abilitysgorwmod:animations/custom/[a-z0-9_-]{1,32}\\.json")&&e.clip!=null&&e.clip.matches("[A-Za-z0-9_.-]{1,64}")&&e.keyCode>=0&&e.keyCode<=348;}
    private static boolean duplicate(Entry e){for(Entry old:ENTRIES)if(old.id.equals(e.id)||(e.keyCode>0&&old.keyCode==e.keyCode))return true;return false;}
    private static String slug(String name){String value=name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+","_").replaceAll("^[_-]+|[_-]+$","");if(value.isBlank())value="dance";return value.length()>25?value.substring(0,25):value;}
    public static String importAnimation(String source,String displayName,String clip){
        load();if(source==null||source.isBlank())return "先にアニメーションJSONファイルを選んでください。";
        String normalizedDisplayName = displayName == null
                ? "" : displayName.trim().replaceFirst("(?i)\\.json$", "");
        if(normalizedDisplayName.isBlank()||normalizedDisplayName.length()>40)return "ファイル名（登録名）を1～40文字で入力してください。";
        if(clip==null||!clip.trim().matches("[A-Za-z0-9_.-]{1,64}"))return "JSON内のクリップ名を入力してください（英数字、_、-、.）。";
        try{
            Path input=resolveAnimationFile(source);
            if(!Files.isRegularFile(input)||!input.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")||Files.size(input)>2_000_000L)return "選択したJSONが見つからないか、2MBを超えています。";
            String clipName=clip.trim();
            try(Reader reader=Files.newBufferedReader(input,StandardCharsets.UTF_8)){
                JsonObject root=JsonParser.parseReader(reader).getAsJsonObject();JsonObject animations=root.getAsJsonObject("animations");
                if(animations==null||!animations.has(clipName)||!animations.get(clipName).isJsonObject())return "JSON内にそのクリップ名がありません。animations内の名前を確認してください。";
            }
            String stem=slug(normalizedDisplayName),id=stem;int suffix=2;
            while(hasId(id)){String tail="_"+suffix++;id=stem.substring(0,Math.min(stem.length(),32-tail.length()))+tail;}
            Entry entry=new Entry(id,normalizedDisplayName,"abilitysgorwmod:animations/custom/"+id+".json",clipName,0);
            Path target=customDirectory().resolve(id+".json");Files.createDirectories(target.getParent());Files.copy(input,target,StandardCopyOption.REPLACE_EXISTING);
            ENTRIES.add(entry);try{save();}catch(Exception ex){ENTRIES.remove(entry);throw ex;}
            return "登録しました: "+entry.displayName+" / "+entry.clip;
        }catch(Exception ex){LOG.error("アニメーションを取り込めません: {}",source,ex);return "登録に失敗しました。JSONとログを確認してください。";}
    }
    private static boolean hasId(String id){for(Entry e:ENTRIES)if(e.id.equals(id))return true;return false;}
    private static void save() throws java.io.IOException {Files.createDirectories(configFile().getParent());Files.writeString(configFile(),GSON.toJson(ENTRIES),StandardCharsets.UTF_8);}
    public static String exportSettings(String target){load();try{Path p=Path.of(target).toAbsolutePath().normalize();if(p.getParent()!=null)Files.createDirectories(p.getParent());Files.writeString(p,GSON.toJson(ENTRIES),StandardCharsets.UTF_8);return "設定を出力しました: "+p;}catch(Exception ex){LOG.error("設定を出力できません: {}",target,ex);return "設定を出力できません。パスを確認してください。";}}
    public static String importSettings(String source){load();try(Reader reader=Files.newBufferedReader(Path.of(source).toAbsolutePath().normalize(),StandardCharsets.UTF_8)){
        List<Entry> parsed=GSON.fromJson(reader,LIST_TYPE);if(parsed==null)return "設定JSONが空です。";
        List<Entry> old=new ArrayList<>(ENTRIES);ENTRIES.clear();for(Entry e:parsed){if(!validEntry(e)||duplicate(e)){ENTRIES.clear();ENTRIES.addAll(old);return "設定に不正な項目、ID重複、キー重複があります。";}ENTRIES.add(e);}
        save();return "設定を読み込みました。アニメーション本体は各クライアントに別途必要です。";
    }catch(Exception ex){LOG.error("設定を読み込めません: {}",source,ex);return "設定を読み込めません。ファイルとログを確認してください。";}}
    public static boolean play(Entry entry){Minecraft mc=Minecraft.getInstance();if(mc.player==null||!canDance(mc.player))return false;String hash=AnimationResourceHash.sha256(entry.resource);if(hash==null)return false;AbilityNetwork.sendToServer(new AnimationPlayRequestPacket(mc.player.getUUID(),entry.resource,entry.clip,hash,false));return true;}
    public static boolean canDance(net.minecraft.world.entity.player.Player player){return player!=null&&((PlayerAbilityData.hasAbility(player,"dancing")&&PlayerAbilityData.isActive(player,"dancing"))||(PlayerAbilityData.hasAbility(player,"example:dancing")&&PlayerAbilityData.isActive(player,"example:dancing")));}
    public static void tickKeys(){
        load();Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.screen!=null){HELD.clear();return;}
        if(!canDance(mc.player)){HELD.clear();return;}long window=mc.getWindow().getWindow();Set<String> down=new HashSet<>();
        for(Entry e:ENTRIES){if(e.keyCode<=0)continue;boolean pressed=com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,e.keyCode);if(pressed)down.add(e.id);if(pressed&&!HELD.contains(e.id)){String hash=AnimationResourceHash.sha256(e.resource);if(hash!=null)AbilityNetwork.sendToServer(new AnimationPlayRequestPacket(mc.player.getUUID(),e.resource,e.clip,hash,false));}}
        HELD.clear();HELD.addAll(down);
    }
}