package vn.sotay.thuchi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Picker and image work stay separate from screen rendering and never read an entire photo on the UI thread. */
final class AvatarController implements AutoCloseable {
    private static final int PICK_AVATAR = 4701;
    private final Activity activity;
    private final AvatarStore store;
    private final Runnable onChanged;
    private final Consumer<AlertDialog> styleDialog;
    // One queue also preserves write/read order when the Activity rotates during an import.
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ArrayList<WeakReference<AvatarView>> views = new ArrayList<>();
    private Bitmap avatar;
    private String name = "";
    private boolean closed;

    AvatarController(Activity activity, Runnable onChanged, Consumer<AlertDialog> styleDialog) {
        this.activity = activity;
        this.store = new AvatarStore(activity);
        this.onChanged = onChanged;
        this.styleDialog = styleDialog;
        WORKER.execute(() -> {
            try {
                Bitmap saved = store.load();
                main.post(() -> { if (!closed) { avatar = saved; updateViews(); } });
            } catch (Exception ignored) { /* Keep the initial; choosing another photo can repair a damaged file. */ }
        });
    }

    void bind(AvatarView view, String displayName) {
        name = displayName;
        view.setProfile(name, avatar);
        view.setOnClickListener(v -> showChooser());
        views.add(new WeakReference<>(view));
        updateViews();
    }

    void showChooser() {
        String[] options = avatar == null
                ? new String[]{activity.getString(R.string.avatar_choose)}
                : new String[]{activity.getString(R.string.avatar_choose), activity.getString(R.string.avatar_remove)};
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.avatar_title)
                .setItems(options, (d, which) -> { if (which == 0) pick(); else remove(); })
                .setNegativeButton(R.string.avatar_cancel, null).create();
        dialog.show();
        styleDialog.accept(dialog);
    }

    private void pick() {
        Intent picker = Build.VERSION.SDK_INT >= 33
                ? new Intent(MediaStore.ACTION_PICK_IMAGES).setType("image/*")
                : documentPicker();
        try {
            activity.startActivityForResult(picker, PICK_AVATAR);
        } catch (ActivityNotFoundException unavailable) {
            try { activity.startActivityForResult(documentPicker(), PICK_AVATAR); }
            catch (ActivityNotFoundException error) { toast(R.string.avatar_picker_unavailable); }
        }
    }

    private Intent documentPicker() {
        return new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*")
                .addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PICK_AVATAR) return false;
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null || closed) return true;
        Uri uri = data.getData();
        WORKER.execute(() -> {
            try {
                Bitmap replacement = store.replace(uri);
                main.post(() -> {
                    if (!closed) { avatar = replacement; updateViews(); onChanged.run(); }
                });
            } catch (Exception error) {
                main.post(() -> { if (!closed) toast(R.string.avatar_error); });
            }
        });
        return true;
    }

    private void remove() {
        WORKER.execute(() -> {
            store.remove();
            main.post(() -> {
                if (!closed) { avatar = null; updateViews(); onChanged.run(); }
            });
        });
    }

    private void updateViews() {
        Iterator<WeakReference<AvatarView>> iterator = views.iterator();
        while (iterator.hasNext()) {
            AvatarView view = iterator.next().get();
            if (view == null) iterator.remove();
            else view.setProfile(name, avatar);
        }
    }

    private void toast(int message) { Toast.makeText(activity, message, Toast.LENGTH_LONG).show(); }

    @Override public void close() {
        closed = true;
        views.clear();
    }
}
