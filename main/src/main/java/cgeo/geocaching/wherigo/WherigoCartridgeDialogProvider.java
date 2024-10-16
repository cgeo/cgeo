package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoCartridgeDetailsBinding;
import cgeo.geocaching.databinding.WherigolistItemBinding;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.LocalizationUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;

import java.util.Arrays;
import java.util.List;

import cz.matejcik.openwig.formats.CartridgeFile;

public class WherigoCartridgeDialogProvider implements IWherigoDialogProvider {

    private final WherigoCartridgeInfo cartridgeInfo;
    private final CartridgeFile cartridgeFile;
    private WherigoCartridgeDetailsBinding binding;

    private enum CartridgeAction {
        PLAY(TextParam.id(R.string.play).setImage(ImageParam.id(R.drawable.ic_menu_select_play))),
        DELETE(TextParam.id(R.string.delete).setImage(ImageParam.id(R.drawable.ic_menu_delete))),
        CLOSE(TextParam.id(R.string.close).setImage(ImageParam.id(R.drawable.wherigo_close)));

        private final TextParam tp;

        CartridgeAction(final TextParam tp) {
            this.tp = tp;
        }

        public TextParam getTextParam() {
            return tp;
        }
    }

    public WherigoCartridgeDialogProvider(final WherigoCartridgeInfo cartridgeInfo) {
        this.cartridgeInfo = cartridgeInfo;
        this.cartridgeFile = cartridgeInfo.getCartridgeFile();
    }

    @Override
    public Dialog createDialog(final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.cgeo_fullScreen);
        binding = WherigoCartridgeDetailsBinding.inflate(LayoutInflater.from(activity));
        final AlertDialog dialog = builder.create();
        dialog.setTitle(cartridgeFile.name);
        dialog.setView(binding.getRoot());

        final List<WherigoSavegameInfo> saveGames = cartridgeInfo.getLoadableSavegames();

        binding.description.setText(WherigoGame.get().toDisplayText(cartridgeFile.description));
        //following info is debug -> no translation needed
        TextParam.text("- **CGUID:** " + cartridgeInfo.getCGuid() + "\n" +
            "- **Device:** " + cartridgeFile.device + "\n" +
            "- **Type:** " + cartridgeFile.type + "\n" +
            "- **Member:** " + cartridgeFile.member + "\n" +
            "- **Version:** " + cartridgeFile.version + "\n" +
            "- **Save Games** " + saveGames.size() + " (" + saveGames + ")\n" +
            "- **C:** " + cartridgeFile.code + "\n" +
            "- **Url:** " + cartridgeFile.url + "\n").setMarkdown(true).applyTo(binding.debugInfo);
            binding.debugInfo.setVisibility(WherigoGame.get().isDebugMode() ? View.VISIBLE : View.GONE);

        byte[] mediaData = cartridgeInfo.getSplashData();
        if (mediaData == null) {
            mediaData = cartridgeInfo.getIconData();
        }

        binding.media.setMediaData("jpg", mediaData, null);
        refreshGui();

        WherigoUtils.setViewActions(Arrays.asList(CartridgeAction.values()), binding.dialogActionlist, CartridgeAction::getTextParam, item -> {
            WherigoDialogManager.get().clear();
            if (item == CartridgeAction.CLOSE) {
                return;
            }
            //Other actions require ending a running game (if any)
            WherigoUtils.ensureNoGameRunning(activity, () -> performActionAfterGameEnded(item, activity, saveGames));
        });
        return dialog;
    }

    @Override
    public void onGameNotification(final WherigoGame.NotifyType notifyType) {
        refreshGui();
    }

    private void refreshGui() {
        TextParam.text("**" + LocalizationUtils.getString(R.string.wherigo_author) + ":** " + cartridgeFile.author + "  \n" +
            "**" + LocalizationUtils.getString(R.string.cache_location) + ":** " + cartridgeInfo.getCartridgeLocation() + "  \n" +
            "**" + LocalizationUtils.getString(R.string.distance) + ":** " + WherigoUtils.getDisplayableDistance(WherigoLocationProvider.get().getLocation(), cartridgeInfo.getCartridgeLocation())
            ).setMarkdown(true).applyTo(binding.headerInformation);
    }

    //Action is either "delete cartridge" or "Play Game"
    private void performActionAfterGameEnded(final CartridgeAction action, final Activity activity, final List<WherigoSavegameInfo> saveGames) {
        if (action == CartridgeAction.DELETE) {
            SimpleDialog.of(activity).setTitle(TextParam.id(R.string.wherigo_confirm_delete_cartridge_title))
                    .setMessage(TextParam.id(R.string.wherigo_confirm_delete_cartridge_message, WherigoGame.get().getCartridgeName(), "" + saveGames.size()))
                    .setButtons(SimpleDialog.ButtonTextSet.YES_NO)
                    .confirm(() -> {
                        ContentStorage.get().delete(cartridgeInfo.getFileInfo().uri);
                        for (WherigoSavegameInfo savegame : saveGames) {
                            ContentStorage.get().delete(savegame.fileInfo.uri);
                        }
                    });
        } else {
            playGame(activity);
        }
    }

    private void playGame(final Activity activity) {

        final List<WherigoSavegameInfo> loadGameList = this.cartridgeInfo.getLoadableSavegames();
        if (loadGameList.size() == 1 && loadGameList.get(0).name == null) {
            //no savegames present --> just start a new game
            WherigoGame.get().newGame(cartridgeInfo.getFileInfo());
            return;
        }

        final SimpleDialog.ItemSelectModel<WherigoSavegameInfo> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(loadGameList)
            .setDisplayViewMapper(R.layout.wherigolist_item, (si, group, view) -> {
                final WherigolistItemBinding itemBinding = WherigolistItemBinding.bind(view);
                itemBinding.name.setText(si.getUserDisplayableName());
                itemBinding.description.setText(si.getUserDisplayableSaveDate());
                itemBinding.icon.setImageResource(R.drawable.ic_menu_upload);
            }, (item, itemGroup) -> item == null || item.name == null ? "" : item.name)
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);

        SimpleDialog.of(activity)
                .setTitle(TextParam.id(R.string.wherigo_choose_new_loadgame))
                .selectSingle(model, s -> {
                    WherigoGame.get().loadGame(cartridgeInfo.getFileInfo(), s.name);
                });
    }

}
