package net.coms.abilitysgorwmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Desktop;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DancingAnimationScreen extends Screen {
    private static final Logger LOGGER=LoggerFactory.getLogger("abilitysgorwmod/DancingScreen");
    private final Screen parent;
    private EditBox source,displayName,clip,settings;
    private volatile String message="JSONを選び、登録名とクリップ名を確認して登録してください。";
    public DancingAnimationScreen(Screen parent){super(Component.literal("ダンシング設定"));this.parent=parent;}
    @Override protected void init(){
        int x=width/2-145,y=20;
        source=box(x,y,220,"JSONファイル");
        addRenderableWidget(Button.builder(Component.literal("ファイルを開く"),b->chooseAnimation()).bounds(x+225,y,65,20).build());
        displayName=box(x,y+28,290,"登録名（ファイル名）");
        clip=box(x,y+56,290,"クリップ名（animations内の名前）");
        settings=box(x,y+84,290,"設定JSONのパス");settings.setValue(FMLPaths.CONFIGDIR.get().resolve("abilitysgorwmod/dancing_animations.json").toString());
        addRenderableWidget(Button.builder(Component.literal("登録"),b->registerAnimation()).bounds(x,y+112,92,20).build());
        addRenderableWidget(Button.builder(Component.literal("フォルダを開く"),b->openImportFolder()).bounds(x+98,y+112,96,20).build());
        addRenderableWidget(Button.builder(Component.literal("再生リスト"),b->minecraft.setScreen(new DancingAnimationSelectScreen())).bounds(x+200,y+112,90,20).build());
        addRenderableWidget(Button.builder(Component.literal("設定を出力"),b->message=DancingAnimationConfig.exportSettings(settings.getValue())).bounds(x,y+138,140,20).build());
        addRenderableWidget(Button.builder(Component.literal("設定を読み込み"),b->message=DancingAnimationConfig.importSettings(settings.getValue())).bounds(x+150,y+138,140,20).build());
        addRenderableWidget(Button.builder(Component.literal("戻る"),b->minecraft.setScreen(parent)).bounds(width/2-45,height-28,90,20).build());
        try{DancingAnimationConfig.importDirectory();}catch(Exception ex){message="取り込みフォルダを作成できません: "+ex.getMessage();}
    }
    private EditBox box(int x,int y,int w,String hint){EditBox e=new EditBox(font,x,y,w,20,Component.literal(hint));e.setHint(Component.literal(hint));e.setMaxLength(1024);addRenderableWidget(e);return e;}
    private void chooseAnimation() {
        try {
            Path directory = DancingAnimationConfig.importDirectory();
            SwingUtilities.invokeLater(() -> {
                JFileChooser chooser = new JFileChooser(directory.toFile());
                chooser.setDialogTitle("アニメーションJSONを選択");
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setFileFilter(new FileNameExtensionFilter("アニメーションJSON (*.json)", "json"));
                if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                Path file = chooser.getSelectedFile().toPath().toAbsolutePath();
                String selected = file.toString();
                String base = file.getFileName().toString().replaceFirst("(?i)\\.json$", "");
                List<String> clips = List.of();
                String error = null;
                try {
                    clips = DancingAnimationConfig.animationNames(selected);
                } catch (Exception ex) {
                    error = ex.getMessage();
                }
                List<String> foundClips = clips;
                String readError = error;
                Minecraft.getInstance().execute(() -> {
                    if (minecraft.screen != this) {
                        return;
                    }
                    source.setValue(selected);
                    displayName.setValue(base);
                    if (foundClips.isEmpty()) {
                        clip.setValue("");
                        message = "JSONを選択しましたが、animations内のクリップを読めません: " + readError;
                        return;
                    }
                    clip.setValue(foundClips.get(0));
                    message = "クリップ候補: " + String.join("、", foundClips)
                            + "（先頭の「" + foundClips.get(0) + "」を入力しました）";
                });
            });
        } catch (Exception ex) {
            message = "ファイル選択画面を開けません: " + ex.getMessage();
        }
    }

    private void registerAnimation() {
        try {
            List<String> clips = DancingAnimationConfig.animationNames(source.getValue());
            String selectedClip = clip.getValue().trim();
            if (selectedClip.isEmpty()) {
                selectedClip = clips.get(0);
                clip.setValue(selectedClip);
            }
            String result = DancingAnimationConfig.importAnimation(
                    source.getValue(), displayName.getValue(), selectedClip
            );
            message = clips.size() > 1 && result.startsWith("登録しました:")
                    ? result + " / 他のクリップ: " + String.join("、", clips)
                    : result;
        } catch (Exception ex) {
            message = "クリップ名を読み取れません: " + ex.getMessage();
        }
    }
    private void openImportFolder(){
        try{Path directory=DancingAnimationConfig.importDirectory().toAbsolutePath().normalize();String os=System.getProperty("os.name","").toLowerCase(java.util.Locale.ROOT);if(os.contains("win"))new ProcessBuilder("explorer.exe",directory.toString()).start();else if(Desktop.isDesktopSupported())Desktop.getDesktop().open(directory.toFile());else{message="ダンスフォルダ: "+directory;return;}message="ダンスフォルダを開きました: "+directory;}
        catch(Exception ex){LOGGER.error("ダンス取り込みフォルダを開けません",ex);message="フォルダを開けません: "+ex.getClass().getSimpleName()+" ("+FMLPaths.CONFIGDIR.get().resolve("abilitysgorwmod/animations/import")+")";}
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);g.drawCenteredString(font,title,width/2,7,0xFFFFFF);
        g.drawString(font,Component.literal("取り込みフォルダ: config/abilitysgorwmod/animations/import"),width/2-145,166,0xBBBBBB);
        g.drawString(font,Component.literal(message),width/2-145,180,0xFFFFFF);
        g.drawString(font,Component.literal("登録後はNキーの再生リストから選択できます。"),width/2-145,195,0xDDDDDD);
        super.render(g,mx,my,partial);
    }    @Override public boolean isPauseScreen(){return false;}
}